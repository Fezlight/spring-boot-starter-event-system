package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.config.EventRegistryConfig;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventHandler;
import fr.fezlight.eventsystem.models.EventWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

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

    public EventListeners(EventRegistryConfig eventRegistryConfig, ApplicationEventPublisher applicationEventPublisher,
                          Supplier<String> defaultMainQueueNaming) {
        this.eventRegistryConfig = eventRegistryConfig;
        this.applicationEventPublisher = applicationEventPublisher;
        this.defaultMainQueueNaming = defaultMainQueueNaming;
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
        log.debug("Consuming event {}", event);

        List<String> eventHandlers = eventRegistryConfig.getHandlersName(event.getClass());

        eventHandlers.forEach(handlerName -> applicationEventPublisher.publishEvent(
                EventWrapper.<E>builder()
                        .event(event)
                        .handlerName(handlerName)
                        .build()
        ));

        log.debug("Propagate event to {}", eventHandlers);
    }

    /**
     * Method used to process an {@link EventWrapper} received by {@link EventListeners#process(Event)}.
     *
     * <p>Call the Event Handler if found by its name {@link EventWrapper#getHandlerName()}
     * <p>This method will also check if the replyTo headers received from RabbitMQ is matching to the current main
     * event queue name. If not, the event is ignored.
     *
     *
     * @param <E> Type of Event.
     * @param replyTo RabbitMQ Header "reply_to".
     * @param event Event received from {@link ApplicationEventPublisher}.
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

        Optional<EventHandler<E>> eventHandlers = eventRegistryConfig.getByHandlerName(event.getHandlerName());

        eventHandlers.ifPresentOrElse(tEventHandler -> {
            log.debug("Handler found => {}", event.getHandlerName());

            event.setRetryLeft(tEventHandler.getSubscribeEvent().retry());

            tEventHandler.handle(event.getEvent());
        }, () -> log.error("No handler found for name '{}'", event.getHandlerName()));
    }

    /**
     * Method used to ignore Event not compatible with Event System.
     *
     * @param event Unknown object.
     */
    @RabbitHandler(isDefault = true)
    public void errorEvent(Object event) {
        log.debug("Receiving error event {}", event);
    }
}
