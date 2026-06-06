package br.com.arenamanager.player_service.dto;

public record PlayerResponseDTO(
        Long Id,
        String nome,
        String nickname,
        String email
) {
}
