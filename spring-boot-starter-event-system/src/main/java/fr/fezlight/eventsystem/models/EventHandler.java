package fr.fezlight.eventsystem.models;

import fr.fezlight.eventsystem.annotation.SubscribeEvent;

public interface EventHandler<E extends Event> {
    void handle(E event);

    SubscribeEvent getSubscribeEvent();
}
