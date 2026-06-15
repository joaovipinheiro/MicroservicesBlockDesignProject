package br.com.arenamanager.tournament_service.service;

import br.com.arenamanager.tournament_service.Dto.TournamentCreatedEvent;
import br.com.arenamanager.tournament_service.Dto.TournamentRequest;
import br.com.arenamanager.tournament_service.Dto.TournamentResponse;
import br.com.arenamanager.tournament_service.domain.model.RuleSet;
import br.com.arenamanager.tournament_service.domain.model.Tournament;
import br.com.arenamanager.tournament_service.domain.model.TournamentStatus;
import br.com.arenamanager.tournament_service.producer.TournamentProducer;
import br.com.arenamanager.tournament_service.repository.TournamentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class TournamentService {

    private static final Logger log = LoggerFactory.getLogger(TournamentService.class);

    private final TournamentRepository tournamentRepository;
    private final TournamentProducer tournamentProducer;
    private final MeterRegistry meterRegistry;

    public TournamentService(TournamentRepository tournamentRepository,
                             TournamentProducer tournamentProducer,
                             MeterRegistry meterRegistry) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentProducer = tournamentProducer;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public TournamentResponse createTournament(TournamentRequest request) {
        log.info("Criando torneio: nome={}", request.getNome());

        Tournament tournament = new Tournament();
        tournament.setNome(request.getNome());
        tournament.setDescricao(request.getDescricao());
        tournament.setData_inicio(request.getData_inicio());
        tournament.setData_fim(request.getData_fim());
        tournament.setStatus(TournamentStatus.CRIADO);

        if (request.getRegras() != null) {
            var regrasEntity = new RuleSet();
            regrasEntity.setFormato(request.getRegras().getFormato());
            regrasEntity.setMaxParticipantes(request.getRegras().getMaxParticipantes());
            regrasEntity.setMelhorDe(request.getRegras().getMelhorDe());
            tournament.setRegras(regrasEntity);
        }

        Tournament savedTournament = tournamentRepository.save(tournament);

        // Métrica de negócio: conta torneios por status
        meterRegistry.counter("tournaments.status.total",
                "status", savedTournament.getStatus().name(),
                "service", "tournament-service"
        ).increment();

        log.info("Torneio criado: id={}, status={}", savedTournament.getId(), savedTournament.getStatus());

        String formatoTorneio = (savedTournament.getRegras() != null) ? savedTournament.getRegras().getFormato() : "N/A";
        TournamentCreatedEvent event = new TournamentCreatedEvent(
                savedTournament.getId(),
                savedTournament.getNome(),
                formatoTorneio
        );
        tournamentProducer.publishTournamentCreated(event);

        return new TournamentResponse(
                savedTournament.getId(),
                savedTournament.getNome(),
                savedTournament.getDescricao(),
                savedTournament.getStatus(),
                savedTournament.getData_inicio(),
                savedTournament.getData_fim()
        );
    }

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> getTournamentById(Long id) {
        return tournamentRepository.findById(id);
    }

    @Transactional
    public TournamentResponse abrirInscricoes(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Torneio nao encontrado: " + id));

        if (tournament.getStatus() != TournamentStatus.CRIADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Torneio nao pode ter inscricoes abertas. Status atual: " + tournament.getStatus());
        }

        tournament.setStatus(TournamentStatus.REGISTRO_ABERTO);
        Tournament saved = tournamentRepository.save(tournament);

        return new TournamentResponse(
                saved.getId(),
                saved.getNome(),
                saved.getDescricao(),
                saved.getStatus(),
                saved.getData_inicio(),
                saved.getData_fim()
        );
    }
}



