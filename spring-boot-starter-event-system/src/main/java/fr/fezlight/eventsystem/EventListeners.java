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

    @RabbitHandler
    @Transactional
    public <T extends Event> void process(T event) {
        log.debug("Consuming event {}", event);

        List<String> eventHandlers = eventRegistryConfig.getHandlersName(event.getClass());

        eventHandlers.forEach(handlerName -> applicationEventPublisher.publishEvent(
                EventWrapper.<T>builder()
                        .event(event)
                        .handlerName(handlerName)
                        .build()
        ));

        log.debug("Propagate event to {}", eventHandlers);
    }

    @RabbitHandler
    public <T extends Event> void processEvent(@Header(value = AmqpHeaders.REPLY_TO, required = false) String replyTo,
                                               EventWrapper<T> event) {
        if (replyTo != null && !Objects.equals(replyTo, defaultMainQueueNaming.get())) {
            log.debug("No consuming for this message '{}' related to other queue {}", event.getEvent().getClass().getName(), replyTo);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Receiving event {}", event);
        }

        Optional<EventHandler<T>> eventHandlers = eventRegistryConfig.getByHandlerName(event.getHandlerName());

        eventHandlers.ifPresentOrElse(tEventHandler -> {
            log.debug("Handler found => {}", event.getHandlerName());

            event.setRetryLeft(tEventHandler.getSubscribeEvent().retry());

            tEventHandler.handle(event.getEvent());
        }, () -> log.error("No handler found for name '{}'", event.getHandlerName()));
    }

    @RabbitHandler(isDefault = true)
    public void errorEvent(Object event) {
        log.debug("Receiving error event {}", event);
    }
}
