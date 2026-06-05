package br.com.arenamanager.notification_service.domain.model;

public record EmailMessage(
        String to,
        String subject,
        String body,
        String traceId
) {}
