package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.annotation.SubscribeEvent;
import fr.fezlight.eventsystem.config.EventRegistryConfig;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EventRegistryConfigTest {

    @InjectMocks
    private EventRegistryConfig eventRegistryConfig;

    @Test
    void givenHandlersRegistered_whenGetHandlersName_thenReturnHandler() {
        eventRegistryConfig.registerHandler("test", TestEventRegistry.class, new EventHandler<>() {
            @Override
            public void handle(TestEventRegistry event) {
                // Nothing
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return null;
            }
        });

        var result = eventRegistryConfig.getByHandlerName("test");

        assertThat(result).isPresent();
    }

    @Test
    void given2HandlersRegistered_whenGetHandlersName_thenReturnHandlersName() {
        var eventHandler = new EventHandler<TestEventRegistry>() {
            @Override
            public void handle(TestEventRegistry event) {
                // Nothing
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return null;
            }
        };
        eventRegistryConfig.registerHandler("test", TestEventRegistry.class, eventHandler);
        eventRegistryConfig.registerHandler("test2", TestEventRegistry.class, eventHandler);

        var result = eventRegistryConfig.getHandlersName(TestEventRegistry.class);

        assertThat(result).contains("test", "test2");
    }

    @Test
    void givenNoHandlers_whenGetHandlersName_thenReturnEmpty() {
        var result = eventRegistryConfig.getByHandlerName("test");

        assertThat(result).isEmpty();
    }

    @Test
    void givenEventHandler_whenRegisterHandler_thenHandlerRegistered() {
        eventRegistryConfig.registerHandler("test", TestEventRegistry.class, new EventHandler<>() {
            @Override
            public void handle(TestEventRegistry event) {
                // Nothing
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return null;
            }
        });

        assertThat(eventRegistryConfig.getByHandlerName("test")).isPresent();
        assertThat(eventRegistryConfig.getHandlersName(TestEventRegistry.class))
                .hasSize(1)
                .contains("test");
    }

    @Test
    void givenEventHandler_whenRegisterHandlerWithExistingName_thenThrowException() {
        var eventHandler = new EventHandler<TestEventRegistry>() {
            @Override
            public void handle(TestEventRegistry event) {
                // Nothing
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return null;
            }
        };
        var handlerName = "test";
        eventRegistryConfig.registerHandler(handlerName, TestEventRegistry.class, eventHandler);

        var e = assertThrows(IllegalArgumentException.class,
                () -> eventRegistryConfig.registerHandler(handlerName, TestEventRegistry.class, eventHandler));

        assertThat(e).hasMessageContaining(
                "Handler with name "
                + handlerName +
                " already registered, use 'customName' properties to define an alternative name");
    }

    @Test
    void givenEventHandlerConsumer_whenRegisterHandler_thenHandlerRegistered() {
        eventRegistryConfig.registerHandler(TestEventRegistry.class, e -> {
        }, 5);

        var handlers = eventRegistryConfig.getHandlersName(TestEventRegistry.class);

        assertThat(handlers).hasSize(1);

        var handler = eventRegistryConfig.getByHandlerName(handlers.get(0));
        assertThat(handler).isPresent();
        assertThat(handler.get().getSubscribeEvent().retry()).isEqualTo(5);
        assertThat(handler.get().getSubscribeEvent().customName()).isNotNull();
    }

    @Test
    void givenRegisteredEventHandler_whenUnregisterHandler_thenHandlerUnregistered() {
        eventRegistryConfig.registerHandler("test", TestEventRegistry.class, new EventHandler<>() {
            @Override
            public void handle(TestEventRegistry event) {
                // Nothing
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return null;
            }
        });

        eventRegistryConfig.unregisterHandler(TestEventRegistry.class, "test");

        assertThat(eventRegistryConfig.getByHandlerName("test")).isEmpty();
        assertThat(eventRegistryConfig.getHandlersName(TestEventRegistry.class)).isEmpty();
    }

    @Test
    void givenNoRegisteredEventHandler_whenUnregisterHandler_thenOnlyLog() {
        eventRegistryConfig.unregisterHandler(TestEventRegistry.class, "test");

        assertThat(eventRegistryConfig.getByHandlerName("test")).isEmpty();
        assertThat(eventRegistryConfig.getHandlersName(TestEventRegistry.class)).isEmpty();
    }

    public record TestEventRegistry(String eventName) implements Event {
    }
}
