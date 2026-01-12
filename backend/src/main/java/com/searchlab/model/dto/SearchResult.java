package com.searchlab.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single search result.
 * Contains document information and relevance score.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {

    private Long id;
    private String title;
    private String content;
    private String source;
    private Double score;
    private String snippet;
}
