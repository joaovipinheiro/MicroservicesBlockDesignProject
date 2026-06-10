package br.com.arenamanager.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;

public record SolicitacaoPagamentoDTO(

        @JsonAlias({"usuarioId", "playerId"})
        Long usuarioId,

        @JsonAlias({"torneioId", "tournamentId"})
        Long torneioId,

        BigDecimal valor
) {}