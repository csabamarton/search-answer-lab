# Embeddings Service

FastAPI service for generating text embeddings using sentence-transformers. Converts text into vector representations for semantic search.

## Technology Stack

- Python 3.11+
- FastAPI
- sentence-transformers
- PyTorch
- Uvicorn

## Model

**Model:** `sentence-transformers/all-MiniLM-L6-v2`
- **Dimensions:** 384
- **Type:** Sentence embeddings
- **Normalization:** Yes (embeddings are normalized)
- **Use Case:** General-purpose semantic similarity

## Setup

### 1. Create Virtual Environment

```bash
python -m venv .venv
```

### 2. Activate Virtual Environment

**Windows:**
```bash
.venv\Scripts\activate
```

**Linux/Mac:**
```bash
source .venv/bin/activate
```

### 3. Install Dependencies

```bash
pip install -r requirements.txt
```

**Note:** Installing PyTorch and sentence-transformers may take several minutes on first run.

## Run

### Development (with auto-reload)

```bash
uvicorn main:app --port 8090 --reload
```

### Production

```bash
uvicorn main:app --port 8090 --host 0.0.0.0
```

Service will be available at `http://localhost:8090`

## API Endpoints

### Health Check

```bash
GET /health
```

**Response:**
```json
{
  "status": "ok",
  "model": "sentence-transformers/all-MiniLM-L6-v2"
}
```

### Generate Embeddings

```bash
POST /embed
Content-Type: application/json

{
  "texts": ["hello world", "database indexing"]
}
```

**Response:**
```json
{
  "model": "sentence-transformers/all-MiniLM-L6-v2",
  "dim": 384,
  "vectors": [
    [0.123, -0.456, ...],  // 384 dimensions
    [0.789, 0.012, ...]
  ]
}
```

**Features:**
- Batch processing (multiple texts in one request)
- Normalized embeddings (unit vectors)
- Returns 384-dimensional vectors

## Usage Examples

### Single Text

```bash
curl -X POST http://localhost:8090/embed \
  -H "Content-Type: application/json" \
  -d '{"texts": ["how to improve database performance"]}'
```

### Multiple Texts (Batch)

```bash
curl -X POST http://localhost:8090/embed \
  -H "Content-Type: application/json" \
  -d '{"texts": ["database", "postgresql", "indexing"]}'
```

### Health Check

```bash
curl http://localhost:8090/health
```

## Integration with Backend

The Spring Boot backend calls this service via `EmbeddingClient`:

1. **During Indexing:**
   - Backend chunks documents
   - Sends chunks to `/embed` endpoint
   - Receives vectors and stores in database

2. **During Search:**
   - Backend embeds user query
   - Uses vector for similarity search in database

## Configuration

### Change Model

Edit `main.py`:

```python
MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"  # Change this
model = SentenceTransformer(MODEL_NAME)
```

**Note:** If changing model dimensions, update database migration:
- Edit `V5__create_document_chunks.sql`
- Change `vector(384)` to match new dimensions
- Reindex all documents

### Change Port

```bash
uvicorn main:app --port 8090  # Change port number
```

Update backend `application.yml`:
```yaml
embeddings:
  base-url: http://localhost:8090  # Match port
```

## Performance

- **First Request:** Slower (model loading)
- **Subsequent Requests:** Fast (model cached in memory)
- **Batch Processing:** More efficient than individual requests
- **Memory Usage:** ~500MB (model size)

## Dependencies

See `requirements.txt`:

- `fastapi==0.110.0` - Web framework
- `uvicorn[standard]==0.27.1` - ASGI server
- `sentence-transformers==2.6.1` - Embeddings library
- `torch>=2.0.0` - PyTorch (ML framework)

## Troubleshooting

**Service won't start:**
- Check Python version: `python --version` (needs 3.11+)
- Verify virtual environment is activated
- Install dependencies: `pip install -r requirements.txt`

**Model download fails:**
- Check internet connection (model downloads on first use)
- Verify HuggingFace access
- Check disk space (~500MB for model)

**Slow performance:**
- First request loads model (one-time cost)
- Use batch requests for multiple texts
- Consider GPU acceleration for production

**Backend can't connect:**
- Verify service is running: `curl http://localhost:8090/health`
- Check port matches backend configuration
- Verify firewall/network settings

## Alternative Models

You can use other sentence-transformers models:

**Smaller (faster, less accurate):**
- `all-MiniLM-L6-v2` (current, 384 dims)
- `paraphrase-MiniLM-L3-v2` (384 dims)

**Larger (slower, more accurate):**
- `all-mpnet-base-v2` (768 dims)
- `sentence-transformers/all-MiniLM-L12-v2` (384 dims)

**Domain-specific:**
- `ms-marco-MiniLM-L-6-v3` (384 dims, optimized for search)

**Note:** Changing dimensions requires database migration update.

## Production Considerations

- Use process manager (systemd, supervisor)
- Add authentication/rate limiting
- Use GPU if available (faster inference)
- Monitor memory usage
- Consider model quantization for smaller footprint
- Add request logging
- Implement health checks for orchestration

## License

MIT
