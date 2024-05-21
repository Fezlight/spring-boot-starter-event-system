package fr.fezlight.eventsystem;

import fr.fezlight.eventsystem.config.AppConfiguration;
import fr.fezlight.eventsystem.config.EventSchedulingTaskConfig;
import fr.fezlight.eventsystem.models.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.core.EventPublicationRepository;
import org.springframework.modulith.events.core.PublicationTargetIdentifier;
import org.springframework.modulith.events.core.TargetEventPublication;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ContextConfiguration(classes = AppConfiguration.class)
@Import(EventSchedulingTaskConfig.class)
public class EventSchedulingTaskIT {
    @Autowired
    private EventSchedulingTaskConfig eventSchedulingTaskConfig;

    @Autowired
    private EventPublicationRepository eventPublicationRepository;

    @Test
    void givenPublishedEventCompletedMoreThan1Minutes_whenClear_ThenEmptyTable() {
        var publications = TargetEventPublication.of(
                new TestEventSchedulingEvent("test"), PublicationTargetIdentifier.of(UUID.randomUUID().toString())
        );
        eventPublicationRepository.create(publications);
        eventPublicationRepository.markCompleted(publications, ZonedDateTime.now().minusMinutes(2).toInstant());

        var completedEventPublications = eventPublicationRepository.findCompletedPublications();
        assertThat(completedEventPublications).hasSize(1);

        eventSchedulingTaskConfig.clearCompletedEvent();

        completedEventPublications = eventPublicationRepository.findCompletedPublications();
        assertThat(completedEventPublications).hasSize(0);
    }

    @Test
    void givenPublishedEventCompletedLessThan1Minutes_whenClear_ThenOneEntryLeft() {
        var publications = TargetEventPublication.of(
                new TestEventSchedulingEvent("test"), PublicationTargetIdentifier.of(UUID.randomUUID().toString())
        );
        eventPublicationRepository.create(publications);
        eventPublicationRepository.markCompleted(publications, ZonedDateTime.now().toInstant());

        var completedEventPublications = eventPublicationRepository.findCompletedPublications();
        assertThat(completedEventPublications).hasSize(1);

        eventSchedulingTaskConfig.clearCompletedEvent();

        completedEventPublications = eventPublicationRepository.findCompletedPublications();
        assertThat(completedEventPublications).hasSize(1);

        // Cleanup
        eventPublicationRepository.deleteCompletedPublications();
    }

    @Test
    void givenPublishedEventImpletedMoreThan1Minutes_whenRetry_ThenCompleteEvent() {
        var publications = TargetEventPublication.of(
                new TestEventSchedulingEvent("test"),
                PublicationTargetIdentifier.of("org.springframework.modulith.events.support.DelegatingEventExternalizer.externalize(java.lang.Object)"),
                ZonedDateTime.now().minusMinutes(2).toInstant()
        );
        eventPublicationRepository.create(publications);

        var completedEventPublications = eventPublicationRepository.findIncompletePublications();
        assertThat(completedEventPublications).hasSize(1);

        eventSchedulingTaskConfig.retryIncompleteEvents();

        await().atMost(Duration.ofMillis(200)).until(() -> !eventPublicationRepository.findCompletedPublications().isEmpty());

        completedEventPublications = eventPublicationRepository.findCompletedPublications();
        assertThat(completedEventPublications).hasSize(1);

        // Cleanup
        eventPublicationRepository.deleteCompletedPublications();
    }

    public record TestEventSchedulingEvent(String name) implements Event {
    }
}
