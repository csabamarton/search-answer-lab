package com.searchlab.semantic;

import com.searchlab.embeddings.EmbeddingClient;
import com.searchlab.model.entity.Document;
import com.searchlab.repository.DocumentRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.searchlab.semantic.VectorSql.toVectorLiteral;

@Service
public class SemanticIndexerService {

    private final DocumentRepository documentRepository;
    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate jdbcTemplate;

    public SemanticIndexerService(DocumentRepository documentRepository,
                                 EmbeddingClient embeddingClient,
                                 JdbcTemplate jdbcTemplate) {
        this.documentRepository = documentRepository;
        this.embeddingClient = embeddingClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void reindexAll(int maxChunkChars) {
        jdbcTemplate.update("DELETE FROM document_chunks");

        List<Document> docs = documentRepository.findAll();
        for (Document doc : docs) {
            reindexDocument(doc.getId(), maxChunkChars);
        }
    }

    @Transactional
    public void reindexDocument(long documentId, int maxChunkChars) {
        jdbcTemplate.update("DELETE FROM document_chunks WHERE document_id = ?", documentId);

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        List<String> chunks = Chunker.chunkByParagraphs(doc.getContent(), maxChunkChars);

        if (chunks.isEmpty()) {
            return;
        }

        // Embed all chunks in one batch call
        List<List<Double>> vectors = embeddingClient.embed(chunks);

        String insert = """
          INSERT INTO document_chunks(document_id, chunk_index, chunk_text, embedding)
          VALUES (?, ?, ?, ?::vector)
        """;

        for (int i = 0; i < chunks.size(); i++) {
            String vecLit = toVectorLiteral(vectors.get(i));
            jdbcTemplate.update(insert, documentId, i, chunks.get(i), vecLit);
        }
    }
}
