package fr.fezlight.eventsystem.models;

import fr.fezlight.eventsystem.annotation.SubscribeEvent;

/**
 * Interface used to implement an event handler.
 *
 * <li>{@link EventHandler#handle(Event)} method will receive the event.</li>
 * <li>{@link EventHandler#getSubscribeEvent()} method needed to retrieve event handler parameters by annotation</li>
 *
 * @author FezLight
 */
public interface EventHandler<E extends Event> {
    void handle(E event);

    SubscribeEvent getSubscribeEvent();
}
