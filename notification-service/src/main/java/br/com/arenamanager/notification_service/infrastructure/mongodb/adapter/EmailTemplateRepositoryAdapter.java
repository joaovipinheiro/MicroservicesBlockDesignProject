package br.com.arenamanager.notification_service.infrastructure.mongodb.adapter;

import br.com.arenamanager.notification_service.application.port.out.EmailTemplateRepository;
import br.com.arenamanager.notification_service.domain.model.EmailTemplate;
import br.com.arenamanager.notification_service.infrastructure.mongodb.document.EmailTemplateDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.EmailTemplateMongoRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter that bridges the domain {@link EmailTemplateRepository} port with the
 * Spring Data MongoDB {@link EmailTemplateMongoRepository}.
 *
 * <p>Valida: Requisito 3.1</p>
 */
@Component
public class EmailTemplateRepositoryAdapter implements EmailTemplateRepository {

    private final EmailTemplateMongoRepository mongoRepository;

    public EmailTemplateRepositoryAdapter(EmailTemplateMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Optional<EmailTemplate> findByType(String type) {
        return mongoRepository.findByType(type).map(this::toDomain);
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private EmailTemplate toDomain(EmailTemplateDocument doc) {
        return new EmailTemplate(
                doc.getId(),
                doc.getType(),
                doc.getSubject(),
                doc.getBodyHtml()
        );
    }
}
