/**
 * MCP server setup and tool registration.
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListToolsRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { handleSearchDocs } from "./tools/searchDocs.js";

/**
 * Creates and configures the MCP server with search_docs tool.
 */
export async function createMcpServer(): Promise<Server> {
  const server = new Server(
    {
      name: "search-answer-lab-mcp-server",
      version: "0.1.0",
    },
    {
      capabilities: {
        tools: {},
      },
    }
  );

  // List tools handler
  server.setRequestHandler(ListToolsRequestSchema, async () => {
    return {
      tools: [
        {
          name: "search_docs",
          description: "Searches the SearchLab document store via the backend /api/search endpoint.",
          inputSchema: {
            type: "object",
            properties: {
              query: {
                type: "string",
                description: "Search query string",
              },
              mode: {
                type: "string",
                enum: ["traditional", "semantic"],
                description: "Search mode: 'traditional' for keyword search, 'semantic' for vector search",
              },
              page: {
                type: "number",
                description: "Page number (0-indexed)",
                minimum: 0,
              },
              pageSize: {
                type: "number",
                description: "Number of results per page",
                minimum: 1,
                maximum: 100,
              },
              traceId: {
                type: "string",
                description: "Optional trace ID for request tracking",
              },
            },
            required: ["query"],
          },
        },
      ],
    };
  });

  // Call tool handler
  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    if (request.params.name === "search_docs") {
      const result = await handleSearchDocs(request.params.arguments || {});
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(result, null, 2),
          },
        ],
      };
    }

    throw new Error(`Unknown tool: ${request.params.name}`);
  });

  return server;
}