package fr.fezlight.eventsystem.config.rabbitmq;

import fr.fezlight.eventsystem.config.AppConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class QueueNameResolverIT {

    @SpringBootTest
    @ContextConfiguration(classes = AppConfiguration.class)
    static abstract class TestBase {
        @Autowired
        private QueueNameResolver queueNameResolver;

        @Test
        void mainQueueName() {
            assertThat(queueNameResolver.getMainQueueName()).isEqualTo(expectedMainQueue());
        }

        @Test
        void workerQueueName() {
            assertThat(queueNameResolver.getWorkerQueueName()).isEqualTo(expectedWorkerQueue());
        }

        String expectedMainQueue() {
            return "events.testevents.main";
        }

        String expectedWorkerQueue() {
            return "events.testevents.worker";
        }
    }


    @Nested
    @TestPropertySource(properties = {
            "events.rabbit.queue.main.name=mainQueue",
            "events.rabbit.queue.worker.name=workerQueue"
    })
    class WithAlternateNameProperties extends TestBase {
        String expectedMainQueue() {
            return "mainQueue";
        }

        String expectedWorkerQueue() {
            return "workerQueue";
        }
    }

    @Nested
    class WithoutAlternateNameProperties extends TestBase {
    }

    @Nested
    @Import(WithOldBean.OldBeanConfiguration.class)
    class WithOldBean extends TestBase {
        @TestConfiguration
        static class OldBeanConfiguration {
            @Bean
            Supplier<String> defaultMainQueueNaming() {
                return () -> "legacyMainQueueName";
            }

            @Bean
            Supplier<String> defaultWorkerQueueNaming() {
                return () -> "legacyWorkerQueueName";
            }
        }

        String expectedMainQueue() {
            return "legacyMainQueueName";
        }

        String expectedWorkerQueue() {
            return "legacyWorkerQueueName";
        }
    }

    @Nested
    @Import(WithNewBean.NewBeanConfiguration.class)
    class WithNewBean extends TestBase {
        @TestConfiguration
        static class NewBeanConfiguration {
            @Bean
            String defaultMainQueueName() {
                return "newMainQueueName";
            }

            @Bean
            String defaultWorkerQueueName() {
                return "newWorkerQueueName";
            }
        }

        String expectedMainQueue() {
            return "newMainQueueName";
        }

        String expectedWorkerQueue() {
            return "newWorkerQueueName";
        }
    }
}