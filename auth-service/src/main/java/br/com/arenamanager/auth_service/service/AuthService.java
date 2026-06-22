package br.com.arenamanager.auth_service.service;

import br.com.arenamanager.auth_service.dto.*;
import br.com.arenamanager.auth_service.model.User;
import br.com.arenamanager.auth_service.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void registrar(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email ja cadastrado: " + request.email());
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role("USER")
                .build();
        userRepository.save(user);
        log.info("User registrado: {}", request.email());
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Credenciais invalidas");
        }

        String accessToken = jwtService.gerarAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtService.gerarRefreshToken(user.getEmail());

        log.info("Login realizado: {}", user.getEmail());
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getExpirationMs());
    }

    public TokenResponse refresh(RefreshRequest request) {
        Claims claims = jwtService.validarToken(request.refreshToken());

        if (!jwtService.isRefreshToken(claims)) {
            throw new IllegalArgumentException("Token informado nao e um refresh token");
        }

        String email = claims.getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User nao encontrado"));

        String novoAccessToken = jwtService.gerarAccessToken(user.getEmail(), user.getRole());
        String novoRefreshToken = jwtService.gerarRefreshToken(user.getEmail());

        log.info("Token renovado para: {}", email);
        return new TokenResponse(novoAccessToken, novoRefreshToken, "Bearer", jwtService.getExpirationMs());
    }
}
