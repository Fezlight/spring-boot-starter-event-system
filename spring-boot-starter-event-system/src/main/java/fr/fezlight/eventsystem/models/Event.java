package fr.fezlight.eventsystem.models;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.modulith.events.Externalized;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Externalized
public interface Event {
}
