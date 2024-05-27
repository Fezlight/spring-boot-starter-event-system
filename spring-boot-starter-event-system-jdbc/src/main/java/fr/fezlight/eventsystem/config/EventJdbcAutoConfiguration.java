package fr.fezlight.eventsystem.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

@ConditionalOnProperty(
        value = "events.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@AutoConfiguration
@AutoConfigureAfter(EventAutoConfiguration.class)
@Import({EventSchedulingTaskConfig.class, EventSchedulerLockConfig.class})
public class EventJdbcAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "events.scheduled-task.lock-enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
