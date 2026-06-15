package br.com.arenamanager.payment_service.service;

import br.com.arenamanager.payment_service.dto.EventoPagamentoAprovado;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class PagamentoPublisherService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoPublisherService.class);
    private static final String TOPICO = "pagamentos-aprovados";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PagamentoPublisherService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarPagamentoAprovado(EventoPagamentoAprovado evento) {
        String correlationId = MDC.get("correlationId");

        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPICO, evento);
        if (correlationId != null) {
            record.headers().add(new RecordHeader(HEADER_CORRELATION, correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record);
        log.info("Evento PagamentoAprovado publicado no Kafka: topic={}, pagamentoId={}, correlationId={}",
                TOPICO, evento.pagamentoId(), correlationId);
    }
}
