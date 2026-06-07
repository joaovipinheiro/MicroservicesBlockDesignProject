package br.com.arenamanager.analytics_service.controller;
import br.com.arenamanager.analytics_service.dto.MatchHistoryRequest;
import br.com.arenamanager.analytics_service.model.MatchHistory;
import br.com.arenamanager.analytics_service.service.AnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/match")
    public ResponseEntity<MatchHistory> salvarResultado(@RequestBody MatchHistoryRequest request) {
        MatchHistory resultadoSalvo = analyticsService.processarESalvarPartida(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultadoSalvo);
    }

    @GetMapping("/player/{nickname}")
    public ResponseEntity<List<MatchHistory>> buscarHistoricoPorNickname(@PathVariable String nickname) {
        List<MatchHistory> historico = analyticsService.obterHistoricoPorJogador(nickname);
        return ResponseEntity.ok(historico);
    }
}