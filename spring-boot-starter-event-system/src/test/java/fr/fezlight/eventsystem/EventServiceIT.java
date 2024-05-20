package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.config.AppConfiguration;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@ContextConfiguration(classes = AppConfiguration.class)
public class EventServiceIT {

    @Autowired
    private EventService eventService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    void given1MessageInErrorQueue_whenCallRetry_thenRetryAll() {
        rabbitTemplate.convertAndSend("events.error", EventWrapper.builder()
                        .event(new TestEventService("test"))
                        .handlerName("test")
                        .retryLeft(0)
                        .build(),
                message -> {
                    message.getMessageProperties().setReplyTo("events.testevents");
                    return message;
                }
        );

        Integer countError = amqpAdmin.getQueueInfo("events.error").getMessageCount();
        assertThat(countError).isEqualTo(1);

        eventService.reprocessAllFailedMessage();

        countError = amqpAdmin.getQueueInfo("events.error").getMessageCount();
        assertThat(countError).isEqualTo(0);
        Integer count = amqpAdmin.getQueueInfo("events.testevents").getMessageCount();
        assertThat(count).isEqualTo(1);
    }

    public record TestEventService(String eventName) implements Event {
    }
}
