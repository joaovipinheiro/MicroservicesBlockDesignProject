package br.com.arenamanager.notification_service.infrastructure.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document for notification_logs collection.
 *
 * <p>Valida: Requisitos 5.1, 5.2, 2.3</p>
 */
@Document(collection = "notification_logs")
@CompoundIndexes({
        @CompoundIndex(name = "playerId_sentAt_idx", def = "{'playerId': 1, 'sentAt': -1}")
})
public class NotificationLogDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    private String paymentId;

    private String playerId;

    private String playerEmail;

    private String status;

    private String errorMessage;

    private Instant sentAt;

    private String traceId;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public NotificationLogDocument() {
    }

    public NotificationLogDocument(String id, String eventId, String paymentId,
                                    String playerId, String playerEmail,
                                    String status, String errorMessage,
                                    Instant sentAt, String traceId) {
        this.id = id;
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.playerId = playerId;
        this.playerEmail = playerEmail;
        this.status = status;
        this.errorMessage = errorMessage;
        this.sentAt = sentAt;
        this.traceId = traceId;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerEmail() { return playerEmail; }
    public void setPlayerEmail(String playerEmail) { this.playerEmail = playerEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
}
