package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.config.AppConfiguration;
import fr.fezlight.eventsystem.config.EventRegistryConfig;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventWrapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.atomic.AtomicBoolean;

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

    @BeforeEach
    void setUp() {
        eventRegistryConfig.clear();
    }

    @Test
    void givenRegisteredHandlerWithCondition_whenPublishEvent_ThenSingleEvent(Scenario scenario) {
        AtomicBoolean bool = new AtomicBoolean(false);
        var handler = eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
            bool.set(true);
        }, 0, "#event.name.equals(\"testName\")");

        var event = new TestEventListener("testName");

        scenario.stimulate(() -> eventListeners.process(event))
                .andWaitForEventOfType(EventWrapper.class)
                .toArriveAndVerify(e -> {
                    assertThat(e).isNotNull();
                    assertThat(e.getHandlerName()).isEqualTo(handler.name());
                    assertThat(e.getEvent()).isEqualTo(event);
                });

        assertThat(bool.get()).isEqualTo(true);

        eventRegistryConfig.unregisterHandler(TestEventListener.class, handler.name());
    }

    @Test
    void givenRegisteredHandlerWithCondition_whenPublishEvent_ThenNoHandlerFound(Scenario scenario) {
        AtomicBoolean bool = new AtomicBoolean(false);
        var handler = eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
            bool.set(true);
        }, 0, "#event.name.equals(\"other\")");

        var event = new TestEventListener("testName");

        eventListeners.process(event);

        assertThat(bool.get()).isEqualTo(false);

        eventRegistryConfig.unregisterHandler(TestEventListener.class, handler.name());
    }

    @Test
    void given4RegisteredHandler_whenPublishEvent_ThenMultipleEvent(PublishedEvents events) {
        var listEvents = List.of(
                eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
                }, 0, ""),
                eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
                }, 0, ""),
                eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
                }, 0, ""),
                eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
                }, 0, "")
        );

        var event = new TestEventListener("testName");

        eventListeners.process(event);

        var result = events.ofType(EventWrapper.class);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);

        listEvents.forEach(e -> eventRegistryConfig.unregisterHandler(TestEventListener.class, e.name()));
    }

    @Test
    void givenRegisteredHandlerWithException_whenPublishEvent_ThenSendToError(Scenario scenario) {
        var handler = eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
            throw new IllegalArgumentException();
        }, 0, "");

        var event = new TestEventListener("testName");

        scenario.stimulate(() -> eventListeners.process(event))
                .andWaitForEventOfType(EventWrapper.class)
                .toArriveAndVerify(e -> {
                    assertThat(e).isNotNull();
                    assertThat(e.getHandlerName()).isEqualTo(handler.name());
                    assertThat(e.getEvent()).isEqualTo(event);
                });

        Long countError = amqpAdmin.getQueueInfo("events.error").getMessageCount();
        assertThat(countError).isEqualTo(1L);

        eventRegistryConfig.unregisterHandler(TestEventListener.class, handler.name());
    }

    @Test
    void givenRegisteredHandlerWithExceptionAndCanRetry_whenPublishEvent_ThenSendToRetry(Scenario scenario) {
        var ev = eventRegistryConfig.registerHandler(TestEventListener.class, e -> {
            throw new IllegalArgumentException();
        }, 1, "");

        var event = new TestEventListener("testName");
        var eventHandlers = eventRegistryConfig.getHandlers(TestEventListener.class);

        scenario.stimulate(() -> eventListeners.process(event))
                .andWaitForEventOfType(EventWrapper.class)
                .toArriveAndVerify(e -> {
                    assertThat(e).isNotNull();
                    assertThat(e.getHandlerName()).isEqualTo(eventHandlers.get(0).name());
                    assertThat(e.getEvent()).isEqualTo(event);
                });

        Long countError = amqpAdmin.getQueueInfo("events.error").getMessageCount();
        assertThat(countError).isEqualTo(0L);
        Long countRetry = amqpAdmin.getQueueInfo("events.retry").getMessageCount();
        assertThat(countRetry).isEqualTo(1L);

        eventRegistryConfig.unregisterHandler(TestEventListener.class, ev.name());
    }

    public record TestEventListener(String name) implements Event {
    }
}
