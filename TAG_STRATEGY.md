# Git Tag Strategy for Blog Posts

## Current Status ✅

**Tags Created:**
- ✅ `v0.3-baseline` - Post #3: Traditional Search Baseline (PostgreSQL FTS only)
- ✅ `v0.4-semantic` - Post #4: Semantic Search (Traditional + Semantic)

**Releases Created:**
- ✅ GitHub Release for `v0.3-baseline`
- ✅ GitHub Release for `v0.4-semantic`

**Branches:**
- `main` - Current development (full implementation)
- `post-3-traditional-only` - Traditional search only branch (tagged as v0.3-baseline)

## Using Tags

### For Blog Post #3 (Traditional Search)

```bash
git clone https://github.com/csabamarton/search-answer-lab.git
cd search-answer-lab
git checkout v0.3-baseline
```

**What's included:**
- ✅ Traditional full-text search (PostgreSQL FTS)
- ✅ Spring Boot REST API
- ✅ React frontend (traditional mode only)
- ✅ No semantic/embedding code
- ✅ Standard PostgreSQL (not pgvector)

### For Blog Post #4 (Semantic Search)

```bash
git clone https://github.com/csabamarton/search-answer-lab.git
cd search-answer-lab
git checkout v0.4-semantic
```

**What's included:**
- ✅ Traditional full-text search
- ✅ Semantic vector search (pgvector + embeddings)
- ✅ Python embeddings service
- ✅ Mode toggle in frontend
- ✅ Automatic fallback mechanism

## Tag Management

**List all tags:**
```bash
git tag -l
```

**View tag details:**
```bash
git show v0.3-baseline
git show v0.4-semantic
```

**Checkout a tag:**
```bash
git checkout v0.3-baseline  # Traditional only
git checkout v0.4-semantic  # Full implementation
```

**Return to main:**
```bash
git checkout main
```

## Future Tags

As you implement more features, create tags:

```bash
# After hybrid search
git tag -a v0.5-hybrid -m "Post #5: Hybrid Search"
git push origin v0.5-hybrid

# After RAG
git tag -a v0.6-rag -m "Post #6: RAG Answer Generation"
git push origin v0.6-rag
```

## GitHub Releases

All tags have corresponding GitHub Releases with:
- Release notes
- Links to blog posts
- Key features
- Performance metrics

View releases: https://github.com/csabamarton/search-answer-lab/releases

## Summary

✅ **Completed:**
- v0.3-baseline tag and release (Post #3)
- v0.4-semantic tag and release (Post #4)
- Traditional-only branch created
- All documentation updated

⏭️ **Next Steps:**
- Create tags/releases for future blog posts (v0.5-hybrid, v0.6-rag)
- Update README with new tags as they're created
