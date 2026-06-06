package br.com.arenamanager.notification_service.infrastructure.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document for email_templates collection.
 *
 * <p>Valida: Requisitos 3.1, 2.3</p>
 */
@Document(collection = "email_templates")
public class EmailTemplateDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String type;

    private String subject;

    private String bodyHtml;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public EmailTemplateDocument() {
    }

    public EmailTemplateDocument(String id, String type, String subject, String bodyHtml) {
        this.id = id;
        this.type = type;
        this.subject = subject;
        this.bodyHtml = bodyHtml;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
}
