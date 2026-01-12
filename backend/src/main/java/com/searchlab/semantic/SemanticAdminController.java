package com.searchlab.semantic;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile("dev")
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class SemanticAdminController {

    private final SemanticIndexerService indexer;

    public SemanticAdminController(SemanticIndexerService indexer) {
        this.indexer = indexer;
    }

    @PostMapping("/reindex")
    public void reindexAll(@RequestParam(defaultValue = "1200") int maxChunkChars) {
        indexer.reindexAll(maxChunkChars);
    }

    @PostMapping("/reindex/{documentId}")
    public void reindexDoc(@PathVariable long documentId,
                           @RequestParam(defaultValue = "1200") int maxChunkChars) {
        indexer.reindexDocument(documentId, maxChunkChars);
    }
}
