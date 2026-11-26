package fr.fezlight.eventsystem.config;

import fr.fezlight.eventsystem.config.properties.EventProperties;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

@EnableScheduling
@Configuration
@ConditionalOnProperty(value = "events.scheduled-task.enabled", havingValue = "true", matchIfMissing = true)
public class EventSchedulingTaskConfig {
    private static final Logger log = LoggerFactory.getLogger(EventSchedulingTaskConfig.class);

    private final EventPublicationRegistry eventPublicationRegistry;
    private final FailedEventPublications failedEventPublications;
    private final EventProperties eventProperties;

    public EventSchedulingTaskConfig(EventPublicationRegistry eventPublicationRegistry,
                                     FailedEventPublications failedEventPublications,
                                     EventProperties eventProperties) {
        this.eventPublicationRegistry = eventPublicationRegistry;
        this.failedEventPublications = failedEventPublications;
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

        Duration retryOlderThan = eventProperties.getScheduledTask().getIncompleteRetry().getOlderThan();
        failedEventPublications
                .resubmit(ResubmissionOptions.defaults().withMinAge(retryOlderThan));
    }
}
