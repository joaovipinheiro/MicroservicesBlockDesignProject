package br.com.arenamanager.tournament_service.repository;

import br.com.arenamanager.tournament_service.domain.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repositório para cuidar de todas as operações do banco para a entidade Match
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentId(Long tournamentId);
}
