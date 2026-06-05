package br.com.arenamanager.notification_service.infrastructure.email;

import br.com.arenamanager.notification_service.application.port.out.EmailSenderPort;
import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Implementation of {@link EmailSenderPort} that uses Spring's {@link JavaMailSender}.
 *
 * <p>Valida: Requisitos 4.1, 4.2, 4.3</p>
 */
@Component
public class JavaMailEmailSender implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(JavaMailEmailSender.class);

    private final JavaMailSender mailSender;

    public JavaMailEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body(), true);
            mailSender.send(mimeMessage);

            log.info("Email sent successfully: to={}, traceId={}, sentAt={}",
                    message.to(), message.traceId(), Instant.now());
        } catch (MessagingException ex) {
            throw new MailException("Failed to build MIME message: " + ex.getMessage(), ex) {};
        }
        // MailException propagates naturally from mailSender.send()
    }
}
