package fr.fezlight.eventsystem.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("events.mongodb")
public class EventMongodbProperties {
    @NotNull
    private String databaseName;

    public @NotNull String getDatabaseName() {
        return this.databaseName;
    }

    public void setDatabaseName(@NotNull String databaseName) {
        this.databaseName = databaseName;
    }
}
