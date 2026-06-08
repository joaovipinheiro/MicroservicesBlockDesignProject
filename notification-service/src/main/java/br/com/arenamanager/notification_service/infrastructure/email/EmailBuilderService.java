package br.com.arenamanager.notification_service.infrastructure.email;

import br.com.arenamanager.notification_service.application.port.out.EmailTemplateRepository;
import br.com.arenamanager.notification_service.domain.exception.TemplateNotFoundException;
import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import br.com.arenamanager.notification_service.domain.model.EmailTemplate;
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PagamentoAprovadoEvent;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

/**
 * Constrói o {@link EmailMessage} a partir de um {@link PagamentoAprovadoEvent}
 * e do template MongoDB do tipo {@code PAGAMENTO_APROVADO}.
 *
 * <p>Valida: Requisitos 3.3, 3.4</p>
 */
@Component
public class EmailBuilderService {

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
    public EmailMessage build(PagamentoAprovadoEvent event) {
        EmailTemplate template = templateRepository.findByType(TEMPLATE_TYPE)
                .orElseThrow(() -> new TemplateNotFoundException(TEMPLATE_TYPE));

        String body = interpolate(template.bodyHtml(), event);
        String subject = interpolate(template.subject(), event);

        return new EmailMessage(event.emailJogador(), subject, body,
                String.valueOf(event.pagamentoId()));
    }

    private String interpolate(String template, PagamentoAprovadoEvent event) {
        String amount = event.valor() != null
                ? event.valor().setScale(2, RoundingMode.HALF_UP).toPlainString()
                : "";

        return template
                .replace("{{playerName}}", nvl(event.nomeJogador()))
                .replace("{{tournamentName}}", nvl(String.valueOf(event.torneioId())))
                .replace("{{amount}}", amount)
                .replace("{{currency}}", "BRL")
                .replace("{{approvedAt}}", "");
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
