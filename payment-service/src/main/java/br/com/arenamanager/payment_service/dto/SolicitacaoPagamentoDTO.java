package br.com.arenamanager.payment_service.dto;

import java.math.BigDecimal;

public record SolicitacaoPagamentoDTO(Long usuarioId, Long torneioId, BigDecimal valor) {
}
