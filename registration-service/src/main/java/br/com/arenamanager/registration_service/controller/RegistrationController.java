package br.com.arenamanager.registration_service.controller;

import br.com.arenamanager.registration_service.dto.RegistrationRequest;
import br.com.arenamanager.registration_service.dto.RegistrationResponse;
import br.com.arenamanager.registration_service.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<RegistrationResponse> create(@RequestBody @Valid RegistrationRequest request) {
        RegistrationResponse response = registrationService.createRegistration(request);
        HttpStatus status = switch (response.getStatus()) {
            case CONFIRMADO -> HttpStatus.CREATED;
            default -> HttpStatus.ACCEPTED;
        };
        return ResponseEntity.status(status).body(response);
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

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<RegistrationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.cancelRegistration(id));
    }
}
