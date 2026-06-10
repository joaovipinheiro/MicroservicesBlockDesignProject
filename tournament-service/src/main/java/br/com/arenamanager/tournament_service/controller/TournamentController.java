package br.com.arenamanager.tournament_service.controller;

import br.com.arenamanager.tournament_service.Dto.TournamentRequest;
import br.com.arenamanager.tournament_service.Dto.TournamentResponse;
import br.com.arenamanager.tournament_service.domain.model.Tournament;
import br.com.arenamanager.tournament_service.service.TournamentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tournament")
public class TournamentController {

    private static final Logger log = LoggerFactory.getLogger(TournamentController.class);
    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> create(
            @RequestBody TournamentRequest request,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        setupMdc(correlationId);
        try {
            log.info("Requisição recebida: POST /tournament, caller=api-gateway/registration-service, nome={}", request.getNome());
            TournamentResponse response = tournamentService.createTournament(request);
            log.info("Resposta enviada: POST /tournament, status=201, idTorneio={}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    public ResponseEntity<List<Tournament>> getAll(
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        setupMdc(correlationId);
        try {
            log.info("Requisição recebida: GET /tournament, caller=api-gateway/registration-service");
            List<Tournament> tournaments = tournamentService.getAllTournaments();
            log.info("Resposta enviada: GET /tournament, status=200, total={}", tournaments.size());
            return ResponseEntity.ok(tournaments);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getById(
            @PathVariable Long id,
            @RequestHeader(value = HEADER_CORRELATION_ID, required = false) String correlationId) {
        setupMdc(correlationId);
        try {
            log.info("Requisição recebida: GET /tournament/{}, caller=api-gateway/registration-service", id);
            return tournamentService.getTournamentById(id)
                    .map(t -> {
                        log.info("Resposta enviada: GET /tournament/{}, status=200", id);
                        return ResponseEntity.ok(t);
                    })
                    .orElseGet(() -> {
                        log.warn("Torneio não encontrado: id={}", id);
                        return ResponseEntity.notFound().build();
                    });
        } finally {
            MDC.clear();
        }
    }

    private void setupMdc(String correlationId) {
        // Se o caller não enviou correlationId, gera um novo para rastrear internamente
        MDC.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
    }
}
