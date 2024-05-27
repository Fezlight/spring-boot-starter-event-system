package fr.fezlight.eventsystem.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Setter
@Getter
@Validated
@ConfigurationProperties(value = "events")
public class EventProperties {
    private boolean enabled = true;
    private Rabbit rabbit = new Rabbit();
    private ScheduledTask scheduledTask = new ScheduledTask();

    @Setter
    @Getter
    public static class Rabbit {
        private Queue queue = new Queue();

        @Setter
        @Getter
        public static class Queue {
            private MainQueueConfig main = new MainQueueConfig("events", "events", "events.direct");
            private QueueConfig error = new QueueConfig("events.error", "events.direct");
            private RetryQueueConfig retry = new RetryQueueConfig("events.retry", "events.direct", Duration.ofMinutes(1));
            private boolean autoconfigure = true;

            @Setter
            @Getter
            public static class QueueConfig {
                private String name;
                private String exchange;

                public QueueConfig(String name, String exchange) {
                    this.name = name;
                    this.exchange = exchange;
                }
            }

            @Setter
            @Getter
            public static class MainQueueConfig extends QueueConfig {
                private String directExchange;

                public MainQueueConfig(String name, String exchange, String directExchange) {
                    super(name, exchange);
                    this.directExchange = directExchange;
                }
            }

            @Setter
            @Getter
            public static class RetryQueueConfig extends QueueConfig {
                private Duration timeBetweenRetries;

                public RetryQueueConfig(String name, String exchange, Duration timeBetweenRetries) {
                    super(name, exchange);
                    this.timeBetweenRetries = timeBetweenRetries;
                }
            }
        }
    }

    @Getter
    @Setter
    public static class ScheduledTask {
        private boolean enabled = true;
        private boolean lockEnabled = false;
        private Time incompleteRetry = new Time();
        private Time completeClear = new Time();

        @Setter
        @Getter
        public static class Time {
            private boolean enabled = true;
            private String cron = "0 */1 * * * *";
            private Duration olderThan = Duration.ofMinutes(1);
        }
    }
}
