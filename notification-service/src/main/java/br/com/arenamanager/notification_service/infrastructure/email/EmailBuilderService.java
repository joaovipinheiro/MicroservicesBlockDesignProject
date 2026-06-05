package br.com.arenamanager.notification_service.infrastructure.email;

import br.com.arenamanager.notification_service.application.port.out.EmailTemplateRepository;
import br.com.arenamanager.notification_service.domain.exception.TemplateNotFoundException;
import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import br.com.arenamanager.notification_service.domain.model.EmailTemplate;
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PagamentoAprovadoEvent;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Constrói o {@link EmailMessage} a partir de um {@link PagamentoAprovadoEvent}
 * e do template MongoDB do tipo {@code PAGAMENTO_APROVADO}.
 *
 * <p>Valida: Requisitos 3.3, 3.4</p>
 */
@Component
public class EmailBuilderService {

    private static final String TEMPLATE_TYPE = "PAGAMENTO_APROVADO";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final EmailTemplateRepository templateRepository;

    public EmailBuilderService(EmailTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Constrói um {@link EmailMessage} interpolando os dados do evento no template.
     *
     * @param event evento de pagamento aprovado
     * @return e-mail pronto para envio
     * @throws TemplateNotFoundException se o template {@code PAGAMENTO_APROVADO} não for encontrado
     */
    public EmailMessage build(PagamentoAprovadoEvent event) {
        EmailTemplate template = templateRepository.findByType(TEMPLATE_TYPE)
                .orElseThrow(() -> new TemplateNotFoundException(TEMPLATE_TYPE));

        String body = interpolate(template.bodyHtml(), event);
        String subject = interpolate(template.subject(), event);

        return new EmailMessage(event.playerEmail(), subject, body, event.traceId());
    }

    private String interpolate(String template, PagamentoAprovadoEvent event) {
        String amount = event.amount() != null
                ? event.amount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                : "";
        String approvedAt = event.approvedAt() != null
                ? DATE_FMT.format(event.approvedAt())
                : "";

        return template
                .replace("{{playerName}}", nvl(event.playerName()))
                .replace("{{tournamentName}}", nvl(event.tournamentName()))
                .replace("{{amount}}", amount)
                .replace("{{currency}}", nvl(event.currency()))
                .replace("{{approvedAt}}", approvedAt);
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
