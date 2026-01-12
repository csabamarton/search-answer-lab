package com.searchlab.semantic;

import com.searchlab.embeddings.EmbeddingClient;
import com.searchlab.model.dto.SearchResult;
import com.searchlab.model.entity.Document;
import com.searchlab.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.searchlab.semantic.VectorSql.toVectorLiteral;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingClient embeddingClient;
    private final SemanticSearchRepository semanticRepo;
    private final DocumentRepository documentRepo;

    /**
     * Performs semantic search: embed query -> vector search -> return document results.
     */
    public List<SearchResult> search(String query, int page, int pageSize) {
        // Embed the query
        List<List<Double>> embeddings = embeddingClient.embed(List.of(query));
        List<Double> queryVector = embeddings.get(0);
        String queryVectorLiteral = toVectorLiteral(queryVector);

        // Perform vector search
        int offset = page * pageSize;
        List<SemanticSearchRepository.SemanticHit> hits = semanticRepo.searchByVector(
                queryVectorLiteral, pageSize, offset
        );

        // Get unique document IDs and fetch documents
        Set<Long> documentIds = hits.stream()
                .map(SemanticSearchRepository.SemanticHit::documentId)
                .collect(Collectors.toSet());

        Map<Long, Document> documents = documentRepo.findAllById(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, doc -> doc));

        // Map hits to SearchResult DTOs
        return hits.stream()
                .map(hit -> {
                    Document doc = documents.get(hit.documentId());
                    if (doc == null) {
                        return null;
                    }
                    
                    // Convert distance to score (lower distance = higher score)
                    // Cosine distance: 0 = identical, 1 = orthogonal, 2 = opposite
                    double score = 1.0 - hit.distance(); // Invert distance for score
                    
                    return SearchResult.builder()
                            .id(doc.getId())
                            .title(doc.getTitle())
                            .content(doc.getContent())
                            .source(doc.getSource())
                            .score(score)
                            .snippet(hit.chunkText()) // Use the matching chunk as snippet
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Counts total results for semantic search (approximate - gets all hits then counts).
     * For better performance, could use a separate count query.
     */
    public long countResults(String query) {
        List<List<Double>> embeddings = embeddingClient.embed(List.of(query));
        List<Double> queryVector = embeddings.get(0);
        String queryVectorLiteral = toVectorLiteral(queryVector);

        // Get a large sample to count (not perfect, but works for now)
        List<SemanticSearchRepository.SemanticHit> hits = semanticRepo.searchByVector(
                queryVectorLiteral, 10000, 0
        );
        
        return hits.size();
    }
}
