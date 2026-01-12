# Option A Implementation Plan - Embeddings Service

This document details how to add semantic search using a Python embeddings service.

## Current State

- Backend: Spring Boot with PostgreSQL FTS (traditional search working)
- Frontend: React
- Database: PostgreSQL on port 5433
- Search endpoint: POST /api/search with mode parameter

## Target State

Add semantic search using:
- Python embeddings-service (FastAPI)
- pgvector extension in PostgreSQL
- document_chunks table with embeddings
- Semantic search mode in backend

---

## Step 1: Update Folder Structure

Create these new folders/files:

```
search-answer-lab/
  backend/                    (existing)
  frontend/                   (existing)
  embeddings-service/         (NEW - Python FastAPI)
    main.py
    requirements.txt
    README.md
  infra/                      (NEW - or move docker-compose here)
    docker-compose.yml
  docker-compose.yml          (update or move to infra/)
```

---

## Step 2: Update Docker Compose for pgvector

Your current docker-compose.yml uses postgres:15-alpine. We need pgvector support.

Option A: Update existing docker-compose.yml
Option B: Create infra/docker-compose.yml

Recommended: Update existing file to use pgvector image.

Change the image from:
```
image: postgres:15-alpine
```

To:
```
image: pgvector/pgvector:pg15
```

This image includes the pgvector extension pre-installed.

---

## Step 3: Flyway Migrations (Backend)

Add two new migration files:

### V4__enable_pgvector.sql
```
CREATE EXTENSION IF NOT EXISTS vector;
```

### V5__create_document_chunks.sql
```
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

Note: We'll add the ivfflat index later if needed (requires ANALYZE and works better with many rows).

---

## Step 4: Python Embeddings Service

Create embeddings-service/ folder with:

### embeddings-service/requirements.txt
```
fastapi==0.110.0
uvicorn[standard]==0.27.1
sentence-transformers==2.6.1
torch>=2.0.0
```

### embeddings-service/main.py
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

### embeddings-service/README.md
```
# Embeddings Service

FastAPI service for generating text embeddings using sentence-transformers.

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
curl -X POST http://localhost:8090/embed ^
  -H "Content-Type: application/json" ^
  -d "{\"texts\":[\"hello world\",\"database indexing\"]}"
```
```

---

## Step 5: Backend Dependencies

Add to backend/pom.xml (in dependencies section):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

This provides WebClient for calling the embeddings service.

---

## Step 6: Backend Configuration

Add to backend/src/main/resources/application.yml (in dev profile section):

```yaml
  embeddings:
    base-url: http://localhost:8090
    timeout-seconds: 5
```

---

## Step 7: Backend Code Structure

Create these new packages/files:

### Package: com.searchlab.embeddings

**EmbeddingClient.java** - HTTP client for embeddings service

### Package: com.searchlab.semantic

**VectorSql.java** - Helper for converting Java lists to pgvector literals
**Chunker.java** - Simple text chunking utility
**SemanticSearchRepository.java** - Repository for vector search queries
**SemanticSearchService.java** - Service for semantic search
**SemanticIndexerService.java** - Service for indexing documents (chunking + embedding)
**SemanticAdminController.java** - Dev-only endpoints for reindexing

---

## Step 8: Update SearchController

Modify the existing SearchController to:
- Handle mode="semantic" 
- Call SemanticSearchService when mode is semantic
- Fallback to TraditionalSearchService on error
- Set fallbackUsed=true when fallback happens

---

## Implementation Order

1. Update docker-compose.yml (use pgvector image)
2. Add Flyway migrations (V4, V5)
3. Create embeddings-service folder and files
4. Test embeddings service manually
5. Add WebClient dependency to backend
6. Create EmbeddingClient
7. Create semantic package classes
8. Update SearchController
9. Test indexing endpoint
10. Test semantic search

---

## Testing Checklist

- [ ] Docker container starts with pgvector extension
- [ ] Migrations run successfully
- [ ] Embeddings service starts and responds to /health
- [ ] Embeddings service returns vectors for test texts
- [ ] Backend can call embeddings service
- [ ] /api/admin/reindex indexes documents
- [ ] document_chunks table has data
- [ ] POST /api/search with mode=semantic returns results
- [ ] Fallback to traditional works when embeddings service is down
