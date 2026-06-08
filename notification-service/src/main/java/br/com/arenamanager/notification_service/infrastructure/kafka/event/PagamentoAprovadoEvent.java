package br.com.arenamanager.notification_service.infrastructure.kafka.event;

import java.math.BigDecimal;

/**
 * Evento publicado pelo payment-service no tópico {@code pagamentos-aprovados}.
 * Os campos seguem o contrato definido pelo payment-service.
 */
public record PagamentoAprovadoEvent(
        Long pagamentoId,
        String nomeJogador,
        String emailJogador,
        Long torneioId,
        BigDecimal valor
) {}
