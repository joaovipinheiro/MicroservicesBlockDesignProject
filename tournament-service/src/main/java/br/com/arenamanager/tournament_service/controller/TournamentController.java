package br.com.arenamanager.tournament_service.controller;

import br.com.arenamanager.tournament_service.Dto.TournamentRequest;
import br.com.arenamanager.tournament_service.Dto.TournamentResponse;
import br.com.arenamanager.tournament_service.domain.model.Tournament;
import br.com.arenamanager.tournament_service.service.TournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@RequestBody TournamentRequest request) {
        TournamentResponse tournamentResponse = tournamentService.createTournament(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tournamentResponse);
    }

    @GetMapping
    public ResponseEntity<List<Tournament>> getAll() {
        List<Tournament> tournaments = tournamentService.getAllTournaments();
        return ResponseEntity.ok(tournaments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getById(@PathVariable Long id) {
        return tournamentService.getTournamentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/abrir-inscricoes")
    public ResponseEntity<TournamentResponse> abrirInscricoes(@PathVariable Long id) {
        TournamentResponse response = tournamentService.abrirInscricoes(id);
        return ResponseEntity.ok(response);
    }
}
