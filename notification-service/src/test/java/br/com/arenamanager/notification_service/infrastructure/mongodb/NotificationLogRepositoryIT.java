package br.com.arenamanager.notification_service.infrastructure.mongodb;

import br.com.arenamanager.notification_service.infrastructure.mongodb.document.NotificationLogDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.NotificationLogMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link NotificationLogMongoRepository} using Flapdoodle embedded MongoDB.
 *
 * <p>Valida: Requisitos 5.1, 5.2, 2.3, 3.1, 7.2</p>
 */
@DataMongoTest
@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=7.0.0",
        "spring.data.mongodb.auto-index-creation=true"
})
class NotificationLogRepositoryIT {

    @Autowired
    private NotificationLogMongoRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // save + findByEventId
    // -------------------------------------------------------------------------

    @Test
    void save_and_findByEventId_should_work_correctly() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        NotificationLogDocument doc = buildDocument(eventId, "player-1", Instant.now());

        // Act
        repository.save(doc);
        Optional<NotificationLogDocument> found = repository.findByEventId(eventId);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEventId()).isEqualTo(eventId);
        assertThat(found.get().getPaymentId()).isEqualTo(doc.getPaymentId());
        assertThat(found.get().getPlayerId()).isEqualTo(doc.getPlayerId());
        assertThat(found.get().getPlayerEmail()).isEqualTo(doc.getPlayerEmail());
        assertThat(found.get().getStatus()).isEqualTo("SENT");
        assertThat(found.get().getTraceId()).isEqualTo(doc.getTraceId());
    }

    @Test
    void findByEventId_should_return_empty_when_not_found() {
        Optional<NotificationLogDocument> found = repository.findByEventId("non-existent-id");
        assertThat(found).isEmpty();
    }

    // -------------------------------------------------------------------------
    // existsByEventId
    // -------------------------------------------------------------------------

    @Test
    void existsByEventId_should_return_true_when_document_exists() {
        String eventId = UUID.randomUUID().toString();
        repository.save(buildDocument(eventId, "player-1", Instant.now()));

        assertThat(repository.existsByEventId(eventId)).isTrue();
    }

    @Test
    void existsByEventId_should_return_false_when_document_does_not_exist() {
        assertThat(repository.existsByEventId("non-existent-id")).isFalse();
    }

    // -------------------------------------------------------------------------
    // findByPlayerIdOrderBySentAtDesc
    // -------------------------------------------------------------------------

    @Test
    void findByPlayerIdOrderBySentAtDesc_should_return_records_in_descending_order() {
        String playerId = UUID.randomUUID().toString();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        // Inserir em ordem não-cronológica
        NotificationLogDocument oldest = buildDocument(UUID.randomUUID().toString(), playerId, now.minusSeconds(60));
        NotificationLogDocument middle = buildDocument(UUID.randomUUID().toString(), playerId, now.minusSeconds(30));
        NotificationLogDocument newest = buildDocument(UUID.randomUUID().toString(), playerId, now);

        repository.save(oldest);
        repository.save(newest);
        repository.save(middle);

        // Act
        Page<NotificationLogDocument> page = repository.findByPlayerIdOrderBySentAtDesc(
                playerId, PageRequest.of(0, 10));

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getSentAt()).isEqualTo(now);
        assertThat(page.getContent().get(1).getSentAt()).isEqualTo(now.minusSeconds(30));
        assertThat(page.getContent().get(2).getSentAt()).isEqualTo(now.minusSeconds(60));
    }

    @Test
    void findByPlayerIdOrderBySentAtDesc_should_not_return_documents_of_other_players() {
        String playerId1 = UUID.randomUUID().toString();
        String playerId2 = UUID.randomUUID().toString();
        Instant now = Instant.now();

        repository.save(buildDocument(UUID.randomUUID().toString(), playerId1, now));
        repository.save(buildDocument(UUID.randomUUID().toString(), playerId2, now));

        Page<NotificationLogDocument> page = repository.findByPlayerIdOrderBySentAtDesc(
                playerId1, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getPlayerId()).isEqualTo(playerId1);
    }

    @Test
    void findByPlayerIdOrderBySentAtDesc_should_return_empty_page_for_unknown_player() {
        Page<NotificationLogDocument> page = repository.findByPlayerIdOrderBySentAtDesc(
                "unknown-player", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Índice único em eventId
    // -------------------------------------------------------------------------

    @Test
    void unique_index_on_eventId_should_throw_DuplicateKeyException() {
        String eventId = UUID.randomUUID().toString();
        NotificationLogDocument first = buildDocument(eventId, "player-1", Instant.now());
        NotificationLogDocument duplicate = buildDocument(eventId, "player-2", Instant.now());

        repository.save(first);

        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private NotificationLogDocument buildDocument(String eventId, String playerId, Instant sentAt) {
        return new NotificationLogDocument(
                null,
                eventId,
                UUID.randomUUID().toString(),
                playerId,
                "player@example.com",
                "SENT",
                null,
                sentAt,
                UUID.randomUUID().toString()
        );
    }
}
