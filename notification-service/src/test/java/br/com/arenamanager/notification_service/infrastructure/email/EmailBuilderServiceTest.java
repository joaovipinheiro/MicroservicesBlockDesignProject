package br.com.arenamanager.notification_service.infrastructure.email;

import br.com.arenamanager.notification_service.application.port.out.EmailTemplateRepository;
import br.com.arenamanager.notification_service.domain.exception.TemplateNotFoundException;
import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import br.com.arenamanager.notification_service.domain.model.EmailTemplate;
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PagamentoAprovadoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link EmailBuilderService}.
 *
 * <p>Valida: Requisitos 3.1, 3.2, 3.3, 3.4</p>
 */
@ExtendWith(MockitoExtension.class)
class EmailBuilderServiceTest {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{[a-zA-Z]+}}");

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @InjectMocks
    private EmailBuilderService emailBuilderService;

    private PagamentoAprovadoEvent event;

    @BeforeEach
    void setUp() {
        // approvedAt: 2026-01-15T10:30:00Z → dd/MM/yyyy HH:mm UTC = "15/01/2026 10:30"
        Instant approvedAt = Instant.parse("2026-01-15T10:30:00Z");
        event = new PagamentoAprovadoEvent(
                "event-id-001",
                "payment-id-001",
                "player-id-001",
                "jogador@exemplo.com",
                "João Silva",
                "tournament-id-001",
                "Torneio de Verão",
                new BigDecimal("99.9"),
                "BRL",
                approvedAt,
                "trace-abc-123"
        );
    }

    @Test
    @DisplayName("Deve interpolar corretamente todos os placeholders no body HTML")
    void shouldInterpolateAllPlaceholdersInBody() {
        String bodyHtml = "<h1>Olá, {{playerName}}!</h1>"
                + "<p>Torneio: {{tournamentName}}</p>"
                + "<p>Valor: {{amount}} {{currency}}</p>"
                + "<p>Aprovado em: {{approvedAt}}</p>";
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO",
                "Pagamento Aprovado", bodyHtml);

        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO")))
                .thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.body())
                .contains("João Silva")
                .contains("Torneio de Verão")
                .contains("99.90")
                .contains("BRL")
                .contains("15/01/2026 10:30");
    }

    @Test
    @DisplayName("Deve formatar amount com exatamente 2 casas decimais usando Locale.US")
    void shouldFormatAmountWithTwoDecimalPlaces() {
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO",
                "Assunto", "Valor: {{amount}}");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO")))
                .thenReturn(Optional.of(template));

        // amount = 99.9 → deve ser "99.90" (Locale.US, sem separador de milhar)
        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.body()).contains("99.90");
        assertThat(result.body()).doesNotContain("99.9 ");
    }

    @Test
    @DisplayName("Deve formatar approvedAt como dd/MM/yyyy HH:mm em UTC")
    void shouldFormatApprovedAtAsDateTimeUTC() {
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO",
                "Assunto", "Data: {{approvedAt}}");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO")))
                .thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.body()).contains("15/01/2026 10:30");
    }

    @Test
    @DisplayName("Nenhum placeholder residual {{...}} deve permanecer no body após interpolação")
    void shouldLeaveNoResidualPlaceholdersInBody() {
        String bodyHtml = "{{playerName}} {{tournamentName}} {{amount}} {{currency}} {{approvedAt}}";
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO",
                "Assunto", bodyHtml);
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO")))
                .thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(PLACEHOLDER_PATTERN.matcher(result.body()).find())
                .as("Body não deve conter nenhum placeholder residual {{...}}")
                .isFalse();
    }

    @Test
    @DisplayName("Deve lançar TemplateNotFoundException quando o template não for encontrado")
    void shouldThrowTemplateNotFoundExceptionWhenTemplateAbsent() {
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailBuilderService.build(event))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    @DisplayName("EmailMessage.to deve ser igual a event.playerEmail()")
    void shouldSetToAsPlayerEmail() {
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO",
                "Assunto", "Corpo");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO")))
                .thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.to()).isEqualTo(event.playerEmail());
    }

    @Test
    @DisplayName("EmailMessage.traceId deve ser igual a event.traceId()")
    void shouldSetTraceIdFromEvent() {
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO",
                "Assunto", "Corpo");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO")))
                .thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.traceId()).isEqualTo(event.traceId());
    }

    @Test
    @DisplayName("EmailMessage.subject deve ser o subject do template (interpolado se houver placeholder)")
    void shouldSetSubjectFromTemplateInterpolated() {
        // Subject com placeholder para verificar que também é interpolado
        String subjectWithPlaceholder = "Pagamento Aprovado — {{tournamentName}}";
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO",
                subjectWithPlaceholder, "Corpo");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO")))
                .thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.subject()).isEqualTo("Pagamento Aprovado — Torneio de Verão");
        assertThat(PLACEHOLDER_PATTERN.matcher(result.subject()).find())
                .as("Subject não deve conter nenhum placeholder residual")
                .isFalse();
    }
}
