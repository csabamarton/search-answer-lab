# Search Answer Lab - MCP Server

MCP (Model Context Protocol) server that exposes a `search_docs` tool, forwarding requests to the SearchLab Spring Boot backend.

## What It Is

This MCP server acts as an adapter, exposing the Spring Boot `/api/search` endpoint as an MCP tool. It provides a standardized interface for AI assistants to search the document knowledge base.

## Prerequisites

1. Spring Boot backend must be running at `http://localhost:8080` (or configured via `BACKEND_URL`)
2. Node.js 18+ installed
3. npm or yarn

## Setup

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the Spring Boot backend first (see backend README)

4. Run the MCP server:
   ```bash
   npm run dev
   ```

   Or build and run:
   ```bash
   npm run build
   npm start
   ```

## Configuration

Environment variables (in `.env` or environment):

- `BACKEND_URL` - Backend base URL (default: `http://localhost:8080`)
- `PORT` - Not used (server uses stdio transport)
- `REQUEST_TIMEOUT_MS` - HTTP request timeout in milliseconds (default: `8000`)

## Tool: search_docs

**Description:** Searches the SearchLab document store via the backend `/api/search` endpoint.

**Parameters:**
- `query` (required) - Search query string
- `mode` (optional) - `"traditional"` or `"semantic"` (default: `"semantic"`)
- `page` (optional) - Page number, 0-indexed (default: `0`)
- `pageSize` (optional) - Results per page, 1-100 (default: `10`)
- `traceId` (optional) - Request trace ID

**Example Usage:**

Traditional keyword search:
```json
{
  "query": "ts_rank_cd",
  "mode": "traditional"
}
```

Semantic vector search:
```json
{
  "query": "how to improve database performance",
  "mode": "semantic",
  "page": 0,
  "pageSize": 10
}
```

**Response:** Returns the backend JSON response unchanged, including:
- `results` - Array of search result items
- `metadata` - Search metadata (duration, pagination, mode, etc.)
- `requestId` - Request identifier

## Behavior

- ✅ MCP server starts on stdio transport
- ✅ Client can discover `search_docs` tool
- ✅ Tool calls forward to Spring `/api/search` endpoint
- ✅ Response is returned unchanged (pass-through)
- ✅ Invalid input is rejected with clear errors
- ✅ Backend errors are forwarded with status information

## Development

```bash
# Development mode (with hot reload via tsx)
npm run dev

# Build TypeScript
npm run build

# Run compiled JavaScript
npm start
```

## Architecture

- `src/types.ts` - TypeScript interfaces matching backend DTOs
- `src/config.ts` - Environment configuration
- `src/backendClient.ts` - HTTP client for Spring Boot API
- `src/tools/searchDocs.ts` - MCP tool handler with validation
- `src/server.ts` - MCP server setup and tool registration
- `src/index.ts` - Entry point

## Notes

- The server uses stdio transport (standard input/output) as required by MCP
- All validation mirrors backend bean validation constraints
- Mode defaults to `"semantic"` in the tool handler (backend defaults to `"traditional"`)