package br.com.arenamanager.payment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "player-service")
public interface PlayerClient {

    @GetMapping("/api/players/{id}")
    PlayerDTO obterJogadorPorId(@PathVariable("id") Long id);

}