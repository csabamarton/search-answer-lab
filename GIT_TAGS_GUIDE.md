# Git Tags Guide - Blog Post Versions

This document explains how to use Git tags to access different stages of the project for each blog post.

## Current Tags

### v0.4-semantic (Current - Post #4)
**Tag:** `v0.4-semantic`  
**State:** Traditional FTS + Semantic Vector Search  
**Blog Post:** Post #4 - Semantic Search

**To checkout:**
```bash
git checkout v0.4-semantic
```

**What's included:**
- ✅ Traditional full-text search (PostgreSQL FTS)
- ✅ Semantic vector search (pgvector + embeddings)
- ✅ Python embeddings service
- ✅ Mode toggle in frontend
- ✅ Automatic fallback mechanism

---

## Creating Tags for Other Posts

### Option 1: Create Traditional-Only Branch (Recommended for Post #3)

Since your current codebase has both traditional and semantic, create a branch that represents "traditional only":

```bash
# Create a branch from current main
git checkout -b post-3-traditional-only

# Remove semantic-specific files
git rm -r backend/src/main/java/com/searchlab/semantic/
git rm -r backend/src/main/java/com/searchlab/embeddings/
git rm backend/src/main/resources/db/migration/V4__enable_pgvector.sql
git rm backend/src/main/resources/db/migration/V5__create_document_chunks.sql
git rm -r embeddings-service/

# Update SearchController to remove semantic mode
# (manually edit to only have traditional search)

# Update frontend to remove semantic mode
# (manually edit ModeToggle to only show traditional)

# Commit the changes
git commit -m "Post #3: Traditional search only - removed semantic features"

# Tag this branch
git tag -a v0.3-baseline -m "Post #3: Traditional Search Baseline - PostgreSQL FTS only"

# Push branch and tag
git push origin post-3-traditional-only
git push origin v0.3-baseline
```

### Option 2: Document Current State (Simpler)

Keep current codebase and document that:
- **Post #3** = Use traditional mode only (ignore semantic code)
- **Post #4** = Full implementation with both modes

Tag current state for Post #4:
```bash
git tag -a v0.4-semantic -m "Post #4: Semantic Search"
git push origin v0.4-semantic
```

---

## Recommended Approach

**For your blog posts, I recommend:**

1. **Tag current state as v0.4-semantic** (already done)
2. **Create a separate branch for Post #3** that removes semantic code
3. **Tag future posts** as you implement them:
   - `v0.5-hybrid` - Hybrid search
   - `v0.6-rag` - RAG answer generation

---

## Using Tags in Blog Posts

### In Your Blog Post #4:

```markdown
## Try It Yourself

The complete implementation is available on GitHub:

```bash
git clone https://github.com/csabamarton/search-answer-lab.git
cd search-answer-lab
git checkout v0.4-semantic
```

See [README.md](https://github.com/csabamarton/search-answer-lab) for setup instructions.
```

### In Your Blog Post #3:

```markdown
## Try It Yourself

Checkout the traditional search baseline:

```bash
git clone https://github.com/csabamarton/search-answer-lab.git
cd search-answer-lab
git checkout v0.3-baseline
```
```

---

## Tag Management

**List all tags:**
```bash
git tag -l
```

**View tag details:**
```bash
git show v0.4-semantic
```

**Delete a tag (if needed):**
```bash
git tag -d v0.4-semantic
git push origin :refs/tags/v0.4-semantic
```

**Push all tags:**
```bash
git push origin --tags
```

---

## Future Tags

As you implement more features, create tags:

```bash
# After implementing hybrid search
git tag -a v0.5-hybrid -m "Post #5: Hybrid Search - Combines traditional + semantic"

# After implementing RAG
git tag -a v0.6-rag -m "Post #6: RAG Answer Generation - LLM-powered answers"

# Push tags
git push origin --tags
```

---

## GitHub Releases

After creating tags, create GitHub Releases:

1. Go to: https://github.com/csabamarton/search-answer-lab/releases
2. Click "Create a new release"
3. Select tag (e.g., `v0.4-semantic`)
4. Add release notes with:
   - What's new in this version
   - Link to blog post
   - Key features
   - Performance metrics

This makes it easy for readers to download and try specific versions.
