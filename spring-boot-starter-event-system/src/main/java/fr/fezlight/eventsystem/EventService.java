package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.config.properties.EventProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final RabbitTemplate rabbitTemplate;
    private final EventProperties eventProperties;

    public EventService(RabbitTemplate rabbitTemplate, EventProperties eventProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventProperties = eventProperties;
    }

    public void reprocessAllFailedMessage() {
        log.debug("Retrying all failed event...");

        while (rabbitTemplate.receiveAndReply(eventProperties.getRabbit().getQueue().getError().getName(), message -> message))
            ;
    }
}
