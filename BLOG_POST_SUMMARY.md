# Search Answer Lab - Implementation Summary

**For Blog Post: Traditional vs Semantic Search Comparison**

## What Was Built

A full-stack search application demonstrating two search approaches:
1. **Traditional Full-Text Search (FTS)** - PostgreSQL native FTS
2. **Semantic Vector Search** - AI-powered embeddings search

Both modes accessible via the same API endpoint with a `mode` parameter.

## Architecture

**3-Service Architecture:**
- **Backend** (Spring Boot) - REST API, handles both search modes
- **Frontend** (React) - UI with mode toggle
- **Embeddings Service** (Python FastAPI) - Generates vector embeddings

**Database:**
- PostgreSQL 15 with pgvector extension
- `documents` table - Full documents with FTS support
- `document_chunks` table - Chunked documents with vector embeddings

## Traditional Search Implementation

**Technology:** PostgreSQL Full-Text Search (FTS)

**How it works:**
1. Documents stored with auto-generated `tsvector` column
2. User query converted with `websearch_to_tsquery` (handles natural language)
3. Search uses `@@` operator on `search_vector` column
4. Results ranked by `ts_rank` (relevance scoring)
5. Snippets generated with `ts_headline` (PostgreSQL built-in)

**Key Features:**
- Fast and deterministic
- No external dependencies
- Keyword-based matching
- Works out of the box with PostgreSQL

**Code Location:**
- `TraditionalSearchService.java` - Core search logic
- `DocumentRepository.java` - Native SQL query with FTS

## Semantic Search Implementation

**Technology:** Vector embeddings + pgvector

**How it works:**
1. Documents chunked into paragraphs (max 1200 chars)
2. Chunks embedded via Python service (`sentence-transformers/all-MiniLM-L6-v2`)
3. Embeddings stored as `vector(384)` in `document_chunks` table
4. User query embedded on-the-fly
5. Vector similarity search using cosine distance (`<=>` operator)
6. Returns most semantically similar chunks

**Key Features:**
- Meaning-based matching (not just keywords)
- Finds conceptually similar content
- Handles synonyms and related concepts
- Automatic fallback to traditional if embeddings service unavailable

**Code Location:**
- `SemanticSearchService.java` - Semantic search logic
- `SemanticIndexerService.java` - Document chunking and indexing
- `EmbeddingClient.java` - HTTP client for embeddings service
- `SemanticSearchRepository.java` - Vector search queries (JdbcTemplate)

## Key Differences

| Aspect | Traditional (FTS) | Semantic (Vector) |
|--------|------------------|-------------------|
| **Matching** | Exact keywords | Conceptual similarity |
| **Query** | "database" | "how to improve database performance" |
| **Results** | Documents containing keywords | Documents with similar meaning |
| **Speed** | Very fast (~50ms) | Slower (~1500ms, includes embedding) |
| **Dependencies** | PostgreSQL only | Requires embeddings service |
| **Setup** | Automatic (tsvector) | Requires indexing step |
| **Best For** | Keyword search, exact matches | Conceptual queries, synonyms |

## Implementation Highlights

### Traditional Search
- Uses `websearch_to_tsquery` for user-friendly query parsing
- Single database query (content + count)
- Returns real relevance scores from PostgreSQL
- PostgreSQL-generated snippets with highlighting

### Semantic Search
- Separate Python service for embeddings (clean separation)
- Document chunking strategy (paragraph-based, 1200 char max)
- pgvector for efficient vector similarity search
- Graceful fallback mechanism (semantic → traditional on error)
- Dev-only admin endpoints for indexing

## Technical Decisions

**Why separate embeddings service?**
- Clean separation of concerns
- Easy to swap models (local vs hosted)
- Can scale independently
- Makes "AI part" explicit

**Why JdbcTemplate for vector search?**
- PostgreSQL `vector` type not directly supported by JPA
- Need custom SQL with `<=>` operator
- More control over vector literal formatting

**Why chunk documents?**
- Embeddings work better on smaller text segments
- Allows finding specific relevant sections
- Better semantic matching at paragraph level

## What's Working

✅ Both search modes fully functional
✅ Same API endpoint, different modes
✅ Automatic fallback (semantic → traditional)
✅ Real relevance scores for both
✅ PostgreSQL-generated snippets (traditional)
✅ Chunk-based snippets (semantic)
✅ Health checks for all services
✅ Postman collection for testing
✅ Frontend with mode toggle

## Example Results

**Query:** "how to improve database performance"

**Traditional Search:**
- Finds documents with exact keywords: "database", "performance", "improve"
- Ranked by keyword frequency and position

**Semantic Search:**
- Finds documents about: optimization, indexing, query tuning, monitoring
- Ranked by semantic similarity (even if keywords don't match exactly)
- Example: "Database Indexing Strategies" scored highest (0.51 similarity)

## Code Statistics

- **Backend:** 22 Java classes
- **Frontend:** React components with TypeScript
- **Embeddings Service:** Single Python file (FastAPI)
- **Database Migrations:** 5 Flyway migrations
- **API Endpoints:** 6 endpoints (health, search, admin)

## Next Steps (Future Enhancements)

- Hybrid search (combine both modes)
- Better chunking strategy (sentence-aware)
- Metrics tracking and analytics
- Caching layer
- Async embedding calls

## For Blog Readers

**To try it yourself:**
1. Clone the repository
2. Start services (see README.md)
3. Index documents: `POST /api/admin/reindex`
4. Compare results: same query, different modes

**Key Takeaway:**
Traditional search is fast and exact. Semantic search understands meaning but requires more setup. The best approach depends on your use case - or use both!
