package br.com.arenamanager.registration_service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Interceptor Feign que propaga o X-Correlation-ID do MDC para todas as
 * chamadas HTTP saintes (payment-service, player-service, tournament-service).
 * Isso permite rastrear uma operação pelo correlationId em todos os serviços.
 */
@Component
public class FeignCorrelationInterceptor implements RequestInterceptor {

    private static final String HEADER = "X-Correlation-ID";

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            template.header(HEADER, correlationId);
        }
    }
}
