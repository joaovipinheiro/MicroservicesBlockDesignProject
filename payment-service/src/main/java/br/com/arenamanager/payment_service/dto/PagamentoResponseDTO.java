package br.com.arenamanager.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponseDTO(
        Long id,
        Long usuarioId,
        Long torneioId,
        BigDecimal valor,
        String status,
        LocalDateTime dataCriacao
) {
}
