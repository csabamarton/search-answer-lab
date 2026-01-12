package com.searchlab.service;

import com.searchlab.model.dto.SearchResult;
import com.searchlab.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for traditional keyword-based search using PostgreSQL full-text search.
 * Implements search functionality using tsvector and websearch_to_tsquery.
 */
@Service
@RequiredArgsConstructor
public class TraditionalSearchService {

    private final DocumentRepository documentRepository;

    /**
     * Performs full-text search and returns paginated results with total count.
     * 
     * @param query The search query string (user input, will be processed by websearch_to_tsquery)
     * @param page Page number (0-indexed)
     * @param pageSize Number of results per page
     * @return SearchResultPage containing results list and total count
     */
    public SearchResultPage search(String query, int page, int pageSize) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchResultPage(List.of(), 0);
        }
        
        // Create pagination
        Pageable pageable = PageRequest.of(page, pageSize);
        
        // Execute search - websearch_to_tsquery handles user input naturally
        Page<Object[]> resultPage = documentRepository.searchByFullText(query.trim(), pageable);
        
        // Convert Object[] arrays to SearchResult DTOs
        List<SearchResult> results = resultPage.getContent().stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());
        
        return new SearchResultPage(results, resultPage.getTotalElements());
    }

    /**
     * Maps Object[] array from query result to SearchResult DTO.
     * Array format: [id, title, content, source, score, snippet]
     * 
     * @param row Object array from query result
     * @return SearchResult DTO
     */
    private SearchResult mapToSearchResult(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        String title = (String) row[1];
        String content = (String) row[2];
        String source = (String) row[3];
        Double score = row[4] != null ? ((Number) row[4]).doubleValue() : null;
        String snippet = row[5] != null ? (String) row[5] : "";
        
        // Clean up snippet (remove HTML tags that ts_headline might add)
        if (snippet != null) {
            snippet = snippet.replaceAll("<[^>]+>", "");
        }
        
        return SearchResult.builder()
                .id(id)
                .title(title)
                .content(content)
                .source(source)
                .score(score)
                .snippet(snippet != null && !snippet.isEmpty() ? snippet : content)
                .build();
    }

    /**
     * Inner class to hold search results and total count.
     */
    public static class SearchResultPage {
        private final List<SearchResult> results;
        private final long totalElements;

        public SearchResultPage(List<SearchResult> results, long totalElements) {
            this.results = results;
            this.totalElements = totalElements;
        }

        public List<SearchResult> getResults() {
            return results;
        }

        public long getTotalElements() {
            return totalElements;
        }
    }
}
