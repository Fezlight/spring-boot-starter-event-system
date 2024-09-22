package fr.fezlight.eventsystem.config;

import fr.fezlight.eventsystem.annotation.SubscribeEvent;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.Handler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EventAutoConfigurationTest {

    @InjectMocks
    private EventAutoConfiguration eventAutoConfiguration;

    @Test
    void given1EventWith1Subscriber_whenEventRegistryConfigAutoconfigure_ThenEventRegistryContain1Handler() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.registerBean(Test1Listeners.class, Test1Listeners::new);
        applicationContext.refresh();

        var eventRegistry = eventAutoConfiguration.eventRegistryConfig(applicationContext);

        assertThat(eventRegistry).isNotNull();
        assertThat(eventRegistry.getHandlers(Test1Event.class).stream().map(Handler::name))
                .hasSize(1)
                .contains("Test1Listeners#handleEvent");
        assertThat(eventRegistry.getByHandlerName("Test1Listeners#handleEvent")).isPresent();
    }

    @Test
    void given1EventWith2SubscriberSameName_whenEventRegistryConfigAutoconfigure_ThenThrowException() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.registerBean(Test2Listeners.class, Test2Listeners::new);
        applicationContext.refresh();

        var e = assertThrows(IllegalArgumentException.class, () -> eventAutoConfiguration.eventRegistryConfig(applicationContext));

        assertThat(e.getMessage()).isEqualTo("Handler with name Test2Listeners#handleEvent already registered, " +
                                             "use 'customName' properties to define an alternative name");
    }

    @Test
    void given1EventWith2SubscriberWithCustomName_whenEventRegistryConfigAutoconfigure_ThenRegisterTwoHandlers() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.registerBean(Test3Listeners.class, Test3Listeners::new);
        applicationContext.refresh();

        var eventRegistry = eventAutoConfiguration.eventRegistryConfig(applicationContext);

        assertThat(eventRegistry).isNotNull();
        assertThat(eventRegistry.getHandlers(Test3Event.class).stream().map(Handler::name))
                .hasSize(1)
                .contains("Test3Listeners#handleEvent");
        assertThat(eventRegistry.getHandlers(Test3AltEvent.class).stream().map(Handler::name))
                .hasSize(1)
                .contains("handleEventCustom");
        assertThat(eventRegistry.getByHandlerName("Test3Listeners#handleEvent")).isPresent();
        assertThat(eventRegistry.getByHandlerName("handleEventCustom")).isPresent();
    }

    @Test
    void given1EventWith1SubscribersWithTwoParameter_whenEventRegistryConfigAutoconfigure_ThenThrowException() {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.registerBean(Test4Listeners.class, Test4Listeners::new);
        applicationContext.refresh();

        var e = assertThrows(IllegalArgumentException.class, () -> eventAutoConfiguration.eventRegistryConfig(applicationContext));

        assertThat(e.getMessage()).isEqualTo("Method annotated with @SubscribeEvent must have exactly one parameter");
    }

    @Component
    public record Test1Listeners() {
        @SubscribeEvent
        public void handleEvent(Test1Event event) {
            // Nothing
        }
    }

    @Component
    public record Test2Listeners() {
        @SubscribeEvent
        public void handleEvent(Test2Event event) {
            // Nothing
        }

        @SubscribeEvent
        public void handleEvent(Test2AltEvent event) {
            // Nothing
        }
    }

    @Component
    public record Test3Listeners() {
        @SubscribeEvent
        public void handleEvent(Test3Event event) {
            // Nothing
        }

        @SubscribeEvent(customName = "handleEventCustom")
        public void handleEvent(Test3AltEvent event) {
            // Nothing
        }
    }

    @Component
    public record Test4Listeners() {
        @SubscribeEvent
        public void handleEvent(Test4Event event, String secondArgs) {
            // Nothing
        }
    }

    public record Test1Event() implements Event {
    }

    public record Test2Event() implements Event {
    }

    public record Test2AltEvent() implements Event {
    }

    public record Test3Event() implements Event {
    }

    public record Test3AltEvent() implements Event {
    }

    public record Test4Event() implements Event {
    }
}
