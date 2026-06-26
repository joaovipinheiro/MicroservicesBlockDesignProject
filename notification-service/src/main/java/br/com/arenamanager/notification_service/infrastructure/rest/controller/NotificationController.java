package br.com.arenamanager.notification_service.infrastructure.rest.controller;

import br.com.arenamanager.notification_service.infrastructure.mongodb.document.NotificationLogDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.NotificationLogMongoRepository;
import br.com.arenamanager.notification_service.infrastructure.rest.dto.NotificationLogResponse;
import br.com.arenamanager.notification_service.infrastructure.rest.dto.PagedResponse;
import br.com.arenamanager.notification_service.infrastructure.rest.mapper.NotificationLogMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

/**
 * REST controller exposing notification history endpoints.
 *
 * <p>Valida: Requisitos 7.1, 7.2, 7.3, 7.4, 7.5</p>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    /**
     * UUID v4 regex pattern:
     * {@code xxxxxxxx-xxxx-4xxx-[89ab]xxx-xxxxxxxxxxxx}
     */
    private static final Pattern UUID_V4_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    );

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationLogMongoRepository repository;
    private final NotificationLogMapper mapper;

    public NotificationController(NotificationLogMongoRepository repository,
                                   NotificationLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Returns a paginated list of notification logs for the given player, ordered by
     * {@code sentAt} descending.
     *
     * @param playerId UUID v4 of the player
     * @param page     zero-based page index (default 0)
     * @param size     number of items per page (default 20, max 100)
     * @return paginated {@link PagedResponse} of {@link NotificationLogResponse}
     * @throws IllegalArgumentException if {@code playerId} is not a valid UUID v4 or
     *                                  {@code size} exceeds {@value MAX_PAGE_SIZE}
     */
    @GetMapping("/{playerId}")
    public ResponseEntity<PagedResponse<NotificationLogResponse>> getNotificationsByPlayer(
            @PathVariable String playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        validatePlayerId(playerId);
        validatePageSize(size);

        Page<NotificationLogDocument> resultPage = repository
                .findByPlayerIdOrderBySentAtDesc(playerId, PageRequest.of(page, size));

        List<NotificationLogResponse> content = resultPage.getContent()
                .stream()
                .map(mapper::toResponse)
                .toList();

        PagedResponse<NotificationLogResponse> response = new PagedResponse<>(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Private validation helpers
    // -------------------------------------------------------------------------

    private void validatePlayerId(String playerId) {
        if (playerId == null || !UUID_V4_PATTERN.matcher(playerId.toLowerCase()).matches()) {
            throw new IllegalArgumentException("playerId inválido: deve ser UUID v4");
        }
    }

    private void validatePageSize(int size) {
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size não pode exceder 100");
        }
    }
}
