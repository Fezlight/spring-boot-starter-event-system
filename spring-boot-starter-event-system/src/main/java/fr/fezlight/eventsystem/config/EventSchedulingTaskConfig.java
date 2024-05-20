package fr.fezlight.eventsystem.config;

import fr.fezlight.eventsystem.config.properties.EventProperties;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.core.DefaultEventPublicationRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import static org.springframework.scheduling.annotation.Scheduled.CRON_DISABLED;

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT1M")
@ConditionalOnProperty(value = "events.scheduled-task.enabled", havingValue = "true", matchIfMissing = true)
public class EventSchedulingTaskConfig {
    private static final Logger log = LoggerFactory.getLogger(EventSchedulingTaskConfig.class);

    private final DefaultEventPublicationRegistry defaultEventPublicationRegistry;
    private final IncompleteEventPublications incompleteEventPublications;
    private final EventProperties eventProperties;

    public EventSchedulingTaskConfig(DefaultEventPublicationRegistry defaultEventPublicationRegistry,
                                     IncompleteEventPublications incompleteEventPublications,
                                     EventProperties eventProperties) {
        this.defaultEventPublicationRegistry = defaultEventPublicationRegistry;
        this.incompleteEventPublications = incompleteEventPublications;
        this.eventProperties = eventProperties;
    }

    @Bean
    public String retryIncompleteEventsCron() {
        if (eventProperties.getScheduledTask().isEnabled() || eventProperties.getScheduledTask().getIncompleteRetry().isEnabled()) {
            return eventProperties.getScheduledTask().getIncompleteRetry().getCron();
        }

        return CRON_DISABLED;
    }

    @Bean
    public String clearCompletedEventCron() {
        if (eventProperties.getScheduledTask().isEnabled() || eventProperties.getScheduledTask().getCompleteClear().isEnabled()) {
            return eventProperties.getScheduledTask().getCompleteClear().getCron();
        }

        return CRON_DISABLED;
    }

    @Scheduled(cron = "#{@clearCompletedEventCron}")
    @SchedulerLock(name = "EventPublicationsConfig#clearCompletedEvent")
    public void clearCompletedEvent() {
        log.debug("Delete completed events ...");
        defaultEventPublicationRegistry.deleteCompletedPublicationsOlderThan(eventProperties.getScheduledTask().getCompleteClear().getOlderThan());
    }

    @Scheduled(cron = "#{@retryIncompleteEventsCron}")
    @SchedulerLock(name = "EventPublicationsConfig#retryIncompleteEvents")
    public void retryIncompleteEvents() {
        log.debug("Retry incomplete events ...");
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(eventProperties.getScheduledTask().getIncompleteRetry().getOlderThan());
    }
}
