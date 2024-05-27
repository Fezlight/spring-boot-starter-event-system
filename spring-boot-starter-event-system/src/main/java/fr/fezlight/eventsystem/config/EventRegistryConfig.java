package fr.fezlight.eventsystem.config;

import fr.fezlight.eventsystem.annotation.SubscribeEvent;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.*;
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

    private final MultiValueMap<Class<? extends Event>, String> handlersRegistry = new LinkedMultiValueMap<>();
    private final Map<String, EventHandler<? extends Event>> handlersMap = new HashMap<>();

    /**
     * Method used to register a new handler to the registry by specifying its name and event class.
     *
     * @param handlerName  Name of the handler about to be registered (avoid using already registered name)
     * @param event        Event related class
     * @param eventHandler A class implementation of EventHandler or lambda
     * @param <E>          Event related class type
     * @return the handler name
     */
    public <E extends Event> String registerHandler(String handlerName, Class<E> event, EventHandler<E> eventHandler) {
        if (handlersMap.containsKey(handlerName)) {
            throw new IllegalArgumentException("Handler with name " + handlerName + " already registered, use 'customName' properties to define an alternative name");
        }

        handlersMap.put(handlerName, eventHandler);

        log.debug("Registering handler for {} with id '{}'", event.getSimpleName(), handlerName);
        handlersRegistry.add(event, handlerName);

        return handlerName;
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
     * @param <E>          Event related class type
     * @return the handler name
     */
    public <E extends Event> String registerHandler(Class<E> event, Consumer<E> eventHandler, int retry) {
        var subscribeEvent = new SubscribeEvent() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return SubscribeEvent.class;
            }

            @Override
            public String customName() {
                return UUID.randomUUID().toString();
            }

            @Override
            public int retry() {
                return retry;
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

        handlersMap.remove(handlerName);

        log.debug("Unregistering handler for {} with id '{}'", event.getSimpleName(), handlerName);
        handlersRegistry.get(event).remove(handlerName);
    }

    public <E extends Event> List<String> getHandlersName(Class<E> event) {
        if (!handlersRegistry.containsKey(event)) {
            return List.of();
        }

        return handlersRegistry.get(event);
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> Optional<EventHandler<E>> getByHandlerName(String handlerName) {
        return Optional.ofNullable((EventHandler<E>) handlersMap.get(handlerName));
    }

    @EventListener(ApplicationStartedEvent.class)
    public void handleStartupEvent() {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("Registry config registered event");

        handlersRegistry.forEach((key, value) -> {
            log.debug("Event \"{}\"", key.getSimpleName());
            value.forEach(handlerName -> log.debug("- Handler '{}'", handlerName));
        });
    }
}
