package br.com.arenamanager.tournament_service.service;

import br.com.arenamanager.tournament_service.Dto.TournamentCreatedEvent;
import br.com.arenamanager.tournament_service.Dto.TournamentRequest;
import br.com.arenamanager.tournament_service.Dto.TournamentResponse;
import br.com.arenamanager.tournament_service.domain.model.RuleSet;
import br.com.arenamanager.tournament_service.domain.model.Tournament;
import br.com.arenamanager.tournament_service.domain.model.TournamentStatus;
import br.com.arenamanager.tournament_service.producer.TournamentProducer;
import br.com.arenamanager.tournament_service.repository.TournamentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final TournamentProducer tournamentProducer;

    public TournamentService(TournamentRepository tournamentRepository, TournamentProducer tournamentProducer) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentProducer = tournamentProducer;
    }

    @Transactional
    public TournamentResponse createTournament (TournamentRequest request){
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
}



