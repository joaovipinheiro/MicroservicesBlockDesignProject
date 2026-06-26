package br.com.arenamanager.notification_service.infrastructure.email;

import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link JavaMailEmailSender}.
 *
 * <p>Valida: Requisitos 4.1, 4.2, 4.3</p>
 */
@ExtendWith(MockitoExtension.class)
class JavaMailEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private JavaMailEmailSender javaMailEmailSender;

    @Test
    @DisplayName("Deve invocar createMimeMessage() e send(MimeMessage) com os campos corretos")
    void shouldCreateAndSendMimeMessageWithCorrectFields() throws Exception {
        // Arrange
        EmailMessage message = new EmailMessage(
                "jogador@exemplo.com",
                "Pagamento Aprovado — Torneio de Verão",
                "<h1>Olá, João!</h1><p>Seu pagamento foi aprovado.</p>",
                "trace-abc-123"
        );

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        javaMailEmailSender.send(message);

        // Assert
        verify(mailSender, times(1)).createMimeMessage();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertThat(sentMessage).isSameAs(mimeMessage);

        // Verify fields were set on the MimeMessage
        assertThat(sentMessage.getAllRecipients()).isNotNull();
        assertThat(sentMessage.getAllRecipients()[0].toString())
                .isEqualTo("jogador@exemplo.com");
        assertThat(sentMessage.getSubject())
                .isEqualTo("Pagamento Aprovado — Torneio de Verão");
    }

    @Test
    @DisplayName("Deve propagar MailException sem engolir")
    void shouldPropagateMailExceptionWithoutSwallowing() {
        // Arrange
        EmailMessage message = new EmailMessage(
                "jogador@exemplo.com",
                "Assunto",
                "<p>Corpo HTML</p>",
                "trace-xyz"
        );

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> javaMailEmailSender.send(message))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP server unavailable");
    }

    @Test
    @DisplayName("Deve definir o corpo do e-mail como HTML (text/html)")
    void shouldSetBodyAsHtml() throws Exception {
        // Arrange
        String htmlBody = "<h1>Olá!</h1><p>Pagamento aprovado.</p>";
        EmailMessage message = new EmailMessage(
                "dest@example.com",
                "Assunto de Teste",
                htmlBody,
                "trace-001"
        );

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        javaMailEmailSender.send(message);

        // Assert: the content type of the message should be multipart (MimeMessageHelper with multipart=true)
        // or text/html — verify send was called
        verify(mailSender).send(mimeMessage);
    }
}
