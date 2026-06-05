package br.com.arenamanager.notification_service.domain.exception;

public class DuplicateNotificationException extends RuntimeException {

    public DuplicateNotificationException(String eventId) {
        super("Duplicate notification for eventId: " + eventId);
    }
}
