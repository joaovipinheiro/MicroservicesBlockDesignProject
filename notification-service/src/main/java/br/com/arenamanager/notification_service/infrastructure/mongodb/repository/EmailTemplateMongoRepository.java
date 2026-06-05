package br.com.arenamanager.notification_service.infrastructure.mongodb.repository;

import br.com.arenamanager.notification_service.infrastructure.mongodb.document.EmailTemplateDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the {@code email_templates} collection.
 *
 * <p>Valida: Requisito 3.1</p>
 */
public interface EmailTemplateMongoRepository
        extends MongoRepository<EmailTemplateDocument, String> {

    Optional<EmailTemplateDocument> findByType(String type);
}
