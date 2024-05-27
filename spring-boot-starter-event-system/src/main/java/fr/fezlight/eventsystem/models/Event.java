package fr.fezlight.eventsystem.models;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.modulith.events.Externalized;

/**
 * Interface used to identify an event.
 * <p>
 * This interface will implement an externalized way for any Event.
 *
 * @author FezLight
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Externalized
public interface Event {
}
