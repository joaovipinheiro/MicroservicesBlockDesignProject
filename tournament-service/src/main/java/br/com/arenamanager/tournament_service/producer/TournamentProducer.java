package br.com.arenamanager.tournament_service.producer;

import br.com.arenamanager.tournament_service.Dto.TournamentCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TournamentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String TOPIC_NAME = "tournament-created";

    public TournamentProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTournamentCreated(TournamentCreatedEvent event) {
        // Envia o evento para o tópico do Kafka
        kafkaTemplate.send(TOPIC_NAME, event);
    }
}
