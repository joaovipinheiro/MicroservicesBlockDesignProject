package br.com.arenamanager.tournament_service.repository;

import br.com.arenamanager.tournament_service.domain.model.Tournament;
import br.com.arenamanager.tournament_service.domain.model.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repositório para cuidar de todas as operações de banco para a entidade Tournament
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findByStatus(TournamentStatus status);
}
