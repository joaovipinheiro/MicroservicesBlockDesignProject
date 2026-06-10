package br.com.arenamanager.analytics_service.service;

import br.com.arenamanager.analytics_service.dto.TournamentEventDTO;
import br.com.arenamanager.analytics_service.model.MatchHistory;
import br.com.arenamanager.analytics_service.repository.MatchHistoryRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TournamentEventListener {

    private final MatchHistoryRepository repository;

    public TournamentEventListener(MatchHistoryRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "tournament-created", groupId = "analytics-group-v2")
    public void consumirEvento(TournamentEventDTO evento) {

        MatchHistory history = new MatchHistory();
        history.setIdTorneio(evento.id());
        history.setNome(evento.nome());
        history.setFormato(evento.formato());

        repository.save(history); // Salva no Elasticsearch!

        System.out.println("Torneio processado no Analytics! ID: " + evento.id());
    }
}