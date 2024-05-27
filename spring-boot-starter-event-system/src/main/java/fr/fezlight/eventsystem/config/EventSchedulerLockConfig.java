package fr.fezlight.eventsystem.config;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@EnableSchedulerLock(defaultLockAtMostFor = "PT1M")
@ConditionalOnProperty(value = "events.scheduled-task.lock-enabled", havingValue = "true")
public class EventSchedulerLockConfig {
}
