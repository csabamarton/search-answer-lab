/**
 * Environment configuration for MCP server.
 */
export interface Config {
    backendUrl: string;
    port: number;
    requestTimeoutMs: number;
}
/**
 * Reads and validates configuration from environment variables.
 */
export declare function loadConfig(): Config;
export declare const config: Config;
//# sourceMappingURL=config.d.ts.map