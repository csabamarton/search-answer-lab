# Search Answer Lab - Backend

Spring Boot REST API providing both traditional full-text search and semantic vector search capabilities.

## Technology Stack

- Java 17
- Spring Boot 3.2.1
- Spring Data JPA
- PostgreSQL 15 + pgvector
- Flyway (database migrations)
- Maven
- WebFlux (for embeddings service client)
- Lombok

## Project Structure

```
backend/
├── src/main/java/com/searchlab/
│   ├── SearchAnswerLabApplication.java
│   ├── controller/
│   │   ├── SearchController.java          # Main search endpoint
│   │   ├── HealthController.java          # Health checks
│   │   └── SemanticAdminController.java   # Admin endpoints (dev only)
│   ├── service/
│   │   ├── TraditionalSearchService.java  # PostgreSQL FTS
│   │   ├── SemanticSearchService.java     # Vector search
│   │   ├── SemanticIndexerService.java    # Document indexing
│   │   └── MetricsService.java
│   ├── repository/
│   │   ├── DocumentRepository.java        # JPA repository
│   │   ├── SemanticSearchRepository.java  # Vector search (JdbcTemplate)
│   │   └── MetricsRepository.java
│   ├── embeddings/
│   │   └── EmbeddingClient.java           # HTTP client for embeddings service
│   ├── semantic/
│   │   ├── VectorSql.java                 # pgvector utilities
│   │   └── Chunker.java                   # Text chunking
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Document.java
│   │   │   └── SearchMetric.java
│   │   └── dto/
│   │       ├── SearchRequest.java
│   │       ├── SearchResponse.java
│   │       ├── SearchResult.java
│   │       └── SearchMetadata.java
│   └── config/
│       ├── CorsConfig.java
│       └── DatabaseConfig.java
└── src/main/resources/
    ├── application.yml
    └── db/migration/
        ├── V1__create_documents_table.sql
        ├── V2__create_metrics_table.sql
        ├── V3__seed_sample_documents.sql
        ├── V4__enable_pgvector.sql
        └── V5__create_document_chunks.sql
```

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL running (via docker-compose in project root)
- Embeddings service running (for semantic search)

## Getting Started

### 1. Start PostgreSQL

From project root:
```bash
docker compose up -d
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The API will start on `http://localhost:8080`

## Database Migrations

Flyway automatically runs migrations on application startup:

- **V1**: Creates `documents` table with full-text search support (tsvector)
- **V2**: Creates `search_metrics` table for analytics
- **V3**: Seeds 10 sample technical documents
- **V4**: Enables pgvector extension
- **V5**: Creates `document_chunks` table for vector embeddings

## API Endpoints

### Health Check

- `GET /api/health` - Application health status
- `GET /api/health/db` - Database connectivity check

### Search

- `POST /api/search` - Execute search query (traditional or semantic)

**Request Body:**
```json
{
  "query": "database performance",
  "mode": "semantic",  // "traditional" or "semantic"
  "page": 0,
  "pageSize": 10,
  "traceId": "optional-trace-id"
}
```

**Response:**
```json
{
  "results": [
    {
      "id": 1,
      "title": "Document Title",
      "content": "Full content...",
      "source": "source_name",
      "score": 0.85,
      "snippet": "Relevant snippet..."
    }
  ],
  "metadata": {
    "durationMs": 150,
    "totalResults": 10,
    "page": 0,
    "pageSize": 10,
    "searchMode": "semantic",
    "fallbackUsed": false
  },
  "requestId": "uuid"
}
```

### Admin Endpoints (Dev Only)

- `POST /api/admin/reindex` - Reindex all documents
  - Query param: `maxChunkChars` (default: 1200)
- `POST /api/admin/reindex/{documentId}` - Reindex specific document
  - Query param: `maxChunkChars` (default: 1200)

**Note:** Admin endpoints are only available in `dev` profile.

## Configuration

### Development Profile (default)

Configuration in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/searchlab
    username: searchlab
    password: searchlab

embeddings:
  base-url: http://localhost:8090
  timeout-seconds: 5
```

### CORS Configuration

Allows requests from `http://localhost:3000` for frontend integration.

## Search Modes

### Traditional Search

Uses PostgreSQL full-text search:
- Converts query with `websearch_to_tsquery` (handles natural language)
- Searches `documents.search_vector` column
- Ranks with `ts_rank`
- Generates snippets with `ts_headline`

**Best for:**
- Exact keyword matching
- Fast, deterministic results
- When embeddings service is unavailable

### Semantic Search

Uses vector embeddings:
- Embeds query via embeddings service
- Searches `document_chunks.embedding` using cosine distance
- Returns most semantically similar chunks
- Falls back to traditional if embeddings service fails

**Best for:**
- Conceptual queries
- Finding similar meaning (not exact keywords)
- Natural language questions

## Development

### Hot Reload

Spring Boot DevTools is included for automatic restart on code changes.

### SQL Logging

In development profile, SQL queries are logged to console for debugging.

### Testing

```bash
mvn test
```

### Building for Production

```bash
mvn clean package
java -jar target/search-answer-lab-0.0.1-SNAPSHOT.jar
```

## Dependencies

Key dependencies in `pom.xml`:

- `spring-boot-starter-web` - REST API
- `spring-boot-starter-webflux` - WebClient for embeddings service
- `spring-boot-starter-data-jpa` - JPA/Hibernate
- `postgresql` - PostgreSQL driver
- `flyway-core` - Database migrations
- `lombok` - Reduce boilerplate
- `spring-boot-starter-validation` - Input validation

## Architecture Notes

### Why JdbcTemplate for Vector Search?

The `document_chunks` table uses PostgreSQL's `vector` type, which isn't directly supported by JPA. We use `JdbcTemplate` for:
- Vector similarity queries (`<=>` operator)
- Inserting vector literals
- Custom SQL with pgvector functions

### EmbeddingClient

Uses Spring WebFlux `WebClient` to call the Python embeddings service:
- Synchronous blocking calls (could be made async in future)
- 5-second timeout
- Throws exception on failure (triggers fallback)

### Chunking Strategy

Simple paragraph-based chunking:
- Splits on blank lines
- Combines paragraphs up to `maxChunkChars` (default: 1200)
- Each chunk is embedded separately

## Troubleshooting

**Application won't start:**
- Check PostgreSQL is running: `docker compose ps`
- Verify database connection in `application.yml`
- Check port 8080 is available

**Semantic search fails:**
- Ensure embeddings service is running on port 8090
- Check `embeddings.base-url` in `application.yml`
- Verify documents are indexed: `SELECT COUNT(*) FROM document_chunks;`

**Migrations fail:**
- Check PostgreSQL version (needs 15+)
- Verify pgvector extension is available (use `pgvector/pgvector:pg15` image)
- Check database user has CREATE EXTENSION permission

## Next Steps

- [ ] Add hybrid search (combine traditional + semantic)
- [ ] Implement metrics tracking
- [ ] Add caching layer
- [ ] Improve chunking strategy (sentence-aware)
- [ ] Add batch embedding support
- [ ] Implement async embedding calls
