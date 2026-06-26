package br.com.arenamanager.notification_service.infrastructure.email;

import br.com.arenamanager.notification_service.application.port.out.EmailTemplateRepository;
import br.com.arenamanager.notification_service.domain.exception.TemplateNotFoundException;
import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import br.com.arenamanager.notification_service.domain.model.EmailTemplate;
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PaymentApprovedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailBuilderServiceTest {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{[a-zA-Z]+}}");

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @InjectMocks
    private EmailBuilderService emailBuilderService;

    private PaymentApprovedEvent event;

    @BeforeEach
    void setUp() {
        event = new PaymentApprovedEvent(
                1L,
                "João Silva",
                "jogador@exemplo.com",
                2L,
                new BigDecimal("99.9")
        );
    }

    @Test
    @DisplayName("Deve interpolar playerName e amount no body HTML")
    void shouldInterpolatePlayerNameAndAmountInBody() {
        String bodyHtml = "<h1>Olá, {{playerName}}!</h1><p>Valor: {{amount}} {{currency}}</p>";
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO", "Pagamento Aprovado", bodyHtml);
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO"))).thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.body()).contains("João Silva").contains("99.90").contains("BRL");
    }

    @Test
    @DisplayName("Deve formatar amount com exatamente 2 casas decimais")
    void shouldFormatAmountWithTwoDecimalPlaces() {
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO", "Assunto", "Valor: {{amount}}");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO"))).thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.body()).contains("99.90");
    }

    @Test
    @DisplayName("Nenhum placeholder residual deve permanecer no body após interpolação")
    void shouldLeaveNoResidualPlaceholdersInBody() {
        String bodyHtml = "{{playerName}} {{tournamentName}} {{amount}} {{currency}} {{approvedAt}}";
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO", "Assunto", bodyHtml);
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO"))).thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(PLACEHOLDER_PATTERN.matcher(result.body()).find())
                .as("Body não deve conter nenhum placeholder residual {{...}}")
                .isFalse();
    }

    @Test
    @DisplayName("Deve lançar TemplateNotFoundException quando o template não for encontrado")
    void shouldThrowTemplateNotFoundExceptionWhenTemplateAbsent() {
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailBuilderService.build(event))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    @DisplayName("EmailMessage.to deve ser igual a emailJogador do evento")
    void shouldSetToAsEmailJogador() {
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO", "Assunto", "Corpo");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO"))).thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.to()).isEqualTo("jogador@exemplo.com");
    }

    @Test
    @DisplayName("EmailMessage.traceId deve ser o pagamentoId como string")
    void shouldSetTraceIdAsPagamentoId() {
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO", "Assunto", "Corpo");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO"))).thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.traceId()).isEqualTo("1");
    }

    @Test
    @DisplayName("Subject não deve conter placeholders residuais após interpolação")
    void shouldLeaveNoResidualPlaceholdersInSubject() {
        String subjectWithPlaceholder = "Pagamento Aprovado — {{tournamentName}}";
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO", subjectWithPlaceholder, "Corpo");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO"))).thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(PLACEHOLDER_PATTERN.matcher(result.subject()).find())
                .as("Subject não deve conter nenhum placeholder residual")
                .isFalse();
    }

    @Test
    @DisplayName("Deve interpolar corretamente currency como BRL")
    void shouldInterpolateCurrencyAsBRL() {
        EmailTemplate template = new EmailTemplate("t1", "PAGAMENTO_APROVADO", "Assunto", "Moeda: {{currency}}");
        when(emailTemplateRepository.findByType(eq("PAGAMENTO_APROVADO"))).thenReturn(Optional.of(template));

        EmailMessage result = emailBuilderService.build(event);

        assertThat(result.body()).contains("BRL");
    }
}
