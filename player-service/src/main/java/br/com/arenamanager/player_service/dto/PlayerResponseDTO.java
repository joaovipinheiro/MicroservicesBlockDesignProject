package br.com.arenamanager.player_service.dto;

public record PlayerResponseDTO(
        Long id,
        String name,
        String nickname,
        String email
) {
}
