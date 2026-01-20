/**
 * Entry point for MCP server.
 * Starts the server and begins listening on stdio.
 */
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { config } from "./config.js";
import { createMcpServer } from "./server.js";
async function main() {
    try {
        const server = await createMcpServer();
        const transport = new StdioServerTransport();
        await server.connect(transport);
        // Log to stderr so it doesn't interfere with MCP protocol
        console.error(`MCP server listening on stdio -> backend ${config.backendUrl}`);
    }
    catch (error) {
        console.error("Failed to start MCP server:", error.message);
        process.exit(1);
    }
}
main();
//# sourceMappingURL=index.js.map