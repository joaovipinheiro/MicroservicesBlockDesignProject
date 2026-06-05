package br.com.arenamanager.notification_service.domain.exception;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(String templateType) {
        super("Template not found: " + templateType);
    }
}
