package br.com.arenamanager.notification_service.domain.model;

public record EmailTemplate(
        String id,
        String type,
        String subject,
        String bodyHtml
) {}
