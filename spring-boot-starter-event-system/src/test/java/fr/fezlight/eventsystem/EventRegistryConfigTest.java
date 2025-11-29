package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.annotation.SubscribeEvent;
import fr.fezlight.eventsystem.config.EventRegistryConfig;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventHandler;
import fr.fezlight.eventsystem.models.Handler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EventRegistryConfigTest {

    @InjectMocks
    private EventRegistryConfig eventRegistryConfig;

    private final SubscribeEvent subscribeEvent = new SubscribeEvent() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return SubscribeEvent.class;
        }

        @Override
        public String customName() {
            return "";
        }

        @Override
        public int retry() {
            return 0;
        }

        @Override
        public String condition() {
            return "";
        }
    };

    @Test
    void givenHandlersRegistered_whenGetHandlers_thenReturnHandler() {
        eventRegistryConfig.registerHandler("test", TestEventRegistry.class, new EventHandler<>() {
            @Override
            public void handle(TestEventRegistry event) {
                // Nothing
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return subscribeEvent;
            }
        });

        var result = eventRegistryConfig.getByHandlerName("test");

        assertThat(result).isPresent();
    }

    @Test
    void given2HandlersRegistered_whenGetHandlersName_thenReturnHandlers() {
        var eventHandler = new EventHandler<TestEventRegistry>() {
            @Override
            public void handle(TestEventRegistry event) {
                // Nothing
            }

            @Override
            public SubscribeEvent getSubscribeEvent() {
                return subscribeEvent;
            }
        };
        eventRegistryConfig.registerHandler("test", TestEventRegistry.class, eventHandler);
        eventRegistryConfig.registerHandler("test2", TestEventRegistry.class, eventHandler);

        var result = eventRegistryConfig.getHandlers(TestEventRegistry.class)
                .stream()
                .map(Handler::name);

        assertThat(result).contains("test", "test2");
    }

    @Test
    void givenNoHandlers_whenGetHandlers_thenReturnEmpty() {
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
                return subscribeEvent;
            }
        });

        assertThat(eventRegistryConfig.getByHandlerName("test")).isPresent();
        assertThat(eventRegistryConfig.getHandlers(TestEventRegistry.class).stream().map(Handler::name))
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
                return subscribeEvent;
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
        }, 5, "");

        var handlers = eventRegistryConfig.getHandlers(TestEventRegistry.class);

        assertThat(handlers).hasSize(1);

        var handler = eventRegistryConfig.getByHandlerName(handlers.get(0).name());
        assertThat(handler).isPresent();
        assertThat(handler.get().retry()).isEqualTo(5);
        assertThat(handler.get().name()).isNotNull();
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
                return subscribeEvent;
            }
        });

        eventRegistryConfig.unregisterHandler(TestEventRegistry.class, "test");

        assertThat(eventRegistryConfig.getByHandlerName("test")).isEmpty();
        assertThat(eventRegistryConfig.getHandlers(TestEventRegistry.class)).isEmpty();
    }

    @Test
    void givenNoRegisteredEventHandler_whenUnregisterHandler_thenOnlyLog() {
        eventRegistryConfig.unregisterHandler(TestEventRegistry.class, "test");

        assertThat(eventRegistryConfig.getByHandlerName("test")).isEmpty();
        assertThat(eventRegistryConfig.getHandlers(TestEventRegistry.class)).isEmpty();
    }

    public record TestEventRegistry(String eventName) implements Event {
    }
}
