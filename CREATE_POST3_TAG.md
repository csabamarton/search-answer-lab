# Creating Post #3 Tag (Traditional Only)

Since your current codebase includes both traditional and semantic search, here's how to create a tag for Post #3 that represents "traditional search only".

## Option 1: Create a Branch Without Semantic Code (Recommended)

This creates a clean "traditional only" state:

```bash
# Create a new branch from main
git checkout -b post-3-traditional-only

# Remove semantic-specific files
git rm -r backend/src/main/java/com/searchlab/semantic/
git rm -r backend/src/main/java/com/searchlab/embeddings/
git rm backend/src/main/resources/db/migration/V4__enable_pgvector.sql
git rm backend/src/main/resources/db/migration/V5__create_document_chunks.sql
git rm -r embeddings-service/

# Update SearchController to remove semantic mode handling
# (You'll need to manually edit this file to remove semantic references)

# Update frontend to remove semantic mode
# (Edit ModeToggle.tsx to only show traditional)

# Update application.yml to remove embeddings config
# (Remove the embeddings: section)

# Update pom.xml to remove webflux dependency (if not needed for other reasons)

# Commit the changes
git commit -m "Post #3: Traditional search only - removed semantic features

- Removed semantic search service and repository
- Removed embeddings client
- Removed pgvector migrations
- Removed embeddings service
- Simplified SearchController to traditional only
- Updated frontend to traditional mode only"

# Tag this commit
git tag -a v0.3-baseline -m "Post #3: Traditional Search Baseline

Features:
- PostgreSQL full-text search (FTS)
- Spring Boot REST API
- React UI with search results
- 11ms average latency
- 10 sample technical documents

Blog Post: Post #3 - Traditional Search Baseline"

# Push branch and tag
git push origin post-3-traditional-only
git push origin v0.3-baseline
```

## Option 2: Document Current State (Simpler)

Keep everything as-is and document that:
- **Post #3** = Use traditional mode only (semantic code exists but isn't used)
- **Post #4** = Full implementation with both modes

Just tag the current state for Post #4 (already done):
```bash
git tag -a v0.4-semantic -m "Post #4: Semantic Search"
git push origin v0.4-semantic
```

Then in your blog post #3, mention:
> "The codebase includes semantic search code, but for this post, we're only using traditional mode. See Post #4 for semantic implementation."

## Option 3: Create a Minimal Traditional Branch

Create a branch with just the traditional search essentials:

```bash
git checkout -b post-3-minimal
# Remove semantic files (same as Option 1)
# Keep only traditional search code
git tag -a v0.3-baseline -m "Post #3: Traditional Baseline"
```

## Recommendation

**Use Option 1** - It gives readers a clean "traditional only" codebase to explore, which is better for educational purposes. The extra work is worth it for clarity.

## After Creating Tags

1. **Create GitHub Releases:**
   - Go to: https://github.com/csabamarton/search-answer-lab/releases
   - Create release for `v0.3-baseline`
   - Create release for `v0.4-semantic`

2. **Update Blog Posts:**
   - Post #3: Link to `v0.3-baseline` tag
   - Post #4: Link to `v0.4-semantic` tag

3. **Update README:**
   - Add tag information (already done)
   - Add checkout instructions for each tag
