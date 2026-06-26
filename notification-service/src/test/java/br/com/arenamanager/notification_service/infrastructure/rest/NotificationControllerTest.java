package br.com.arenamanager.notification_service.infrastructure.rest;

import br.com.arenamanager.notification_service.infrastructure.mongodb.document.NotificationLogDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.NotificationLogMongoRepository;
import br.com.arenamanager.notification_service.infrastructure.rest.controller.GlobalExceptionHandler;
import br.com.arenamanager.notification_service.infrastructure.rest.controller.NotificationController;
import br.com.arenamanager.notification_service.infrastructure.rest.dto.NotificationLogResponse;
import br.com.arenamanager.notification_service.infrastructure.rest.mapper.NotificationLogMapper;
import com.mongodb.MongoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link NotificationController} using {@code @WebMvcTest}.
 *
 * <p>Valida: Requisitos 7.1, 7.2, 7.3, 7.4, 7.5</p>
 */
@WebMvcTest(controllers = {NotificationController.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    private static final String VALID_PLAYER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationLogMongoRepository repository;

    @MockitoBean
    private NotificationLogMapper mapper;

    // -------------------------------------------------------------------------
    // 1. GET /notifications/{playerId} válido → HTTP 200 com estrutura PagedResponse
    // -------------------------------------------------------------------------

    @Test
    void whenValidPlayerId_thenReturns200WithPagedResponse() throws Exception {
        NotificationLogDocument doc = buildDocument(VALID_PLAYER_ID);
        NotificationLogResponse response = buildResponse(doc);

        when(repository.findByPlayerIdOrderBySentAtDesc(eq(VALID_PLAYER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc), PageRequest.of(0, 20), 1));
        when(mapper.toResponse(doc)).thenReturn(response);

        mockMvc.perform(get("/notifications/{playerId}", VALID_PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    // -------------------------------------------------------------------------
    // 2. playerId inválido (não UUID) → HTTP 400 com mensagem descritiva
    // -------------------------------------------------------------------------

    @Test
    void whenInvalidPlayerId_thenReturns400WithErrorMessage() throws Exception {
        mockMvc.perform(get("/notifications/{playerId}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void whenPlainStringPlayerId_thenReturns400() throws Exception {
        mockMvc.perform(get("/notifications/{playerId}", "abc123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("playerId inválido: deve ser UUID v4")));
    }

    // -------------------------------------------------------------------------
    // 3. size > 100 → HTTP 400
    // -------------------------------------------------------------------------

    @Test
    void whenSizeExceeds100_thenReturns400() throws Exception {
        mockMvc.perform(get("/notifications/{playerId}", VALID_PLAYER_ID)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("size não pode exceder 100")));
    }

    // -------------------------------------------------------------------------
    // 4. MongoDB indisponível (mock lança MongoException) → HTTP 500
    // -------------------------------------------------------------------------

    @Test
    void whenMongoUnavailable_thenReturns500() throws Exception {
        when(repository.findByPlayerIdOrderBySentAtDesc(eq(VALID_PLAYER_ID), any(Pageable.class)))
                .thenThrow(new MongoException("Connection refused"));

        mockMvc.perform(get("/notifications/{playerId}", VALID_PLAYER_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Erro interno ao consultar notificações")));
    }

    // -------------------------------------------------------------------------
    // 5. playerId sem registros → HTTP 200 com lista vazia
    // -------------------------------------------------------------------------

    @Test
    void whenNoRecordsForPlayer_thenReturns200WithEmptyList() throws Exception {
        when(repository.findByPlayerIdOrderBySentAtDesc(eq(VALID_PLAYER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/notifications/{playerId}", VALID_PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty()))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // -------------------------------------------------------------------------
    // 6. Parâmetros padrão page=0, size=20
    // -------------------------------------------------------------------------

    @Test
    void whenNoQueryParams_thenUsesDefaultPageAndSize() throws Exception {
        when(repository.findByPlayerIdOrderBySentAtDesc(eq(VALID_PLAYER_ID), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    // Verify defaults
                    assert pageable.getPageNumber() == 0 : "Expected page=0";
                    assert pageable.getPageSize() == 20 : "Expected size=20";
                    return new PageImpl<>(Collections.emptyList(), pageable, 0);
                });

        mockMvc.perform(get("/notifications/{playerId}", VALID_PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private NotificationLogDocument buildDocument(String playerId) {
        NotificationLogDocument doc = new NotificationLogDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setEventId(UUID.randomUUID().toString());
        doc.setPaymentId(UUID.randomUUID().toString());
        doc.setPlayerId(playerId);
        doc.setPlayerEmail("player@example.com");
        doc.setStatus("SENT");
        doc.setSentAt(Instant.now());
        doc.setTraceId("trace-123");
        return doc;
    }

    private NotificationLogResponse buildResponse(NotificationLogDocument doc) {
        return new NotificationLogResponse(
                doc.getEventId(),
                doc.getPaymentId(),
                doc.getPlayerEmail(),
                doc.getStatus(),
                doc.getErrorMessage(),
                doc.getSentAt() != null ? doc.getSentAt().toString() : null,
                doc.getTraceId()
        );
    }
}
