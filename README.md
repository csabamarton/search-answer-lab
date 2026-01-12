# Search Answer Lab

A full-stack search application demonstrating traditional full-text search (FTS) vs semantic vector search. Built with React frontend, Spring Boot backend, and a Python embeddings service.

## 🏗️ Architecture

```
search-answer-lab/
├── backend/              # Spring Boot REST API (Java 17)
├── frontend/             # React + TypeScript + Vite
├── embeddings-service/   # Python FastAPI service for embeddings
└── docker-compose.yml    # PostgreSQL with pgvector extension
```

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- Python 3.11+
- Docker & Docker Compose

### 1. Start Database

```bash
docker compose up -d
```

This starts PostgreSQL 15 with pgvector extension on port 5433.

### 2. Start Backend

```bash
cd backend
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`

- Automatically runs Flyway migrations (creates tables, enables pgvector)
- Seeds 10 sample technical documents

### 3. Start Embeddings Service

```bash
cd embeddings-service
python -m venv .venv
.venv\Scripts\activate  # Windows
pip install -r requirements.txt
uvicorn main:app --port 8090 --reload
```

Embeddings service runs on `http://localhost:8090`

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`

### 5. Index Documents (First Time)

After all services are running, index documents for semantic search:

```bash
curl -X POST http://localhost:8080/api/admin/reindex
```

This chunks documents, generates embeddings, and stores them in the database.

## 🔍 Search Modes

### Traditional Search (FTS)

Uses PostgreSQL full-text search with `tsvector` and `tsquery`:

```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "database", "mode": "traditional"}'
```

**Features:**
- Keyword-based matching
- Fast and deterministic
- Uses PostgreSQL `websearch_to_tsquery` for natural language queries
- Returns relevance scores via `ts_rank`

### Semantic Search (Vector)

Uses vector embeddings for semantic similarity:

```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "how to improve database performance", "mode": "semantic"}'
```

**Features:**
- Meaning-based matching (finds conceptually similar content)
- Uses `sentence-transformers/all-MiniLM-L6-v2` model (384 dimensions)
- Vector similarity search with pgvector
- Automatic fallback to traditional search if embeddings service unavailable

## 📁 Project Structure

### Backend (`backend/`)

Spring Boot application providing REST API:

- **Controllers**: `SearchController`, `HealthController`, `SemanticAdminController`
- **Services**: `TraditionalSearchService`, `SemanticSearchService`, `SemanticIndexerService`
- **Repositories**: `DocumentRepository`, `SemanticSearchRepository`
- **Embeddings**: `EmbeddingClient` (calls Python service)

**Key Endpoints:**
- `POST /api/search` - Search (traditional or semantic)
- `GET /api/health` - Application health
- `GET /api/health/db` - Database health
- `POST /api/admin/reindex` - Index all documents (dev only)
- `POST /api/admin/reindex/{id}` - Index single document (dev only)

### Frontend (`frontend/`)

React application with TypeScript:

- Search input with mode toggle
- Results display with snippets
- Metrics bar (latency, results count)
- Responsive UI with Tailwind CSS

### Embeddings Service (`embeddings-service/`)

Python FastAPI service for generating embeddings:

- **Model**: `sentence-transformers/all-MiniLM-L6-v2` (384 dimensions)
- **Endpoints**:
  - `POST /embed` - Generate embeddings for text(s)
  - `GET /health` - Service health check

## 🗄️ Database Schema

### Tables

**documents**
- `id`, `title`, `content`, `source`
- `search_vector` (tsvector) - auto-generated for FTS
- `created_at`, `updated_at`

**document_chunks**
- `id`, `document_id`, `chunk_index`, `chunk_text`
- `embedding` (vector(384)) - for semantic search
- `created_at`, `updated_at`

**search_metrics**
- Tracks search performance and analytics

## 🧪 Testing

### Postman Collection

Import `backend/postman-collections/Search-Answer-Lab-API.postman_collection.json` into Postman.

**Collection includes:**
- Health checks (app, database, embeddings service)
- Traditional search examples
- Semantic search examples
- Admin endpoints (reindex)

### Manual Testing

**Test Traditional Search:**
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "postgresql", "mode": "traditional"}'
```

**Test Semantic Search:**
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "database optimization", "mode": "semantic"}'
```

**Test Embeddings Service:**
```bash
curl http://localhost:8090/health
curl -X POST http://localhost:8090/embed \
  -H "Content-Type: application/json" \
  -d '{"texts": ["hello world", "database indexing"]}'
```

## 🔧 Configuration

### Backend

Configuration in `backend/src/main/resources/application.yml`:

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

### Docker Compose

PostgreSQL configuration in `docker-compose.yml`:

- Port: `5433:5432` (to avoid conflicts with local PostgreSQL)
- Database: `searchlab`
- User: `searchlab` / Password: `searchlab`
- Image: `pgvector/pgvector:pg15` (includes pgvector extension)

## 📊 Features

- ✅ Traditional full-text search (PostgreSQL FTS)
- ✅ Semantic vector search (pgvector + embeddings)
- ✅ Automatic fallback (semantic → traditional on error)
- ✅ Document chunking and indexing
- ✅ Relevance scoring for both modes
- ✅ Pagination support
- ✅ Health checks for all services
- ✅ CORS enabled for frontend
- ✅ Dev-only admin endpoints

## 🛠️ Development

### Adding New Documents

Documents are seeded via Flyway migration `V3__seed_sample_documents.sql`. To add more:

1. Add to migration file, or
2. Insert directly into database, then run:
   ```bash
   curl -X POST http://localhost:8080/api/admin/reindex
   ```

### Changing Embeddings Model

Edit `embeddings-service/main.py`:

```python
MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"  # Change this
```

Then update `document_chunks.embedding` column type in migration if dimensions change.

### Database Migrations

Migrations are in `backend/src/main/resources/db/migration/`:

- `V1__create_documents_table.sql` - Documents table
- `V2__create_metrics_table.sql` - Metrics table
- `V3__seed_sample_documents.sql` - Sample data
- `V4__enable_pgvector.sql` - Enable pgvector extension
- `V5__create_document_chunks.sql` - Chunks table with embeddings

## 📚 Technology Stack

**Backend:**
- Spring Boot 3.2.1
- Spring Data JPA
- PostgreSQL 15 + pgvector
- Flyway (migrations)
- Lombok
- WebFlux (for embeddings service client)

**Frontend:**
- React 18
- TypeScript
- Vite
- Tailwind CSS
- Axios

**Embeddings:**
- Python 3.11+
- FastAPI
- sentence-transformers
- PyTorch

## 🐛 Troubleshooting

**Backend won't start:**
- Check PostgreSQL is running: `docker compose ps`
- Verify port 5433 is available
- Check `application.yml` configuration

**Embeddings service won't start:**
- Ensure Python virtual environment is activated
- Install dependencies: `pip install -r requirements.txt`
- Check port 8090 is available

**Semantic search returns no results:**
- Ensure embeddings service is running
- Run indexing: `curl -X POST http://localhost:8080/api/admin/reindex`
- Check `document_chunks` table has data

**Fallback to traditional search:**
- Check embeddings service health: `curl http://localhost:8090/health`
- Verify `embeddings.base-url` in `application.yml`
- Check backend logs for errors

## 📝 License

MIT
