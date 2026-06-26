package br.com.arenamanager.payment_service.service;

import br.com.arenamanager.payment_service.dto.PaymentApprovedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class PaymentPublisherService {

    private static final Logger log = LoggerFactory.getLogger(PaymentPublisherService.class);
    private static final String TOPICO = "payments-approved";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentPublisherService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentApproved(PaymentApprovedEvent event) {
        String correlationId = MDC.get("correlationId");

        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPICO, event);
        if (correlationId != null) {
            record.headers().add(new RecordHeader(HEADER_CORRELATION, correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        kafkaTemplate.send(record);
        log.info("Evento PagamentoAprovado publicado no Kafka: topic={}, pagamentoId={}, correlationId={}",
                TOPICO, event.paymentId(), correlationId);
    }
}
