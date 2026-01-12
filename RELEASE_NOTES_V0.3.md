## Post #3: Traditional Search Baseline

This release implements the traditional full-text search baseline using PostgreSQL FTS (Full-Text Search).

### ✨ Features

- **PostgreSQL Full-Text Search** - Native FTS using `tsvector` and `tsquery`
- **Spring Boot REST API** - RESTful search endpoint
- **React Frontend** - Search UI with results display
- **Fast Performance** - ~11ms average latency
- **Sample Data** - 10 technical documents pre-loaded

### 🔍 How It Works

- Documents stored with auto-generated `tsvector` column
- User queries converted with `websearch_to_tsquery` (handles natural language)
- Search uses `@@` operator for matching
- Results ranked by `ts_rank` (relevance scoring)
- Snippets generated with `ts_headline` (PostgreSQL built-in)

### 📊 Performance

- **Average Latency**: ~11ms
- **Search Type**: Keyword-based matching
- **Dependencies**: PostgreSQL only (no external services)

### 🏗️ Architecture

- **Backend**: Spring Boot 3.2.1
- **Database**: PostgreSQL 15 with FTS support
- **Frontend**: React + TypeScript
- **Search Engine**: PostgreSQL native FTS

### 📖 Blog Post

Read the full implementation details: [Link to your Post #3 blog post]

### 🚀 Try It Yourself

```bash
git clone https://github.com/csabamarton/search-answer-lab.git
cd search-answer-lab
git checkout v0.3-baseline
```

**Note:** The codebase includes semantic search code (added in Post #4), but for this post, we're only using traditional mode. Use `mode: "traditional"` in your API calls.

See [README.md](README.md) for setup instructions.

### 📁 Key Files

- Search service: `backend/src/main/java/com/searchlab/service/TraditionalSearchService.java`
- Repository: `backend/src/main/java/com/searchlab/repository/DocumentRepository.java`
- Controller: `backend/src/main/java/com/searchlab/controller/SearchController.java`
- Database migrations: `backend/src/main/resources/db/migration/`

### 🔗 Related

- Next: [Post #4 - Semantic Search](https://github.com/csabamarton/search-answer-lab/releases/tag/v0.4-semantic)
