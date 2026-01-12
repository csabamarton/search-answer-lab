package com.searchlab.repository;

import com.searchlab.model.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Document entity.
 * Provides CRUD operations and custom queries for full-text search.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Full-text search using PostgreSQL tsvector and ts_rank.
     * Uses native query to leverage PostgreSQL's full-text search capabilities.
     * Returns Object arrays with: id, title, content, source, score, snippet
     * 
     * @param queryText The search query text (will be converted to tsquery)
     * @param pageable Pagination parameters
     * @return Page of Object arrays sorted by relevance score (ts_rank)
     */
    @Query(value = """
        SELECT 
            d.id,
            d.title,
            d.content,
            d.source,
            ts_rank(d.search_vector, websearch_to_tsquery('english', :queryText)) AS score,
            ts_headline('english', d.content, websearch_to_tsquery('english', :queryText), 'MaxWords=30, MinWords=10, ShortWord=3') AS snippet
        FROM documents d
        WHERE d.search_vector @@ websearch_to_tsquery('english', :queryText)
        ORDER BY score DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM documents d
        WHERE d.search_vector @@ websearch_to_tsquery('english', :queryText)
        """,
        nativeQuery = true)
    Page<Object[]> searchByFullText(@Param("queryText") String queryText, Pageable pageable);
}
