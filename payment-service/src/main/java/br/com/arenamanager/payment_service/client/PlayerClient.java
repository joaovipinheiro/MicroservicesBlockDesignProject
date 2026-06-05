package br.com.arenamanager.payment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "player-service", url = "http://localhost:8082")
public interface PlayerClient {

    @GetMapping("/players/{id}")
    PlayerDTO obterJogadorPorId(@PathVariable("id") Long id);
}

record PlayerDTO(Long id, String nome, String email) {}