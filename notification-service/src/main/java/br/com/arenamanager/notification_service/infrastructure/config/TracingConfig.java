package br.com.arenamanager.notification_service.infrastructure.config;

import brave.sampler.Sampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tracing configuration for the notification service.
 *
 * <h2>B3 Propagation</h2>
 * Spring Boot + {@code micrometer-tracing-bridge-brave} + {@code zipkin-reporter-brave}
 * auto-configure B3 single/multi-format propagation.  No extra beans are required for
 * the propagation itself: Spring Boot wires the {@code Tracer}, {@code Propagator} and
 * Zipkin reporter automatically from {@code application.yaml} properties.
 *
 * <h2>Trace / Span IDs in JSON logs</h2>
 * The {@code logback-spring.xml} uses Logstash's {@code LogstashEncoder}.  Micrometer
 * Tracing (via the Brave bridge) sets MDC keys {@code traceId} and {@code spanId}
 * automatically on every instrumented span.  {@code LogstashEncoder} includes all MDC
 * fields in the JSON output, so the fields appear in every log line without any extra
 * configuration:
 * <pre>
 * {
 *   "timestamp": "...",
 *   "level":     "INFO",
 *   "service":   "notification-service",
 *   "traceId":   "6bdf3e55c4e7f1a0",   ← set by Brave MDC integration
 *   "spanId":    "c4e7f1a06bdf3e55",    ← set by Brave MDC integration
 *   "message":   "Processing payment notification: ..."
 * }
 * </pre>
 *
 * <h2>Kafka header propagation</h2>
 * The {@link br.com.arenamanager.notification_service.infrastructure.kafka.consumer.PaymentApprovedKafkaConsumer}
 * reads the {@code X-B3-TraceId} header from incoming Kafka records and places it in
 * MDC manually before delegating to the use case.  This allows correlation of Kafka
 * consumer logs with the originating HTTP span in the {@code payment-service}.
 *
 * <h2>HTTP header propagation</h2>
 * Spring Boot auto-configures a {@code TracingFilter} that extracts B3 headers
 * ({@code X-B3-TraceId}, {@code X-B3-SpanId}, {@code X-B3-Sampled}) from inbound HTTP
 * requests and creates child spans automatically.  No manual configuration is needed.
 *
 * <h2>Sampling rate</h2>
 * {@code management.tracing.sampling.probability=1.0} in {@code application.yaml}
 * configures 100 % sampling for development.  In production this value should be
 * lowered (e.g. {@code 0.1} for 10 %).  The {@link Sampler} bean below mirrors that
 * setting so Brave uses the same rate even when the auto-configured probability sampler
 * is overridden.
 *
 * <p>Valida: Requisitos 8.1, 8.2, 8.3
 */
@Configuration
public class TracingConfig {

    /**
     * Configures Brave to sample 100 % of traces (suitable for development).
     *
     * <p>Spring Boot auto-configures a {@code ProbabilityBasedSampler} driven by
     * {@code management.tracing.sampling.probability}.  This explicit bean overrides it
     * with {@link Sampler#ALWAYS_SAMPLE} so the Zipkin exporter and log correlation
     * work for every request during development and tests.
     *
     * <p>In production, remove this bean and rely solely on the
     * {@code management.tracing.sampling.probability} property to control sampling.
     */
    @Bean
    public Sampler alwaysSampler() {
        return Sampler.ALWAYS_SAMPLE;
    }
}
