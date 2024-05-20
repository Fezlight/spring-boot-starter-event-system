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

public class EventRegistryConfig {
    private static final Logger log = LoggerFactory.getLogger(EventRegistryConfig.class);

    private final MultiValueMap<Class<? extends Event>, String> handlersRegistry = new LinkedMultiValueMap<>();
    private final Map<String, EventHandler<? extends Event>> handlersMap = new HashMap<>();

    public <T extends Event> String registerHandler(String handlerName, Class<T> event, EventHandler<T> eventHandler) {
        if (handlersMap.containsKey(handlerName)) {
            throw new IllegalArgumentException("Handler with name " + handlerName + " already registered, use 'customName' properties to define an alternative name");
        }

        handlersMap.put(handlerName, eventHandler);

        log.debug("Registering handler for {} with id '{}'", event.getSimpleName(), handlerName);
        handlersRegistry.add(event, handlerName);

        return handlerName;
    }

    public <T extends Event> String registerHandler(Class<T> event, Consumer<T> eventHandler, int retry) {
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
            public void handle(T event) {
                eventHandler.accept(event);
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return subscribeEvent;
            }
        });
    }

    public <T extends Event> void unregisterHandler(Class<T> event, String handlerName) {
        if (!handlersRegistry.containsKey(event)) {
            log.warn("No handler found for event {} and name '{}'", event, handlerName);
            return;
        }

        handlersMap.remove(handlerName);

        log.debug("Unregistering handler for {} with id '{}'", event.getSimpleName(), handlerName);
        handlersRegistry.get(event).remove(handlerName);
    }

    public <T extends Event> List<String> getHandlersName(Class<T> event) {
        if (!handlersRegistry.containsKey(event)) {
            return List.of();
        }

        return handlersRegistry.get(event);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> Optional<EventHandler<T>> getByHandlerName(String handlerName) {
        return Optional.ofNullable((EventHandler<T>) handlersMap.get(handlerName));
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
