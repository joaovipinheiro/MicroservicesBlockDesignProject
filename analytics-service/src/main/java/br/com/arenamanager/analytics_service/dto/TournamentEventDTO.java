package br.com.arenamanager.analytics_service.dto;

public record TournamentEventDTO(
        Long tournamentId,
        String name,
        String format
) {}