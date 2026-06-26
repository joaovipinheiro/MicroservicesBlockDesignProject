package br.com.arenamanager.notification_service.application.port.in;

import br.com.arenamanager.notification_service.infrastructure.kafka.event.PaymentApprovedEvent;

/**
 * Porta de entrada (use case) para processamento de notificações de pagamento aprovado.
 *
 * <p>Valida: Requisito 1.1</p>
 */
public interface ProcessPaymentNotificationUseCase {

    /**
     * Processa a notificação de pagamento aprovado, enviando e-mail ao jogador
     * e persistindo o log de notificação.
     *
     * @param event evento de pagamento aprovado consumido do Kafka
     */
    void processPaymentApprovedNotification(PaymentApprovedEvent event);
}
