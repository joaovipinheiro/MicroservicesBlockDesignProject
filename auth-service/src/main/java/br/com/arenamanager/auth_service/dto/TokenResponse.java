package br.com.arenamanager.auth_service.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tipo,
        long expiresInMs
) {}
