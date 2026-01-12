package com.searchlab.semantic;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SemanticSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public SemanticSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Searches document chunks by vector similarity using cosine distance.
     * 
     * @param queryVectorLiteral pgvector literal string like '[0.1,0.2,...]'
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @return List of semantic hits with document ID, chunk text, and distance
     */
    public List<SemanticHit> searchByVector(String queryVectorLiteral, int limit, int offset) {
        String sql = """
            SELECT c.document_id, c.chunk_text,
                   (c.embedding <=> (?::vector)) AS distance
            FROM document_chunks c
            WHERE c.embedding IS NOT NULL
            ORDER BY c.embedding <=> (?::vector)
            LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SemanticHit(
                        rs.getLong("document_id"),
                        rs.getString("chunk_text"),
                        rs.getDouble("distance")
                ),
                queryVectorLiteral, queryVectorLiteral, limit, offset
        );
    }

    public record SemanticHit(long documentId, String chunkText, double distance) {}
}
