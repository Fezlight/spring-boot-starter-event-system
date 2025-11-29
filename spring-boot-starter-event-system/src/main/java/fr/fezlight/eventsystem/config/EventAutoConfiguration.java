package fr.fezlight.eventsystem.config;

import fr.fezlight.eventsystem.EventListeners;
import fr.fezlight.eventsystem.EventService;
import fr.fezlight.eventsystem.annotation.SubscribeEvent;
import fr.fezlight.eventsystem.config.properties.EventProperties;
import fr.fezlight.eventsystem.config.rabbitmq.EventQueueConfig;
import fr.fezlight.eventsystem.models.Event;
import fr.fezlight.eventsystem.models.EventHandler;
import fr.fezlight.eventsystem.models.EventWrapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static org.springframework.scheduling.annotation.Scheduled.CRON_DISABLED;
import static org.springframework.util.ObjectUtils.isEmpty;

@ConditionalOnProperty(
        value = "events.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@AutoConfiguration
@EnableConfigurationProperties(EventProperties.class)
@Import(EventQueueConfig.class)
public class EventAutoConfiguration {

    @Bean
    @SuppressWarnings("unchecked")
    public EventRegistryConfig eventRegistryConfig(ApplicationContext applicationContext) {
        var registry = new EventRegistryConfig();
        Collection<Object> beans = applicationContext.getBeansWithAnnotation(Component.class).values();

        for (Object bean : beans) {
            Class<?> o = AopProxyUtils.ultimateTargetClass(bean);
            List<Method> methods = Arrays.stream(o.getMethods())
                    .filter(method -> method.isAnnotationPresent(SubscribeEvent.class))
                    .toList();

            for (Method method : methods) {
                if (method.getParameterCount() != 1) {
                    throw new IllegalArgumentException("Method annotated with @SubscribeEvent must have exactly one parameter");
                }

                registry.registerHandler(
                        String.format("%s#%s", o.getSimpleName(), method.getName()),
                        (Class<Event>) method.getParameterTypes()[0],
                        new EventHandler<>() {
                            @Override
                            public void handle(Event event) {
                                try {
                                    method.invoke(bean, event);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public SubscribeEvent getSubscribeEvent() {
                                return method.getAnnotation(SubscribeEvent.class);
                            }
                        }
                );
            }
        }

        return registry;
    }

    @Bean
    public EventListeners eventListeners(EventRegistryConfig eventRegistryConfig,
                                         ApplicationEventPublisher applicationEventPublisher,
                                         Supplier<String> defaultWorkerQueueNaming) {
        return new EventListeners(
                eventRegistryConfig, applicationEventPublisher, defaultWorkerQueueNaming
        );
    }

    @Bean
    public EventService eventService(RabbitTemplate rabbitTemplate, EventProperties eventProperties) {
        return new EventService(rabbitTemplate, eventProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JacksonJsonMessageConverter producerJacksonJsonMessageConverter(JsonMapper jsonMapper) {
        var jacksonMessageConverter = new JacksonJsonMessageConverter(jsonMapper);
        jacksonMessageConverter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.TYPE_ID);
        return jacksonMessageConverter;
    }

    @Bean
    public String retryIncompleteEventsCron(EventProperties eventProperties) {
        if (eventProperties.getScheduledTask().isEnabled() && eventProperties.getScheduledTask().getIncompleteRetry().isEnabled()) {
            return eventProperties.getScheduledTask().getIncompleteRetry().getCron();
        }

        return CRON_DISABLED;
    }

    @Bean
    public String clearCompletedEventCron(EventProperties eventProperties) {
        if (eventProperties.getScheduledTask().isEnabled() && eventProperties.getScheduledTask().getCompleteClear().isEnabled()) {
            return eventProperties.getScheduledTask().getCompleteClear().getCron();
        }

        return CRON_DISABLED;
    }

    @Bean
    @ConditionalOnMissingBean(name = "defaultMainQueueNaming")
    Supplier<String> defaultMainQueueNaming(@Value("${spring.application.name}") String applicationName,
                                            @Value("${events.rabbit.queue.main.name:}") String alternateMainQueueName) {
        if (!isEmpty(alternateMainQueueName)) {
            return () -> alternateMainQueueName;
        }
        return () -> "events." + applicationName.toLowerCase() + ".main";
    }

    @Bean
    @ConditionalOnMissingBean(name = "defaultWorkerQueueNaming")
    Supplier<String> defaultWorkerQueueNaming(@Value("${spring.application.name}") String applicationName,
                                              @Value("${events.rabbit.queue.worker.name:}") String alternateWorkerQueueName) {
        if (!isEmpty(alternateWorkerQueueName)) {
            return () -> alternateWorkerQueueName;
        }
        return () -> "events." + applicationName.toLowerCase() + ".worker";
    }

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration(
            @Qualifier("defaultMainQueueNaming") Supplier<String> defaultMainQueueNaming,
            @Qualifier("defaultWorkerQueueNaming") Supplier<String> defaultWorkerQueueNaming,
            @Value("${events.rabbit.queue.main.direct-exchange:events.direct}") String directExchange,
            @Value("${events.rabbit.queue.main.exchange:events}") String exchange
    ) {
        return EventExternalizationConfiguration.externalizing()
                .select(EventExternalizationConfiguration.annotatedAsExternalized())
                .route(EventWrapper.class, it -> RoutingTarget.forTarget(directExchange).andKey(defaultWorkerQueueNaming.get()))
                .route(Event.class, it -> RoutingTarget.forTarget(exchange).andKey(defaultMainQueueNaming.get()))
                .build();
    }
}
