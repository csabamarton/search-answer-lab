package com.searchlab.semantic;

import java.util.ArrayList;
import java.util.List;

public final class Chunker {
    private Chunker() {}

    /**
     * Chunks text by paragraphs, respecting max character limit.
     * Simple approach: split by blank lines, combine until maxChars.
     */
    public static List<String> chunkByParagraphs(String text, int maxChars) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        
        String[] parts = text.split("\\R\\R+"); // Split by blank lines
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) continue;
            
            if (current.length() + part.length() + 2 > maxChars && current.length() > 0) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(part).append("\n\n");
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        
        return chunks.isEmpty() ? List.of(text) : chunks;
    }
}
