package br.com.arenamanager.notification_service.infrastructure.mongodb;

import br.com.arenamanager.notification_service.infrastructure.mongodb.document.EmailTemplateDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.EmailTemplateMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link EmailTemplateMongoRepository} using Flapdoodle embedded MongoDB.
 *
 * <p>Valida: Requisitos 3.1, 2.3, 5.2</p>
 */
@DataMongoTest
@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=7.0.0",
        "spring.data.mongodb.auto-index-creation=true"
})
class EmailTemplateRepositoryIT {

    @Autowired
    private EmailTemplateMongoRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // save + findByType
    // -------------------------------------------------------------------------

    @Test
    void save_and_findByType_should_work_correctly() {
        // Arrange
        EmailTemplateDocument doc = new EmailTemplateDocument(
                null,
                "PAGAMENTO_APROVADO",
                "Pagamento Aprovado — {{tournamentName}}",
                "<h1>Olá, {{playerName}}!</h1>"
        );

        // Act
        repository.save(doc);
        Optional<EmailTemplateDocument> found = repository.findByType("PAGAMENTO_APROVADO");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo("PAGAMENTO_APROVADO");
        assertThat(found.get().getSubject()).isEqualTo("Pagamento Aprovado — {{tournamentName}}");
        assertThat(found.get().getBodyHtml()).isEqualTo("<h1>Olá, {{playerName}}!</h1>");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void findByType_should_return_empty_when_not_found() {
        Optional<EmailTemplateDocument> found = repository.findByType("NON_EXISTENT_TYPE");
        assertThat(found).isEmpty();
    }

    @Test
    void save_should_persist_all_fields_correctly() {
        // Arrange
        String subject = "Test Subject";
        String bodyHtml = "<p>Test Body</p>";
        EmailTemplateDocument doc = new EmailTemplateDocument(null, "TEST_TYPE", subject, bodyHtml);

        // Act
        EmailTemplateDocument saved = repository.save(doc);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getType()).isEqualTo("TEST_TYPE");
        assertThat(saved.getSubject()).isEqualTo(subject);
        assertThat(saved.getBodyHtml()).isEqualTo(bodyHtml);
    }

    // -------------------------------------------------------------------------
    // Índice único em type
    // -------------------------------------------------------------------------

    @Test
    void unique_index_on_type_should_throw_DuplicateKeyException() {
        EmailTemplateDocument first = new EmailTemplateDocument(
                null, "PAGAMENTO_APROVADO", "Subject 1", "<p>Body 1</p>");
        EmailTemplateDocument duplicate = new EmailTemplateDocument(
                null, "PAGAMENTO_APROVADO", "Subject 2", "<p>Body 2</p>");

        repository.save(first);

        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
