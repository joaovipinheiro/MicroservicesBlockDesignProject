package br.com.arenamanager.player_service.controller;

import br.com.arenamanager.player_service.dto.PlayerRequestDTO;
import br.com.arenamanager.player_service.dto.PlayerResponseDTO;
import br.com.arenamanager.player_service.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService service;

    @PostMapping
    public ResponseEntity<PlayerResponseDTO> create(@RequestBody @Valid PlayerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPlayer(request));
    }

    @GetMapping
    public ResponseEntity<List<PlayerResponseDTO>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponseDTO> update(@PathVariable Long id, @RequestBody @Valid PlayerRequestDTO request) {
        return ResponseEntity.ok(service.updatePlayer(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}