package br.com.arenamanager.notification_service.application.port.out;

import br.com.arenamanager.notification_service.domain.model.EmailTemplate;

import java.util.Optional;

public interface EmailTemplateRepository {

    Optional<EmailTemplate> findByType(String type);
}
