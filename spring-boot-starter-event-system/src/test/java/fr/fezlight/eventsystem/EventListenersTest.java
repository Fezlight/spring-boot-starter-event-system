package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.annotation.SubscribeEvent;
import fr.fezlight.eventsystem.config.EventRegistryConfig;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventHandler;
import fr.fezlight.eventsystem.models.EventWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventListenersTest {

    @InjectMocks
    private EventListeners eventListeners;

    @Mock
    private EventRegistryConfig eventRegistryConfig;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private Supplier<String> defaultMainQueueNaming;

    @Test
    void given1EventWith3Handler_whenProcess_Then3HandlerFound() {
        var event = new TestEventListeners("test");
        when(eventRegistryConfig.getHandlersName(TestEventListeners.class))
                .thenReturn(List.of("handler1", "handler2", "handler3"));

        eventListeners.process(event);

        verify(applicationEventPublisher, times(3)).publishEvent(any(EventWrapper.class));
    }

    @Test
    void given1EventWithNoHandler_whenProcess_ThenNoHandlerFoundNoError() {
        var event = new TestEventListeners("test");
        when(eventRegistryConfig.getHandlersName(TestEventListeners.class))
                .thenReturn(List.of());

        eventListeners.process(event);

        verify(applicationEventPublisher, never()).publishEvent(any(EventWrapper.class));
    }

    @Test
    void given1EventNoReplyTo_whenProcessEvent_ThenHandlerFoundAndHandle() {
        var event = new TestEventListeners("test");
        var eventWrapper = EventWrapper.<TestEventListeners>builder()
                .event(event)
                .handlerName("test")
                .build();

        EventHandler<TestEventListeners> eventHandler = mock(EventHandler.class);
        when(eventHandler.getSubscribeEvent()).thenReturn(new SubscribeEvent() {
            @Override
            public String customName() {
                return "test";
            }

            @Override
            public int retry() {
                return 0;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return SubscribeEvent.class;
            }

            @Override
            public String condition() {
                return "";
            }
        });
        when(eventRegistryConfig.<TestEventListeners>getByHandlerName("test"))
                .thenReturn(Optional.of(eventHandler));

        eventListeners.processEvent(null, eventWrapper);

        verify(eventHandler, times(1)).handle(eq(eventWrapper.getEvent()));
        assertThat(eventWrapper.getRetryLeft()).isEqualTo(0);
    }

    @Test
    void given1EventNoReplyTo_whenProcessEvent_ThenNoHandlerFound() {
        var event = new TestEventListeners("test");
        var eventWrapper = EventWrapper.<TestEventListeners>builder()
                .event(event)
                .handlerName("test")
                .build();

        when(eventRegistryConfig.<TestEventListeners>getByHandlerName("test"))
                .thenReturn(Optional.empty());

        eventListeners.processEvent(null, eventWrapper);

        assertThat(eventWrapper.getRetryLeft()).isNull();
    }

    @Test
    void given1EventWithReplyToInvalid_whenProcessEvent_ThenNotConsuming() {
        var event = new TestEventListeners("test");
        var eventWrapper = EventWrapper.<TestEventListeners>builder()
                .event(event)
                .handlerName("test")
                .build();

        when(defaultMainQueueNaming.get()).thenReturn("events.testevents");

        eventListeners.processEvent("another_queue", eventWrapper);

        verify(eventRegistryConfig, never()).getByHandlerName(anyString());
    }

    @Test
    void given1EventWithReplyToValid_whenProcessEvent_ThenConsuming() {
        var event = new TestEventListeners("test");
        var eventWrapper = EventWrapper.<TestEventListeners>builder()
                .event(event)
                .handlerName("test")
                .build();

        when(defaultMainQueueNaming.get()).thenReturn("events.testevents");
        when(eventRegistryConfig.getByHandlerName(anyString()))
                .thenReturn(Optional.empty());

        eventListeners.processEvent("events.testevents", eventWrapper);

        verify(eventRegistryConfig, times(1)).getByHandlerName(anyString());
    }

    public record TestEventListeners(String eventName) implements Event {
    }
}
