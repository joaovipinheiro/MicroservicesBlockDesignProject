package br.com.arenamanager.player_service.dto;

public record PlayerResponseDTO(
        Long id,
        String nome,
        String nickname,
        String email
) {
}
