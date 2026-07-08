package com.creator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Provides pagination metadata alongside the result data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedResponse<T> {

    private List<T> data;
    private int page;
    private int limit;
    private long totalElements;
    private int totalPages;
}
