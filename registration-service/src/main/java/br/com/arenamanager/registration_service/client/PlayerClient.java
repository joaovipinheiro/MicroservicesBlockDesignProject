package br.com.arenamanager.registration_service.client;

import br.com.arenamanager.registration_service.dto.PlayerResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "player-service")
public interface PlayerClient {

    @GetMapping("/api/players/{id}")
    PlayerResponseDTO getById(@PathVariable Long id);
}
