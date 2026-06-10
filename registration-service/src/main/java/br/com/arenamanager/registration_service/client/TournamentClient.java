package br.com.arenamanager.registration_service.client;

import br.com.arenamanager.registration_service.dto.TournamentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "tournament-service")
public interface TournamentClient {

    @GetMapping("/api/tournaments/{id}")
    TournamentResponseDTO getById(@PathVariable Long id);
}