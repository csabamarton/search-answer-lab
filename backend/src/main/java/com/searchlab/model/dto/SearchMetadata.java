package com.searchlab.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing metadata about search execution.
 * Includes timing, pagination, and search mode information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchMetadata {

    private Long durationMs;
    private Integer totalResults;
    private Integer page;
    private Integer pageSize;
    private String searchMode;
    private Boolean fallbackUsed;
}
