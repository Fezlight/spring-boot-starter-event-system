package fr.fezlight.eventsystem.config.rabbitmq;


import fr.fezlight.eventsystem.config.properties.EventProperties;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.function.Supplier;

@ConditionalOnProperty(
        value = "events.rabbit.queue.autoconfigure",
        havingValue = "true",
        matchIfMissing = true
)
public class EventQueueConfig {
    public static final String AMQP_RETRY_LEFT_HEADER = "retry_left";

    private final EventProperties eventProperties;
    private final Supplier<String> defaultMainQueueNaming;

    public EventQueueConfig(EventProperties eventProperties, Supplier<String> defaultMainQueueNaming) {
        this.eventProperties = eventProperties;
        this.defaultMainQueueNaming = defaultMainQueueNaming;
    }

    @Bean("eventsMain")
    @ConditionalOnMissingBean(name = "eventsMain")
    Declarables eventsMain() {
        Queue queue = QueueBuilder.durable(defaultMainQueueNaming.get())
                .deadLetterExchange(eventProperties.getRabbit().getQueue().getError().getExchange())
                .deadLetterRoutingKey(eventProperties.getRabbit().getQueue().getError().getName())
                .build();
        DirectExchange directExchange = ExchangeBuilder.directExchange(eventProperties.getRabbit().getQueue().getMain().getDirectExchange())
                .build();
        FanoutExchange fanoutExchange = new FanoutExchange(eventProperties.getRabbit().getQueue().getMain().getExchange());

        return new Declarables(
                queue,
                directExchange,
                fanoutExchange,
                BindingBuilder.bind(queue).to(directExchange).withQueueName(),
                BindingBuilder.bind(queue).to(fanoutExchange)
        );
    }

    @Bean("eventsDeadLetter")
    @ConditionalOnMissingBean(name = "eventsDeadLetter")
    Declarables eventsDeadLetter() {
        Queue queue = QueueBuilder.durable(eventProperties.getRabbit().getQueue().getError().getName())
                .build();
        DirectExchange directExchange = ExchangeBuilder.directExchange(eventProperties.getRabbit().getQueue().getError().getExchange())
                .build();

        return new Declarables(
                queue,
                directExchange,
                BindingBuilder.bind(queue).to(directExchange).withQueueName()
        );
    }

    @Bean("eventsRetry")
    @ConditionalOnMissingBean(name = "eventsRetry")
    Declarables eventsRetry() {
        Queue queue = QueueBuilder.durable(eventProperties.getRabbit().getQueue().getRetry().getName())
                .deadLetterExchange(eventProperties.getRabbit().getQueue().getMain().getExchange())
                .ttl((int) eventProperties.getRabbit().getQueue().getRetry().getTimeBetweenRetries().toMillis())
                .build();
        DirectExchange directExchange = ExchangeBuilder.directExchange(eventProperties.getRabbit().getQueue().getRetry().getExchange())
                .build();

        return new Declarables(
                queue,
                directExchange,
                BindingBuilder.bind(queue).to(directExchange).withQueueName()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitListenerErrorHandler rabbitListenerCustomErrorHandler(RabbitTemplate rabbitTemplate,
                                                                       EventProperties eventProperties) {
        return new RabbitListenerCustomErrorHandler(rabbitTemplate, eventProperties);
    }
}
