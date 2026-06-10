package br.com.arenamanager.player_service.controller;

import br.com.arenamanager.player_service.model.Player;
import br.com.arenamanager.player_service.repository.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerRepository repository;

    public PlayerController(PlayerRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Player> create(@RequestBody Player player) {
        Player savedPlayer = repository.save(player);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPlayer);
    }

    @GetMapping
    public ResponseEntity<List<Player>> findAll() {
        return ResponseEntity.ok(repository.findAll());
    }
}