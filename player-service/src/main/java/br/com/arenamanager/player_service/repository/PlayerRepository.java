package br.com.arenamanager.player_service.repository;

import br.com.arenamanager.player_service.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}