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
        log.info("Criando torneio: nome={}", request.getName());

        Tournament tournament = new Tournament();
        tournament.setName(request.getName());
        tournament.setDescription(request.getDescription());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setStatus(TournamentStatus.CREATED);

        if (request.getRuleSet() != null) {
            var ruleSetEntity = new RuleSet();
            ruleSetEntity.setFormat(request.getRuleSet().getFormat());
            ruleSetEntity.setMaxParticipants(request.getRuleSet().getMaxParticipants());
            ruleSetEntity.setBestOf(request.getRuleSet().getBestOf());
            tournament.setRuleSet(ruleSetEntity);
        }

        Tournament savedTournament = tournamentRepository.save(tournament);

        // Métrica de negócio: conta torneios por status
        meterRegistry.counter("tournaments.status.total",
                "status", savedTournament.getStatus().name(),
                "service", "tournament-service"
        ).increment();

        log.info("Torneio criado: id={}, status={}", savedTournament.getId(), savedTournament.getStatus());

        String tournamentFormat = (savedTournament.getRuleSet() != null) ? savedTournament.getRuleSet().getFormat() : "N/A";
        TournamentCreatedEvent event = new TournamentCreatedEvent(
                savedTournament.getId(),
                savedTournament.getName(),
                tournamentFormat
        );
        tournamentProducer.publishTournamentCreated(event);

        return new TournamentResponse(
                savedTournament.getId(),
                savedTournament.getName(),
                savedTournament.getDescription(),
                savedTournament.getStatus(),
                savedTournament.getStartDate(),
                savedTournament.getEndDate()
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

        if (tournament.getStatus() != TournamentStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Torneio nao pode ter inscricoes abertas. Status atual: " + tournament.getStatus());
        }

        tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
        Tournament saved = tournamentRepository.save(tournament);

        return new TournamentResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getStatus(),
                saved.getStartDate(),
                saved.getEndDate()
        );
    }
}



