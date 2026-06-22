package br.com.arenamanager.analytics_service.service;

import br.com.arenamanager.analytics_service.dto.TournamentEventDTO;
import br.com.arenamanager.analytics_service.model.MatchHistory;
import br.com.arenamanager.analytics_service.repository.MatchHistoryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class TournamentEventListener {

    private static final Logger log = LoggerFactory.getLogger(TournamentEventListener.class);
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private final MatchHistoryRepository repository;

    public TournamentEventListener(MatchHistoryRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "tournament-created", groupId = "analytics-group-v2")
    public void consumirEvento(ConsumerRecord<String, TournamentEventDTO> record) {
        TournamentEventDTO evento = record.value();

        Header correlationHeader = record.headers().lastHeader(HEADER_CORRELATION);
        if (correlationHeader != null) {
            MDC.put("correlationId", new String(correlationHeader.value(), StandardCharsets.UTF_8));
        }

        try {
            log.info("Evento TournamentCreated recebido do Kafka: tournamentId={}, nome={}, correlationId={}",
                    evento.tournamentId(), evento.name(), MDC.get("correlationId"));

            MatchHistory history = new MatchHistory();
            history.setTournamentId(evento.tournamentId());
            history.setName(evento.name());
            history.setFormat(evento.format());

            repository.save(history);

            log.info("Torneio salvo no Elasticsearch: tournamentId={}, nome={}", evento.tournamentId(), evento.name());
        } finally {
            MDC.clear();
        }
    }
}
