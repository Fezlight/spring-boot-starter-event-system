package fr.fezlight.eventsystem.config.rabbitmq;

import com.rabbitmq.client.Channel;
import fr.fezlight.eventsystem.config.properties.EventProperties;
import fr.fezlight.eventsystem.models.EventWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

import java.util.Objects;

import static fr.fezlight.eventsystem.config.rabbitmq.EventQueueConfig.AMQP_RETRY_LEFT_HEADER;

public class RabbitListenerCustomErrorHandler implements RabbitListenerErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(RabbitListenerCustomErrorHandler.class);

    private final RabbitTemplate rabbitTemplate;
    private final EventProperties eventProperties;

    public RabbitListenerCustomErrorHandler(RabbitTemplate rabbitTemplate, EventProperties eventProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventProperties = eventProperties;
    }

    @Override
    public Object handleError(Message amqpMessage, org.springframework.messaging.Message<?> message, ListenerExecutionFailedException exception) {
        return null;
    }

    @Override
    public Object handleError(Message amqpMessage, Channel channel, org.springframework.messaging.Message<?> message, ListenerExecutionFailedException exception) {
        if (message == null || !(message.getPayload() instanceof EventWrapper<?> eventWrapper)) {
            throw new AmqpRejectAndDontRequeueException("Unable to handle message");
        }

        Integer retryLeftHeader = message.getHeaders().get(AMQP_RETRY_LEFT_HEADER, Integer.class);
        int retryLeft = Objects.requireNonNullElse(retryLeftHeader, eventWrapper.getRetryLeft());

        if (retryLeft > 0) {
            log.debug("Retry left = {}", retryLeft);
            rabbitTemplate.convertAndSend(
                    eventProperties.getRabbit().getQueue().getRetry().getExchange(),
                    eventProperties.getRabbit().getQueue().getRetry().getName(),
                    eventWrapper,
                    m -> MessageBuilder.fromMessage(amqpMessage)
                            .setHeader(AMQP_RETRY_LEFT_HEADER, retryLeft - 1)
                            .setReplyTo(amqpMessage.getMessageProperties().getConsumerQueue())
                            .build()
            );
        } else {
            throw new AmqpRejectAndDontRequeueException("Rejecting message no retries left");
        }

        return null;
    }
}
