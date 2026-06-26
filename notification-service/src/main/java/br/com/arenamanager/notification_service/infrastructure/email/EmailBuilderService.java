package br.com.arenamanager.notification_service.infrastructure.email;

import br.com.arenamanager.notification_service.application.port.out.EmailTemplateRepository;
import br.com.arenamanager.notification_service.domain.exception.TemplateNotFoundException;
import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import br.com.arenamanager.notification_service.domain.model.EmailTemplate;
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PaymentApprovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

/**
 * Constrói o {@link EmailMessage} a partir de um {@link PaymentApprovedEvent}
 * e do template MongoDB do tipo {@code PAGAMENTO_APROVADO}.
 *
 * <p>Valida: Requisitos 3.3, 3.4</p>
 */
@Component
public class EmailBuilderService {

    private static final Logger log = LoggerFactory.getLogger(EmailBuilderService.class);
    private static final String TEMPLATE_TYPE = "PAGAMENTO_APROVADO";

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
    public EmailMessage build(PaymentApprovedEvent event) {
        log.debug("Buscando template: type={}", TEMPLATE_TYPE);
        EmailTemplate template = templateRepository.findByType(TEMPLATE_TYPE)
                .orElseThrow(() -> {
                    log.error("Template não encontrado: type={}", TEMPLATE_TYPE);
                    return new TemplateNotFoundException(TEMPLATE_TYPE);
                });

        log.debug("Template encontrado, interpolando dados: eventId={}", event.paymentId());
        String body = interpolate(template.bodyHtml(), event);
        String subject = interpolate(template.subject(), event);

        return new EmailMessage(event.playerEmail(), subject, body,
                String.valueOf(event.paymentId()));
    }

    private String interpolate(String template, PaymentApprovedEvent event) {
        String amount = event.amount() != null
                ? event.amount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                : "";

        return template
                .replace("{{playerName}}", nvl(event.playerName()))
                .replace("{{tournamentName}}", nvl(String.valueOf(event.tournamentId())))
                .replace("{{amount}}", amount)
                .replace("{{currency}}", "BRL")
                .replace("{{approvedAt}}", "");
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
