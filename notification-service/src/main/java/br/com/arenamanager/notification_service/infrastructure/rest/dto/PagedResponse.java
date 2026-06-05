package br.com.arenamanager.notification_service.infrastructure.rest.dto;

import java.util.List;

/**
 * Generic paginated response wrapper for REST endpoints.
 *
 * <p>Valida: Requisito 7.2</p>
 *
 * @param <T>           the type of items in the page
 * @param content       the items on the current page
 * @param page          zero-based current page index
 * @param size          requested page size
 * @param totalElements total number of elements across all pages
 * @param totalPages    total number of pages available
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
