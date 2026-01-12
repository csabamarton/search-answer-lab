# Semantic Search Implementation Plan - Option A

Detailed step-by-step guide for adding semantic search with Python embeddings service.

## Overview

Add semantic search capability to the existing search-answer-lab project:
- Python FastAPI service for generating embeddings
- pgvector extension in PostgreSQL
- New backend services for semantic search
- Integration with existing search endpoint

## Folder Structure (After Implementation)

```
search-answer-lab/
├── backend/                    (existing - Spring Boot)
│   └── src/main/java/com/searchlab/
│       ├── embeddings/         (NEW)
│       │   └── EmbeddingClient.java
│       └── semantic/           (NEW)
│           ├── VectorSql.java
│           ├── Chunker.java
│           ├── SemanticSearchRepository.java
│           ├── SemanticSearchService.java
│           ├── SemanticIndexerService.java
│           └── SemanticAdminController.java
├── frontend/                   (existing - React)
├── embeddings-service/         (NEW - Python FastAPI)
│   ├── main.py
│   ├── requirements.txt
│   └── README.md
└── docker-compose.yml          (update - use pgvector image)
```

---

## Step-by-Step Implementation

### Step 1: Update Docker Compose

**File:** `docker-compose.yml`

Change the PostgreSQL image to one with pgvector support:

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg15    # Changed from postgres:15-alpine
    container_name: searchlab-postgres
    environment:
      POSTGRES_DB: searchlab
      POSTGRES_USER: searchlab
      POSTGRES_PASSWORD: searchlab
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U searchlab"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
    driver: local
```

**Action:** Update the image line, restart container: `docker compose down && docker compose up -d`

---

### Step 2: Add Flyway Migrations

**File:** `backend/src/main/resources/db/migration/V4__enable_pgvector.sql`

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

**File:** `backend/src/main/resources/db/migration/V5__create_document_chunks.sql`

```sql
CREATE TABLE IF NOT EXISTS document_chunks (
  id BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
  chunk_index INT NOT NULL,
  chunk_text TEXT NOT NULL,
  embedding vector(384),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chunks_doc ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_chunks_document_id_chunk_index ON document_chunks(document_id, chunk_index);
```

**Note:** We use vector(384) which matches the all-MiniLM-L6-v2 model (384 dimensions).

---

### Step 3: Create Python Embeddings Service

**File:** `embeddings-service/requirements.txt`

```
fastapi==0.110.0
uvicorn[standard]==0.27.1
sentence-transformers==2.6.1
torch>=2.0.0
```

**File:** `embeddings-service/main.py`

```python
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from typing import List

app = FastAPI()

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
model = SentenceTransformer(MODEL_NAME)  # outputs 384-dim vectors

class EmbedRequest(BaseModel):
    texts: List[str]

class EmbedResponse(BaseModel):
    model: str
    dim: int
    vectors: List[List[float]]

@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_NAME}

@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    vectors = model.encode(req.texts, normalize_embeddings=True).tolist()
    return {
        "model": MODEL_NAME,
        "dim": len(vectors[0]) if vectors else 0,
        "vectors": vectors
    }
```

**File:** `embeddings-service/README.md`

```
# Embeddings Service

FastAPI service for generating text embeddings.

## Setup

```bash
python -m venv .venv
.venv\Scripts\activate  # Windows
pip install -r requirements.txt
```

## Run

```bash
uvicorn main:app --port 8090 --reload
```

## Test

```bash
curl -X POST http://localhost:8090/embed -H "Content-Type: application/json" -d "{\"texts\":[\"hello world\",\"database indexing\"]}"
```
```

---

### Step 4: Update Backend Dependencies

**File:** `backend/pom.xml`

Add WebClient dependency (in the dependencies section, after spring-boot-starter-web):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

---

### Step 5: Update Backend Configuration

**File:** `backend/src/main/resources/application.yml`

Add embeddings configuration in the dev profile section (after datasource):

```yaml
  embeddings:
    base-url: http://localhost:8090
    timeout-seconds: 5
```

---

### Step 6: Create Backend Code Files

All files go in `backend/src/main/java/com/searchlab/`

#### Package: com.searchlab.embeddings

**File:** `embeddings/EmbeddingClient.java`

```java
package com.searchlab.embeddings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Component
public class EmbeddingClient {

    private final WebClient webClient;
    private final Duration timeout;

    public EmbeddingClient(@Value("${embeddings.base-url}") String baseUrl,
                          @Value("${embeddings.timeout-seconds:5}") int timeoutSeconds) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public List<List<Double>> embed(List<String> texts) {
        try {
            EmbedRequest req = new EmbedRequest(texts);
            
            EmbedResponse response = webClient.post()
                    .uri("/embed")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(EmbedResponse.class)
                    .timeout(timeout)
                    .block();
            
            return response.vectors();
        } catch (WebClientResponseException | java.util.concurrent.TimeoutException e) {
            throw new EmbeddingServiceException("Failed to get embeddings: " + e.getMessage(), e);
        }
    }

    public record EmbedRequest(List<String> texts) {}
    public record EmbedResponse(String model, int dim, List<List<Double>> vectors) {}
    
    public static class EmbeddingServiceException extends RuntimeException {
        public EmbeddingServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

#### Package: com.searchlab.semantic

**File:** `semantic/VectorSql.java`

```java
package com.searchlab.semantic;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class VectorSql {
    private VectorSql() {}

    /**
     * Converts a list of doubles to pgvector literal format: '[0.1,0.2,...]'
     */
    public static String toVectorLiteral(List<Double> vector) {
        String body = vector.stream()
                .map(d -> String.format(Locale.US, "%.8f", d))
                .collect(Collectors.joining(","));
        return "[" + body + "]";
    }
}
```

**File:** `semantic/Chunker.java`

```java
package com.searchlab.semantic;

import java.util.ArrayList;
import java.util.List;

public final class Chunker {
    private Chunker() {}

    /**
     * Chunks text by paragraphs, respecting max character limit.
     * Simple approach: split by blank lines, combine until maxChars.
     */
    public static List<String> chunkByParagraphs(String text, int maxChars) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        
        String[] parts = text.split("\\R\\R+"); // Split by blank lines
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) continue;
            
            if (current.length() + part.length() + 2 > maxChars && current.length() > 0) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(part).append("\n\n");
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        
        return chunks.isEmpty() ? List.of(text) : chunks;
    }
}
```

**File:** `semantic/SemanticSearchRepository.java`

```java
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
```

**File:** `semantic/SemanticIndexerService.java`

```java
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
```

**File:** `semantic/SemanticSearchService.java`

```java
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
```

**File:** `semantic/SemanticAdminController.java`

```java
package com.searchlab.semantic;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile("dev")
@RestController
@RequestMapping("/api/admin")
public class SemanticAdminController {

    private final SemanticIndexerService indexer;

    public SemanticAdminController(SemanticIndexerService indexer) {
        this.indexer = indexer;
    }

    @PostMapping("/reindex")
    public void reindexAll(@RequestParam(defaultValue = "1200") int maxChunkChars) {
        indexer.reindexAll(maxChunkChars);
    }

    @PostMapping("/reindex/{documentId}")
    public void reindexDoc(@PathVariable long documentId,
                           @RequestParam(defaultValue = "1200") int maxChunkChars) {
        indexer.reindexDocument(documentId, maxChunkChars);
    }
}
```

---

### Step 7: Update SearchController

**File:** `backend/src/main/java/com/searchlab/controller/SearchController.java`

Modify the search method to handle semantic mode. Here's the key change:

```java
// In the try block, replace the search execution:
String mode = request.getMode() != null ? request.getMode() : "traditional";
boolean fallbackUsed = false;

List<SearchResult> results;
long totalResults;

try {
    if ("semantic".equals(mode)) {
        // Try semantic search
        results = semanticSearchService.search(query, page, pageSize);
        totalResults = semanticSearchService.countResults(query);
    } else {
        // Traditional search
        TraditionalSearchService.SearchResultPage resultPage = 
            traditionalSearchService.search(query, page, pageSize);
        results = resultPage.getResults();
        totalResults = resultPage.getTotalElements();
    }
} catch (Exception e) {
    // Fallback to traditional on any error
    fallbackUsed = true;
    TraditionalSearchService.SearchResultPage resultPage = 
        traditionalSearchService.search(query, page, pageSize);
    results = resultPage.getResults();
    totalResults = resultPage.getTotalElements();
}
```

You'll also need to inject SemanticSearchService in the constructor.

---

## Implementation Checklist

- [ ] Update docker-compose.yml to use pgvector image
- [ ] Restart PostgreSQL container
- [ ] Create V4 migration (enable pgvector)
- [ ] Create V5 migration (create document_chunks table)
- [ ] Create embeddings-service folder and files
- [ ] Test embeddings service manually
- [ ] Add WebClient dependency to pom.xml
- [ ] Add embeddings config to application.yml
- [ ] Create EmbeddingClient.java
- [ ] Create VectorSql.java
- [ ] Create Chunker.java
- [ ] Create SemanticSearchRepository.java
- [ ] Create SemanticIndexerService.java
- [ ] Create SemanticSearchService.java
- [ ] Create SemanticAdminController.java
- [ ] Update SearchController to handle semantic mode
- [ ] Test indexing endpoint
- [ ] Test semantic search

---

## Testing Steps

1. Start PostgreSQL: `docker compose up -d`
2. Start backend: `mvn spring-boot:run` (migrations run automatically)
3. Start embeddings service: `cd embeddings-service && uvicorn main:app --port 8090`
4. Index documents: `curl -X POST http://localhost:8080/api/admin/reindex`
5. Test semantic search: Use Postman collection with mode=semantic

---

## Notes

- The embeddings service uses the all-MiniLM-L6-v2 model (384 dimensions)
- Chunking is simple paragraph-based (can be improved later)
- Vector search uses cosine distance (<=> operator)
- Fallback to traditional search on any semantic search error
- Indexing endpoint is dev-only (protected by @Profile("dev"))
