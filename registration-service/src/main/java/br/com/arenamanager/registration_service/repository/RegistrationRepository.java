package br.com.arenamanager.registration_service.repository;

import br.com.arenamanager.registration_service.domain.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findByPlayerId(Long playerId);
    List<Registration> findByTournamentId(Long tournamentId);
    boolean existsByPlayerIdAndTournamentIdAndStatusNot(Long playerId, Long tournamentId, br.com.arenamanager.registration_service.domain.model.RegistrationStatus status);
}
