package fr.fezlight.eventsystem.models;

import static org.springframework.util.StringUtils.hasLength;

public record Handler<T extends Event>(String name, EventHandler<T> eventHandler) {
    public String condition() {
        return eventHandler.getSubscribeEvent().condition();
    }

    public int retry() {
        return eventHandler.getSubscribeEvent().retry();
    }

    public void handle(T event) {
        eventHandler.handle(event);
    }

    @Override
    public String name() {
        var customName = eventHandler.getSubscribeEvent().customName();
        if (hasLength(customName)) {
            return customName;
        }

        return name;
    }
}
