/**
 * Token storage utility for persisting OAuth tokens to disk.
 * Stores tokens in ~/.search-answer-lab/tokens.json
 */

import { promises as fs } from "fs";
import * as path from "path";
import * as os from "os";
import type { StoredTokens } from "../types/auth.js";

const TOKEN_DIR = ".search-answer-lab";
const TOKEN_FILE = "tokens.json";

/**
 * Get the full path to the token storage file.
 * Creates directory if it doesn't exist.
 */
async function getTokenPath(): Promise<string> {
  const homeDir = os.homedir();
  const tokenDir = path.join(homeDir, TOKEN_DIR);
  const tokenFile = path.join(tokenDir, TOKEN_FILE);

  // Ensure directory exists
  try {
    await fs.mkdir(tokenDir, { recursive: true, mode: 0o700 });
  } catch (error: any) {
    // Ignore if directory already exists
    if (error.code !== "EEXIST") {
      throw new Error(`Failed to create token directory: ${error.message}`);
    }
  }

  return tokenFile;
}

/**
 * Load tokens from disk.
 * Returns null if file doesn't exist or is invalid.
 */
export async function loadTokens(): Promise<StoredTokens | null> {
  try {
    const tokenPath = await getTokenPath();
    const data = await fs.readFile(tokenPath, "utf-8");
    const tokens = JSON.parse(data) as StoredTokens;

    // Validate structure
    if (
      !tokens.accessToken ||
      !tokens.refreshToken ||
      !tokens.expiresAt ||
      !Array.isArray(tokens.scopes) ||
      !tokens.userId
    ) {
      console.error("Invalid token file structure, clearing");
      await clearTokens();
      return null;
    }

    return tokens;
  } catch (error: any) {
    if (error.code === "ENOENT") {
      // File doesn't exist - this is normal for first-time use
      return null;
    }
    console.error(`Error loading tokens: ${error.message}`);
    return null;
  }
}

/**
 * Save tokens to disk.
 * Sets file permissions to 600 (read/write owner only).
 */
export async function saveTokens(tokens: StoredTokens): Promise<void> {
  try {
    const tokenPath = await getTokenPath();
    const data = JSON.stringify(tokens, null, 2);
    await fs.writeFile(tokenPath, data, { encoding: "utf-8", mode: 0o600 });
  } catch (error: any) {
    throw new Error(`Failed to save tokens: ${error.message}`);
  }
}

/**
 * Clear tokens from disk (delete file).
 */
export async function clearTokens(): Promise<void> {
  try {
    const tokenPath = await getTokenPath();
    await fs.unlink(tokenPath);
  } catch (error: any) {
    if (error.code !== "ENOENT") {
      // Ignore if file doesn't exist
      console.error(`Error clearing tokens: ${error.message}`);
    }
  }
}
