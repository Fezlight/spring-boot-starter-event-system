package fr.fezlight.eventsystem.config;

import fr.fezlight.eventsystem.config.properties.EventProperties;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@Configuration
@ConditionalOnProperty(value = "events.scheduled-task.enabled", havingValue = "true", matchIfMissing = true)
public class EventSchedulingTaskConfig {
    private static final Logger log = LoggerFactory.getLogger(EventSchedulingTaskConfig.class);

    private final EventPublicationRegistry eventPublicationRegistry;
    private final IncompleteEventPublications incompleteEventPublications;
    private final EventProperties eventProperties;

    public EventSchedulingTaskConfig(EventPublicationRegistry eventPublicationRegistry,
                                     IncompleteEventPublications incompleteEventPublications,
                                     EventProperties eventProperties) {
        this.eventPublicationRegistry = eventPublicationRegistry;
        this.incompleteEventPublications = incompleteEventPublications;
        this.eventProperties = eventProperties;
    }

    @Scheduled(cron = "#{@clearCompletedEventCron}")
    @SchedulerLock(name = "EventPublicationsConfig#clearCompletedEvent")
    public void clearCompletedEvent() {
        log.debug("Delete completed events ...");
        eventPublicationRegistry.deleteCompletedPublicationsOlderThan(eventProperties.getScheduledTask().getCompleteClear().getOlderThan());
    }

    @Scheduled(cron = "#{@retryIncompleteEventsCron}")
    @SchedulerLock(name = "EventPublicationsConfig#retryIncompleteEvents")
    public void retryIncompleteEvents() {
        log.debug("Retry incomplete events ...");
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(eventProperties.getScheduledTask().getIncompleteRetry().getOlderThan());
    }
}
