package fr.fezlight.eventsystem.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(value = "events")
public class EventProperties {
    private boolean enabled = true;
    private Rabbit rabbit = new Rabbit();
    private ScheduledTask scheduledTask = new ScheduledTask();

    public boolean isEnabled() {
        return this.enabled;
    }

    public Rabbit getRabbit() {
        return this.rabbit;
    }

    public ScheduledTask getScheduledTask() {
        return this.scheduledTask;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRabbit(Rabbit rabbit) {
        this.rabbit = rabbit;
    }

    public void setScheduledTask(ScheduledTask scheduledTask) {
        this.scheduledTask = scheduledTask;
    }

    public static class Rabbit {
        private Queue queue = new Queue();

        public Queue getQueue() {
            return this.queue;
        }

        public void setQueue(Queue queue) {
            this.queue = queue;
        }

        public static class Queue {
            private MainQueueConfig main = new MainQueueConfig("", "events", "events.direct");
            private QueueConfig worker = new QueueConfig("", "events.direct");
            private QueueConfig error = new QueueConfig("events.error", "events.direct");
            private RetryQueueConfig retry = new RetryQueueConfig("events.retry", "events.direct", Duration.ofMinutes(1));
            private boolean autoconfigure = true;

            public MainQueueConfig getMain() {
                return this.main;
            }

            public QueueConfig getError() {
                return this.error;
            }

            public QueueConfig getWorker() {
                return worker;
            }

            public RetryQueueConfig getRetry() {
                return this.retry;
            }

            public boolean isAutoconfigure() {
                return this.autoconfigure;
            }

            public void setMain(MainQueueConfig main) {
                this.main = main;
            }

            public void setWorker(QueueConfig worker) {
                this.worker = worker;
            }

            public void setError(QueueConfig error) {
                this.error = error;
            }

            public void setRetry(RetryQueueConfig retry) {
                this.retry = retry;
            }

            public void setAutoconfigure(boolean autoconfigure) {
                this.autoconfigure = autoconfigure;
            }

            public static class QueueConfig {
                private String name;
                private String exchange;

                public QueueConfig(String name, String exchange) {
                    this.name = name;
                    this.exchange = exchange;
                }

                public String getName() {
                    return this.name;
                }

                public String getExchange() {
                    return this.exchange;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public void setExchange(String exchange) {
                    this.exchange = exchange;
                }
            }

            public static class MainQueueConfig extends QueueConfig {
                private String directExchange;

                public MainQueueConfig(String name, String exchange, String directExchange) {
                    super(name, exchange);
                    this.directExchange = directExchange;
                }

                public String getDirectExchange() {
                    return this.directExchange;
                }

                public void setDirectExchange(String directExchange) {
                    this.directExchange = directExchange;
                }
            }

            public static class RetryQueueConfig extends QueueConfig {
                private Duration timeBetweenRetries;

                public RetryQueueConfig(String name, String exchange, Duration timeBetweenRetries) {
                    super(name, exchange);
                    this.timeBetweenRetries = timeBetweenRetries;
                }

                public Duration getTimeBetweenRetries() {
                    return this.timeBetweenRetries;
                }

                public void setTimeBetweenRetries(Duration timeBetweenRetries) {
                    this.timeBetweenRetries = timeBetweenRetries;
                }
            }
        }
    }

    public static class ScheduledTask {
        private boolean enabled = false;
        private boolean lockEnabled = false;
        private Time incompleteRetry = new Time();
        private Time completeClear = new Time();

        public boolean isEnabled() {
            return this.enabled;
        }

        public boolean isLockEnabled() {
            return this.lockEnabled;
        }

        public Time getIncompleteRetry() {
            return this.incompleteRetry;
        }

        public Time getCompleteClear() {
            return this.completeClear;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setLockEnabled(boolean lockEnabled) {
            this.lockEnabled = lockEnabled;
        }

        public void setIncompleteRetry(Time incompleteRetry) {
            this.incompleteRetry = incompleteRetry;
        }

        public void setCompleteClear(Time completeClear) {
            this.completeClear = completeClear;
        }

        public static class Time {
            private boolean enabled = false;
            private String cron = "0 */1 * * * *";
            private Duration olderThan = Duration.ofMinutes(1);

            public boolean isEnabled() {
                return this.enabled;
            }

            public String getCron() {
                return this.cron;
            }

            public Duration getOlderThan() {
                return this.olderThan;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public void setCron(String cron) {
                this.cron = cron;
            }

            public void setOlderThan(Duration olderThan) {
                this.olderThan = olderThan;
            }
        }
    }
}
