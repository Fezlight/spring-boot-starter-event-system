package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.config.AppConfiguration;
import fr.fezlight.eventsystem.config.EventRegistryConfig;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableScenarios
@ExtendWith(PublishedEventsExtension.class)
@ContextConfiguration(classes = AppConfiguration.class)
public class EventListenersIT {
    @Autowired
    private EventRegistryConfig eventRegistryConfig;

    @Autowired
    private EventListeners eventListeners;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    void givenRegisteredHandler_whenPublishEvent_ThenSingleEvent(Scenario scenario) {
        var ev = eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
        }, 0);

        var event = new TestEventListener("testName");
        var eventHandlers = eventRegistryConfig.getHandlersName(TestEventListener.class);

        scenario.stimulate(() -> eventListeners.process(event))
                .andWaitForEventOfType(EventWrapper.class)
                .toArriveAndVerify(e -> {
                    assertThat(e).isNotNull();
                    assertThat(e.getHandlerName()).isEqualTo(eventHandlers.get(0));
                    assertThat(e.getEvent()).isEqualTo(event);
                });

        eventRegistryConfig.unregisterHandler(TestEventListener.class, ev);
    }

    @Test
    void given4RegisteredHandler_whenPublishEvent_ThenMultipleEvent(PublishedEvents events) {
        var listEvents = List.of(
                eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
                }, 0),
                eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
                }, 0),
                eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
                }, 0),
                eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
                }, 0)
        );

        var event = new TestEventListener("testName");

        eventListeners.process(event);

        var result = events.ofType(EventWrapper.class);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);

        listEvents.forEach(e -> eventRegistryConfig.unregisterHandler(TestEventListener.class, e));
    }

    @Test
    void givenRegisteredHandlerWithException_whenPublishEvent_ThenSendToError(Scenario scenario) {
        var ev = eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
            throw new IllegalArgumentException();
        }, 0);

        var event = new TestEventListener("testName");
        var eventHandlers = eventRegistryConfig.getHandlersName(TestEventListener.class);

        scenario.stimulate(() -> eventListeners.process(event))
                .andWaitForEventOfType(EventWrapper.class)
                .toArriveAndVerify(e -> {
                    assertThat(e).isNotNull();
                    assertThat(e.getHandlerName()).isEqualTo(eventHandlers.get(0));
                    assertThat(e.getEvent()).isEqualTo(event);
                });

        Integer countError = amqpAdmin.getQueueInfo("events.error").getMessageCount();
        assertThat(countError).isEqualTo(1);

        eventRegistryConfig.unregisterHandler(TestEventListener.class, ev);
    }

    @Test
    void givenRegisteredHandlerWithExceptionAndCanRetry_whenPublishEvent_ThenSendToRetry(Scenario scenario) {
        var ev = eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
            throw new IllegalArgumentException();
        }, 1);

        var event = new TestEventListener("testName");
        var eventHandlers = eventRegistryConfig.getHandlersName(TestEventListener.class);

        scenario.stimulate(() -> eventListeners.process(event))
                .andWaitForEventOfType(EventWrapper.class)
                .toArriveAndVerify(e -> {
                    assertThat(e).isNotNull();
                    assertThat(e.getHandlerName()).isEqualTo(eventHandlers.get(0));
                    assertThat(e.getEvent()).isEqualTo(event);
                });

        Integer countError = amqpAdmin.getQueueInfo("events.error").getMessageCount();
        assertThat(countError).isEqualTo(0);
        Integer countRetry = amqpAdmin.getQueueInfo("events.retry").getMessageCount();
        assertThat(countRetry).isEqualTo(1);

        eventRegistryConfig.unregisterHandler(TestEventListener.class, ev);
    }

    public record TestEventListener(String name) implements Event {
    }
}
