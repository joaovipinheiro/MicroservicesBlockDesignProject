package br.com.arenamanager.tournament_service.producer;

import br.com.arenamanager.tournament_service.Dto.TournamentCreatedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TournamentProducer {

    private static final Logger log = LoggerFactory.getLogger(TournamentProducer.class);
    private static final String TOPIC_NAME         = "tournament-created";
    private static final String CB_NAME            = "kafkaPublisher";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public TournamentProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    @CircuitBreaker(name = CB_NAME, fallbackMethod = "publishFallback")
    public void publishTournamentCreated(TournamentCreatedEvent event) {
        String correlationId = MDC.get("correlationId");

        // Propaga o correlationId no header Kafka para que consumidores possam correlacionar
        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC_NAME, event);
        if (correlationId != null) {
            record.headers().add(new RecordHeader(HEADER_CORRELATION, correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        log.info("Publicando evento Kafka: topic={}, idTorneio={}, correlationId={}", TOPIC_NAME, event.getIdTorneio(), correlationId);

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Erro ao publicar evento Kafka: topic={}, idTorneio={}, correlationId={}, erro={}",
                        TOPIC_NAME, event.getIdTorneio(), correlationId, ex.getMessage());
                meterRegistry.counter("tournaments.kafka.events.total", "topic", TOPIC_NAME, "status", "falha").increment();
                throw new RuntimeException("Falha ao publicar no Kafka", ex);
            } else {
                log.info("Evento Kafka publicado com sucesso: topic={}, idTorneio={}, partition={}, offset={}, correlationId={}",
                        TOPIC_NAME, event.getIdTorneio(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        correlationId);
                meterRegistry.counter("tournaments.kafka.events.total", "topic", TOPIC_NAME, "status", "sucesso").increment();
            }
        });
    }

    private void publishFallback(TournamentCreatedEvent event, Throwable ex) {
        log.warn("Circuit breaker ABERTO — evento Kafka bloqueado: topic={}, idTorneio={}, correlationId={}, motivo={}",
                TOPIC_NAME, event.getIdTorneio(), MDC.get("correlationId"), ex.getMessage());
        meterRegistry.counter("tournaments.kafka.events.total", "topic", TOPIC_NAME, "status", "circuit-breaker").increment();
    }
}
