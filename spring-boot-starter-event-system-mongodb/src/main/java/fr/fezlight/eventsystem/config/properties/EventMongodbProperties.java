package fr.fezlight.eventsystem.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties("events.mongodb")
public class EventMongodbProperties {
    @NotNull
    private String databaseName;
}
