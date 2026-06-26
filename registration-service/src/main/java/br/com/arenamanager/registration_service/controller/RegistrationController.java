package br.com.arenamanager.registration_service.controller;

import br.com.arenamanager.registration_service.dto.RegistrationRequest;
import br.com.arenamanager.registration_service.dto.RegistrationResponse;
import br.com.arenamanager.registration_service.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<RegistrationResponse> create(
            @RequestBody @Valid RegistrationRequest request,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        String cid = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        try {
            log.info("Requisição recebida: POST /api/registrations, playerId={}, tournamentId={}, correlationId={}",
                    request.getPlayerId(), request.getTournamentId(), cid);
            RegistrationResponse response = registrationService.createRegistration(request, cid);
            HttpStatus status = switch (response.getStatus()) {
                case CONFIRMED -> HttpStatus.CREATED;
                default -> HttpStatus.ACCEPTED;
            };
            log.info("Resposta enviada: POST /api/registrations, status={}, registrationId={}, correlationId={}",
                    status.value(), response.getId(), cid);
            return ResponseEntity.status(status).body(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    public ResponseEntity<List<RegistrationResponse>> getAll() {
        return ResponseEntity.ok(registrationService.getAllRegistrations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegistrationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.getById(id));
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<RegistrationResponse>> getByPlayer(@PathVariable Long playerId) {
        return ResponseEntity.ok(registrationService.getByPlayer(playerId));
    }

    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<List<RegistrationResponse>> getByTournament(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(registrationService.getByTournament(tournamentId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RegistrationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.cancelRegistration(id));
    }
}
