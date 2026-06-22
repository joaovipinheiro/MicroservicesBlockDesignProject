package br.com.arenamanager.notification_service.infrastructure.kafka.consumer;

import br.com.arenamanager.notification_service.application.port.in.ProcessPaymentNotificationUseCase;
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PaymentApprovedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Kafka consumer for the {@code pagamentos.aprovados} topic.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Propagate {@code traceId} from Kafka header {@code X-B3-TraceId} to MDC</li>
 *   <li>Deserialize JSON payload to {@link PaymentApprovedEvent}</li>
 *   <li>On {@link JsonProcessingException}: log ERROR, send to DLQ immediately (no retry)</li>
 *   <li>On success or duplicate: call {@code ack.acknowledge()}</li>
 *   <li>On processing exception: re-throw so {@link org.springframework.kafka.listener.DefaultErrorHandler}
 *       handles retry and DLQ routing</li>
 * </ul>
 *
 * <p>Valida: Requisitos 1.1, 1.2, 1.3, 1.4, 6.1, 6.2</p>
 */
@Component
public class PaymentApprovedKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentApprovedKafkaConsumer.class);

    private static final String MDC_TRACE_ID_KEY      = "traceId";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";
    private static final String HEADER_TRACE_ID        = "X-B3-TraceId";
    private static final String HEADER_CORRELATION_ID  = "X-Correlation-ID";
    private static final String DLQ_COUNTER      = "notifications.dlq.total";

    private final ProcessPaymentNotificationUseCase notificationUseCase;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final String dlqTopic;

    public PaymentApprovedKafkaConsumer(
            ProcessPaymentNotificationUseCase notificationUseCase,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            MeterRegistry meterRegistry,
            @Value("${notification.kafka.topic.dlq:payments-approved-dlq}") String dlqTopic) {
        this.notificationUseCase = notificationUseCase;
        this.objectMapper        = objectMapper;
        this.kafkaTemplate       = kafkaTemplate;
        this.meterRegistry       = meterRegistry;
        this.dlqTopic            = dlqTopic;
    }

    /**
     * Consumes a message from the {@code pagamentos.aprovados} topic.
     *
     * @param record Kafka record with String key and JSON String value
     * @param ack    manual acknowledgment handle
     */
    @KafkaListener(
            topics = "${notification.kafka.topic.payments-approved}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        propagateTraceId(record);
        propagateCorrelationId(record);
        String traceId = MDC.get(MDC_TRACE_ID_KEY);
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);

        try {
            PaymentApprovedEvent event = deserialize(record.value());

            log.info("Evento PagamentoAprovado recebido do Kafka: paymentId={}, playerEmail={}, correlationId={}, traceId={}",
                    event.paymentId(), event.playerEmail(), correlationId, traceId);

            notificationUseCase.processPaymentApprovedNotification(event);
            ack.acknowledge();

        } catch (JsonProcessingException ex) {
            // Malformed JSON → DLQ immediately, no retry
            log.error("Failed to deserialize Kafka message: topic={}, partition={}, offset={}, error={}",
                    record.topic(), record.partition(), record.offset(), ex.getMessage());
            sendToDlq(record, traceId);
            meterRegistry.counter(DLQ_COUNTER).increment();
            ack.acknowledge();

        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
            MDC.remove(MDC_CORRELATION_ID_KEY);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void propagateTraceId(ConsumerRecord<String, String> record) {
        Header traceIdHeader = record.headers().lastHeader(HEADER_TRACE_ID);
        if (traceIdHeader != null) {
            MDC.put(MDC_TRACE_ID_KEY, new String(traceIdHeader.value(), StandardCharsets.UTF_8));
        }
    }

    private void propagateCorrelationId(ConsumerRecord<String, String> record) {
        Header correlationHeader = record.headers().lastHeader(HEADER_CORRELATION_ID);
        if (correlationHeader != null) {
            MDC.put(MDC_CORRELATION_ID_KEY, new String(correlationHeader.value(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Deserializes the JSON payload to a {@link PaymentApprovedEvent}.
     *
     * @throws JsonProcessingException if the payload is not valid JSON or missing required fields
     */
    private PaymentApprovedEvent deserialize(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, PaymentApprovedEvent.class);
    }

    /**
     * Sends the original record to the DLQ topic preserving original headers.
     */
    private void sendToDlq(ConsumerRecord<String, String> record, String traceId) {
        log.error("Forwarding message to DLQ: topic={}, dlq={}, traceId={}",
                record.topic(), dlqTopic, traceId);
        kafkaTemplate.send(dlqTopic, record.key(), record.value());
    }
}
