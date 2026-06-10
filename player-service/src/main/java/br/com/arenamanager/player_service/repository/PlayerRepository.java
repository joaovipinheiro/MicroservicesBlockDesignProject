package br.com.arenamanager.player_service.repository;

import br.com.arenamanager.player_service.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}