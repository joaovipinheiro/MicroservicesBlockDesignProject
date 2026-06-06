package br.com.arenamanager.notification_service.infrastructure.rest.dto;

/**
 * DTO representing a single notification log entry in REST responses.
 *
 * <p>Valida: Requisito 7.2</p>
 *
 * @param eventId      unique event identifier
 * @param paymentId    payment identifier
 * @param playerEmail  recipient e-mail address
 * @param status       notification status (SENT, FAILED, DUPLICATE)
 * @param errorMessage error description when status is FAILED, otherwise null
 * @param sentAt       ISO-8601 UTC timestamp of the processing attempt
 * @param traceId      distributed tracing identifier
 */
public record NotificationLogResponse(
        String eventId,
        String paymentId,
        String playerEmail,
        String status,
        String errorMessage,
        String sentAt,
        String traceId
) {
}
