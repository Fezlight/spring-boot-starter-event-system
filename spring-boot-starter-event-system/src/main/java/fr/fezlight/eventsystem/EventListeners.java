package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.config.EventRegistryConfig;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventWrapper;
import fr.fezlight.eventsystem.models.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.springframework.util.StringUtils.hasLength;

/**
 * Class used to listen on RabbitMQ when an event is published by {@link ApplicationEventPublisher}.
 * There is two methods listening on the main event queue.
 *
 * <p>- {@link EventListeners#process(Event)} process Event published by {@link ApplicationEventPublisher} and divide each
 * handler found into an {@link EventWrapper} and resend to main event queue.
 * <p>- {@link EventListeners#processEvent(String, EventWrapper)} process {@link EventWrapper} published by {@link EventListeners#process(Event)}
 * and call the associated handler.
 *
 * @author FezLight
 */
@RabbitListener(queues = "#{@defaultMainQueueNaming.get()}", errorHandler = "rabbitListenerCustomErrorHandler")
public class EventListeners {
    private static final Logger log = LoggerFactory.getLogger(EventListeners.class);

    private final EventRegistryConfig eventRegistryConfig;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Supplier<String> defaultMainQueueNaming;
    private final ExpressionParser expressionParser;
    private final BiFunction<String, EvaluationContext, Boolean> conditionEvaluation;
    private final BeanFactory beanFactory;

    public EventListeners(EventRegistryConfig eventRegistryConfig, ApplicationEventPublisher applicationEventPublisher,
                          Supplier<String> defaultMainQueueNaming, BeanFactory beanFactory) {
        this.eventRegistryConfig = eventRegistryConfig;
        this.applicationEventPublisher = applicationEventPublisher;
        this.defaultMainQueueNaming = defaultMainQueueNaming;
        this.beanFactory = beanFactory;
        this.expressionParser = new SpelExpressionParser(
                new SpelParserConfiguration(true, true)
        );
        this.conditionEvaluation = (expression, context) -> {
            return Boolean.TRUE.equals(expressionParser.parseExpression(expression).getValue(context, Boolean.class));
        };
    }

    /**
     * Method used to process an event received by {@link ApplicationEventPublisher}.
     * <p>- Search all handlers registered for this event and create {@link EventWrapper} for each one.
     * <p>- Resend all to main event queue.
     *
     * @param <E>   Type of Event.
     * @param event Event received from {@link ApplicationEventPublisher}.
     */
    @RabbitHandler
    @Transactional
    public <E extends Event> void process(E event) {
        if (log.isDebugEnabled()) {
            log.debug("Consuming event {}", event);
        }

        var context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(beanFactory));

        List<Handler<?>> eventHandlers = eventRegistryConfig.getHandlers(event.getClass()).stream().filter(handler -> {
            var expression = handler.condition();
            if (!hasLength(expression)) {
                return true;
            }

            var isValid = conditionEvaluation.apply(expression, context);
            if (!isValid && log.isDebugEnabled()) {
                log.debug("Filter out handler '{}' because condition not matched", handler.name());
            }

            return isValid;
        }).toList();

        eventHandlers.forEach(handler -> applicationEventPublisher.publishEvent(
                EventWrapper.<E>builder()
                        .event(event)
                        .handlerName(handler.name())
                        .retryLeft(0)
                        .build()
        ));

        if (log.isDebugEnabled()) {
            log.debug("Propagate event to {}", eventHandlers);
        }
    }

    /**
     * Method used to process an {@link EventWrapper} received by {@link EventListeners#process(Event)}.
     *
     * <p>Call the Event Handler if found by its name {@link EventWrapper#getHandlerName()}
     * <p>This method will also check if the replyTo headers received from RabbitMQ is matching to the current main
     * event queue name. If not, the event is ignored.
     *
     * @param <E>     Type of Event.
     * @param replyTo RabbitMQ Header "reply_to".
     * @param event   Event received from {@link ApplicationEventPublisher}.
     */
    @RabbitHandler
    public <E extends Event> void processEvent(@Header(value = AmqpHeaders.REPLY_TO, required = false) String replyTo,
                                               EventWrapper<E> event) {
        if (replyTo != null && !Objects.equals(replyTo, defaultMainQueueNaming.get())) {
            log.debug("No consuming for this message '{}' related to other queue {}", event.getEvent().getClass().getName(), replyTo);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Receiving event {}", event);
        }

        Optional<Handler<E>> eventHandlers = eventRegistryConfig.getByHandlerName(event.getHandlerName());

        eventHandlers.ifPresentOrElse(handler -> {
            log.debug("Handler found => {}", event.getHandlerName());

            event.setRetryLeft(handler.retry());

            handler.handle(event.getEvent());
        }, () -> log.error("No handler found for name '{}'", event.getHandlerName()));
    }

    /**
     * Method used to ignore Event not compatible with Event System.
     *
     * @param event Unknown object.
     */
    @RabbitHandler(isDefault = true)
    public void errorEvent(Object event) {
        if (log.isDebugEnabled()) {
            log.debug("Receiving error event {}", event);
        }
    }
}
