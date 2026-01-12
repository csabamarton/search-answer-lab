package com.searchlab.controller;

import com.searchlab.model.dto.SearchMetadata;
import com.searchlab.model.dto.SearchRequest;
import com.searchlab.model.dto.SearchResponse;
import com.searchlab.model.dto.SearchResult;
import com.searchlab.service.TraditionalSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for search operations.
 * Handles traditional keyword search using PostgreSQL FTS.
 */
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class SearchController {

    private final TraditionalSearchService traditionalSearchService;

    /**
     * Execute search query.
     * 
     * @param request Search request with query, mode, pagination parameters
     * @return SearchResponse with results and metadata
     */
    @PostMapping
    public ResponseEntity<SearchResponse> search(@Valid @RequestBody SearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Get search parameters (with defaults)
            String query = request.getQuery();
            int page = request.getPage() != null ? request.getPage() : 0;
            int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
            
            // Execute traditional search
            TraditionalSearchService.SearchResultPage resultPage = traditionalSearchService.search(query, page, pageSize);
            List<SearchResult> results = resultPage.getResults();
            long totalResults = resultPage.getTotalElements();
            
            // Calculate duration
            long durationMs = System.currentTimeMillis() - startTime;
            
            // Build metadata
            SearchMetadata metadata = SearchMetadata.builder()
                    .durationMs(durationMs)
                    .totalResults((int) totalResults)
                    .page(page)
                    .pageSize(pageSize)
                    .searchMode("traditional")
                    .fallbackUsed(false)
                    .build();
            
            // Build response
            SearchResponse response = new SearchResponse(
                    results,
                    metadata,
                    UUID.randomUUID().toString()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Handle errors - return empty results with error metadata
            long durationMs = System.currentTimeMillis() - startTime;
            
            SearchMetadata metadata = SearchMetadata.builder()
                    .durationMs(durationMs)
                    .totalResults(0)
                    .page(request.getPage() != null ? request.getPage() : 0)
                    .pageSize(request.getPageSize() != null ? request.getPageSize() : 10)
                    .searchMode("traditional")
                    .fallbackUsed(false)
                    .build();
            
            SearchResponse response = new SearchResponse(
                    List.of(),
                    metadata,
                    UUID.randomUUID().toString()
            );
            
            // Log error (in production, use proper logger)
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
