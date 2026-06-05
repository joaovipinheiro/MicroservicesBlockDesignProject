package br.com.arenamanager.notification_service.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for the notification service.
 *
 * <p>Pre-registers the counters and timers used across the notification flow so that they
 * appear in the Prometheus/Actuator metrics endpoint even before the first event is
 * processed.  The actual {@code .increment()} / {@code .record()} calls happen in
 * {@link br.com.arenamanager.notification_service.application.service.NotificationService}
 * and
 * {@link br.com.arenamanager.notification_service.infrastructure.kafka.consumer.PagamentoAprovadoKafkaConsumer}.
 *
 * <p>Registered metrics:
 * <ul>
 *   <li>{@code notifications.sent.total} — counter, tag {@code service=notification-service}</li>
 *   <li>{@code notifications.failed.total} — counter, tag {@code service=notification-service}</li>
 *   <li>{@code notifications.dlq.total} — counter, tag {@code service=notification-service}</li>
 *   <li>{@code notifications.processing.duration} — timer, tag {@code service=notification-service}</li>
 * </ul>
 *
 * <p>Valida: Requisito 8.4
 */
@Configuration
public class MetricsConfig {

    private static final String TAG_KEY   = "service";
    private static final String TAG_VALUE = "notification-service";

    /**
     * Registers the notification counters and timer into the {@link MeterRegistry}.
     *
     * <p>Using a {@link MeterBinder} bean ensures the meters are registered after the
     * registry is fully initialised (post-construction, before any traffic).
     */
    @Bean
    public MeterBinder notificationMetricsBinder() {
        return registry -> {
            // Counters
            registry.counter("notifications.sent.total",    TAG_KEY, TAG_VALUE);
            registry.counter("notifications.failed.total",  TAG_KEY, TAG_VALUE);
            registry.counter("notifications.dlq.total",     TAG_KEY, TAG_VALUE);
            registry.counter("notifications.duplicate.total", TAG_KEY, TAG_VALUE);

            // Timer
            registry.timer("notifications.processing.duration", TAG_KEY, TAG_VALUE);
        };
    }
}
