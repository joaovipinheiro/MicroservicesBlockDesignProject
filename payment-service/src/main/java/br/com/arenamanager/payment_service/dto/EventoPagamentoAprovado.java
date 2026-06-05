package br.com.arenamanager.payment_service.dto;

import java.math.BigDecimal;

public record EventoPagamentoAprovado(
        Long pagamentoId,
        String nomeJogador,
        String emailJogador,
        Long torneioId,
        BigDecimal valor
) {}