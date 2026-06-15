package br.com.arenamanager.analytics_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TournamentEventDTO(

        @JsonProperty("tournamentId")
        Long id,

        @JsonProperty("name")
        String nome,

        @JsonProperty("format")
        String formato
) {}