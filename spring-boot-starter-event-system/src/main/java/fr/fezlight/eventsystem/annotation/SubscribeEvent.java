package fr.fezlight.eventsystem.annotation;

import fr.fezlight.eventsystem.config.EventRegistryConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the annotated method is a method that can handle event listening
 * for a specific event.
 * <p>
 * The event related to this annotation is deducted from the first parameter of the method.
 *
 * @author FezLight
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {
    /**
     * Configure a custom handler name for the annotated method in the event registry
     *
     * @return the custom name in the registry, if any (or empty by default)
     * @see EventRegistryConfig
     */
    String customName() default "";

    /**
     * Configure how many times the annotated method can be retried
     * @return the number of retry, if any (or 0 by default)
     */
    int retry() default 0;
}
