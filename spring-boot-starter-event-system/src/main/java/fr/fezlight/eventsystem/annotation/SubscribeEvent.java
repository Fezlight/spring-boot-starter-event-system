package fr.fezlight.eventsystem.annotation;

import fr.fezlight.eventsystem.config.EventRegistryConfig;
import fr.fezlight.eventsystem.models.EventWrapper;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.springframework.core.annotation.AliasFor;

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
@DomainEventHandler
public @interface SubscribeEvent {
    /**
     * Configure a custom handler name for the annotated method in the event registry
     *
     * @return the custom name in the registry, if any (or empty by default)
     * @see EventRegistryConfig
     */
    @AliasFor(annotation = DomainEventHandler.class, attribute = "name")
    String customName() default "";

    /**
     * Configure how many times the annotated method can be retried
     * @return the number of retry, if any (or 0 by default)
     */
    int retry() default 0;

    /**
     * Spring Expression Language (SpEL) expression used for making the event
     * handling conditional.
     * <p>
     * This condition is used when consuming event to route it to the right destination
     * <p>
     * Use #event.[anyField] to find a value from your event object and #root.[anyField] to
     * retrieve a value from wrapper object <b>EventWrapper</b>
     *
     * @return Spring SpEL expression to condition consuming event
     * @see EventWrapper Event wrapper object
     */
    /* language=SpEL */
    String condition() default "";
}
