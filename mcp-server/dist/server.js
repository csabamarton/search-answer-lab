/**
 * MCP server setup and tool registration.
 */
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { CallToolRequestSchema, ListToolsRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { handleSearchDocs } from "./tools/searchDocs.js";
/**
 * Creates and configures the MCP server with search_docs tool.
 */
export async function createMcpServer() {
    const server = new Server({
        name: "search-answer-lab-mcp-server",
        version: "0.1.0",
    }, {
        capabilities: {
            tools: {},
        },
    });
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
            try {
                const result = await handleSearchDocs(request.params.arguments || {});
                // If result already has content array (e.g., from auth instructions), return it directly
                if (result && typeof result === "object" && "content" in result && Array.isArray(result.content)) {
                    return result;
                }
                // Otherwise, wrap result in content array (normal search results)
                return {
                    content: [
                        {
                            type: "text",
                            text: JSON.stringify(result, null, 2),
                        },
                    ],
                };
            }
            catch (error) {
                // Re-throw to let MCP SDK handle it, but ensure error message is clear
                // The error message will be included in the JSON-RPC error response
                throw error;
            }
        }
        throw new Error(`Unknown tool: ${request.params.name}`);
    });
    return server;
}
//# sourceMappingURL=server.js.map