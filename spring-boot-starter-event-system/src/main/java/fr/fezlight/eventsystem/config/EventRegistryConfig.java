package fr.fezlight.eventsystem.config;

import fr.fezlight.eventsystem.annotation.SubscribeEvent;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventHandler;
import fr.fezlight.eventsystem.models.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * This class is the event registry containing all the events handlers registered by @SubscribeEvent or manually.
 * <p>
 * The main purpose of this class is to store all events handlers at runtime and give a simple way to retrieve all
 * these handlers.
 *
 * @author FezLight
 */
public class EventRegistryConfig {
    private static final Logger log = LoggerFactory.getLogger(EventRegistryConfig.class);

    private final MultiValueMap<Class<? extends Event>, Handler<?>> handlersRegistry = new LinkedMultiValueMap<>();

    /**
     * Method used to register a new handler to the registry by specifying its name and event class.
     *
     * @param handlerName  Name of the handler about to be registered (avoid using already registered name)
     * @param event        Event related class
     * @param eventHandler A class implementation of EventHandler or lambda
     * @param <E>          Event related class type
     * @return the handler instance
     */
    public <E extends Event> Handler<?> registerHandler(String handlerName, Class<E> event, EventHandler<E> eventHandler) {
        var handler = new Handler<>(handlerName, eventHandler);

        if (getByHandlerName(handler.name()).isPresent()) {
            throw new IllegalArgumentException("Handler with name " + handlerName + " already registered, use 'customName' properties to define an alternative name");
        }

        log.debug("Registering handler for {} with id '{}'", event.getSimpleName(), handlerName);

        handlersRegistry.add(event, handler);

        return handler;
    }

    /**
     * Method used to register a new handler to the registry by specifying event class and number of retry.
     * <p>
     * It will do the same exact job as {@link #registerHandler(String, Class, EventHandler)} but with more simple way
     * with parameters. You only have to provide Consumer instead of EventHandler interface, and it will initialize all
     * annotation annoying method.
     *
     * @param event        Event related class
     * @param eventHandler Specify what should be done with the event by consumer
     * @param retry        Number of retries permitted
     * @param condition    Condition to handle event (Spring Expression Language (SpEL) expression)
     * @param <E>          Event related class type
     * @return the handler instance
     */
    public <E extends Event> Handler<?> registerHandler(Class<E> event, Consumer<E> eventHandler, int retry, String condition) {
        var id = UUID.randomUUID().toString();
        var subscribeEvent = new SubscribeEvent() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return SubscribeEvent.class;
            }

            @Override
            public String customName() {
                return id;
            }

            @Override
            public int retry() {
                return retry;
            }

            @Override
            public String condition() {
                return condition;
            }
        };

        return registerHandler(subscribeEvent.customName(), event, new EventHandler<>() {
            @Override
            public void handle(E event) {
                eventHandler.accept(event);
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return subscribeEvent;
            }
        });
    }

    /**
     * Method used to unregister a handler by the event class related and handler name.
     *
     * @param event       Event related class
     * @param handlerName Name of the handler
     * @param <E>         Event related class type
     */
    public <E extends Event> void unregisterHandler(Class<E> event, String handlerName) {
        if (!handlersRegistry.containsKey(event)) {
            log.warn("No handler found for event {} and name '{}'", event, handlerName);
            return;
        }

        log.debug("Unregistering handler for {} with id '{}'", event.getSimpleName(), handlerName);
        handlersRegistry.get(event).removeIf(handler -> handler.name().equals(handlerName));
    }

    /**
     * Method used to retrieve all handler related to an event type
     *
     * @param event Event related class
     * @param <E>   Event related class type
     * @return list of handler related to event type, empty list if not found
     */
    public <E extends Event> List<Handler<?>> getHandlers(Class<E> event) {
        if (!handlersRegistry.containsKey(event)) {
            return List.of();
        }

        return handlersRegistry.get(event);
    }

    /**
     * Method used to clear all event handler in the registry.
     * Essentially used for testing purposes.
     */
    public void clear() {
        handlersRegistry.clear();
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> Optional<Handler<E>> getByHandlerName(String handlerName) {
        return handlersRegistry.values()
                .stream()
                .flatMap(List::stream)
                .filter(handler -> handler.name().equals(handlerName))
                .map(handler -> (Handler<E>) handler)
                .findFirst();
    }

    @EventListener(ApplicationStartedEvent.class)
    public void handleStartupEvent() {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("Registry config registered event");

        handlersRegistry.forEach((key, value) -> {
            log.debug("Event \"{}\"", key.getSimpleName());
            value.forEach(handler -> log.debug("- Handler '{}'", handler.name()));
        });
    }
}
