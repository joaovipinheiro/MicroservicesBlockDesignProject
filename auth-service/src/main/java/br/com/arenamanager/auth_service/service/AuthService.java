package br.com.arenamanager.auth_service.service;

import br.com.arenamanager.auth_service.dto.*;
import br.com.arenamanager.auth_service.model.Usuario;
import br.com.arenamanager.auth_service.repository.UsuarioRepository;
import br.com.arenamanager.auth_service.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email ja cadastrado: " + request.email());
        }
        Usuario usuario = Usuario.builder()
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .role("USER")
                .build();
        usuarioRepository.save(usuario);
        log.info("Usuario registrado: {}", request.email());
    }

    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new IllegalArgumentException("Credenciais invalidas");
        }

        String accessToken = jwtService.gerarAccessToken(usuario.getEmail(), usuario.getRole());
        String refreshToken = jwtService.gerarRefreshToken(usuario.getEmail());

        log.info("Login realizado: {}", usuario.getEmail());
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getExpirationMs());
    }

    public TokenResponse refresh(RefreshRequest request) {
        Claims claims = jwtService.validarToken(request.refreshToken());

        if (!jwtService.isRefreshToken(claims)) {
            throw new IllegalArgumentException("Token informado nao e um refresh token");
        }

        String email = claims.getSubject();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));

        String novoAccessToken = jwtService.gerarAccessToken(usuario.getEmail(), usuario.getRole());
        String novoRefreshToken = jwtService.gerarRefreshToken(usuario.getEmail());

        log.info("Token renovado para: {}", email);
        return new TokenResponse(novoAccessToken, novoRefreshToken, "Bearer", jwtService.getExpirationMs());
    }
}
