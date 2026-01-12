# Postman Collection for Search Answer Lab API

This folder contains Postman collections for testing the Search Answer Lab backend API.

## Collection File

- **Search-Answer-Lab-API.postman_collection.json** - Main collection with all API endpoints

## How to Import

1. Open Postman
2. Click **Import** button (top left)
3. Select **File** tab
4. Choose `Search-Answer-Lab-API.postman_collection.json`
5. Click **Import**

## Collection Structure

The collection is organized into folders:

### Health Endpoints

- **Health Check** (`GET /api/health`)
  - Basic health check endpoint
  - Returns application status and timestamp

- **Database Health Check** (`GET /api/health/db`)
  - Database connectivity check
  - Returns database connection status

### Search Endpoints

- **Traditional Search** (`POST /api/search`)
  - Full example with all fields
  
- **Traditional Search - Minimal** (`POST /api/search`)
  - Minimal request with only required `query` field
  
- **Traditional Search - Pagination** (`POST /api/search`)
  - Example with pagination (page, pageSize) and trace ID
  
- **Traditional Search - Multi-word** (`POST /api/search`)
  - Example with multiple search terms

## Environment Variables

The collection uses a variable:
- **`baseUrl`** - Default: `http://localhost:8080`

You can change this in Postman:
1. Click on the collection name
2. Go to **Variables** tab
3. Edit the `baseUrl` value

Or create a Postman Environment with different values for different environments (dev, staging, prod).

## Request Examples

### Health Check Request
```
GET http://localhost:8080/api/health
```

### Search Request
```json
POST http://localhost:8080/api/search
Content-Type: application/json

{
  "query": "database",
  "mode": "traditional",
  "page": 0,
  "pageSize": 10,
  "traceId": null
}
```

### Minimal Search Request
```json
POST http://localhost:8080/api/search
Content-Type: application/json

{
  "query": "postgresql"
}
```

## Response Examples

### Health Check Response
```json
{
  "status": "UP",
  "timestamp": "2024-01-11T18:30:00.123456"
}
```

### Database Health Check Response
```json
{
  "status": "UP",
  "database": "connected",
  "timestamp": "2024-01-11T18:30:00.123456"
}
```

### Search Response
```json
{
  "results": [
    {
      "id": 1,
      "title": "PostgreSQL Full-Text Search Guide",
      "content": "PostgreSQL provides powerful full-text search capabilities...",
      "source": "postgresql_docs",
      "score": null,
      "snippet": "...PostgreSQL provides powerful full-text search capabilities using tsvector..."
    }
  ],
  "metadata": {
    "durationMs": 45,
    "totalResults": 10,
    "page": 0,
    "pageSize": 10,
    "searchMode": "traditional",
    "fallbackUsed": false
  },
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Prerequisites

Before testing the endpoints, make sure:

1. **Database is running**:
   ```bash
   docker-compose up -d
   ```

2. **Backend is running**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. **Database migrations are applied** (automatically on startup)

## Testing Tips

- Use the **Minimal Search** request to test basic functionality
- Use **Pagination** request to test pagination with different page sizes
- Use **Multi-word** request to test complex queries
- Check the `metadata.durationMs` to measure search performance
- The `requestId` can be used for tracing requests
