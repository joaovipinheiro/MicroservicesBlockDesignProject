package br.com.arenamanager.analytics_service.service;

import br.com.arenamanager.analytics_service.dto.MatchHistoryRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TournamentEventListener {

    private final AnalyticsService analyticsService;

    public TournamentEventListener(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @KafkaListener(topics = "tournament-created", groupId = "analytics-group")
    public void consumirEventoTorneio(MatchHistoryRequest dadosDoTorneio) {
        try {
            System.out.println("Evento de torneio recebido via Kafka: " + dadosDoTorneio.getNome());
            analyticsService.processarESalvarPartida(dadosDoTorneio);
        } catch (Exception e) {
            System.err.println("Erro ao processar evento do Kafka: " + e.getMessage());
        }
    }
}