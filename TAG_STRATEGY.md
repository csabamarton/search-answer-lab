# Git Tag Strategy for Blog Posts

## Current Status

✅ **Tag Created:** `v0.4-semantic` (pushed to GitHub)
- Represents: Post #4 - Semantic Search (current state)
- Includes: Traditional FTS + Semantic Vector Search

## What You Have Now

Your repository has:
- **Main branch:** Full implementation (traditional + semantic)
- **Tag v0.4-semantic:** Points to current commit (Post #4)

## Strategy for Blog Posts

### Option 1: Tags Only (Simplest)

**Current approach:**
- Tag `v0.4-semantic` = Post #4 (current state)
- For Post #3: Document that readers should use `mode: "traditional"` only
- Future posts: Create tags as you implement features

**Pros:**
- Simple, no extra branches
- One codebase to maintain
- Easy to reference

**Cons:**
- Post #3 readers see semantic code (even if unused)

### Option 2: Create Traditional-Only Branch (Cleaner)

**Recommended approach:**
1. Create branch `post-3-traditional-only`
2. Remove semantic-specific files
3. Tag that branch as `v0.3-baseline`
4. Keep `main` as current (with both modes)

**Pros:**
- Clean "traditional only" codebase for Post #3
- Better educational experience
- Clear separation

**Cons:**
- More work to maintain
- Need to keep branch updated if fixing bugs

## Recommended Action Plan

### Step 1: Tag Current State (Done ✅)

```bash
git tag -a v0.4-semantic -m "Post #4: Semantic Search"
git push origin v0.4-semantic
```

### Step 2: Create Post #3 Tag (Choose One)

**Option A: Simple (Recommended for now)**
- Keep current codebase
- In Post #3 blog, mention: "Use `mode: 'traditional'` only"
- Tag current state as `v0.3-baseline` with note: "Traditional mode only (semantic code present but unused)"

**Option B: Clean Branch**
- Follow instructions in `CREATE_POST3_TAG.md`
- Creates separate branch without semantic code
- More work but cleaner

### Step 3: Future Tags

As you implement features, create tags:

```bash
# After hybrid search
git tag -a v0.5-hybrid -m "Post #5: Hybrid Search"
git push origin v0.5-hybrid

# After RAG
git tag -a v0.6-rag -m "Post #6: RAG Answer Generation"
git push origin v0.6-rag
```

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
git checkout v0.3-baseline  # or v0.4-semantic and use traditional mode
```

**Note:** The codebase includes semantic search code, but for this post, we're only using traditional mode. See Post #4 for semantic implementation.
```

## Quick Commands

**List all tags:**
```bash
git tag -l
```

**Checkout a tag:**
```bash
git checkout v0.4-semantic
```

**Create a new tag:**
```bash
git tag -a v0.5-hybrid -m "Post #5: Hybrid Search"
git push origin v0.5-hybrid
```

**View tag details:**
```bash
git show v0.4-semantic
```

## GitHub Releases

After creating tags, create GitHub Releases:

1. Go to: https://github.com/csabamarton/search-answer-lab/releases
2. Click "Create a new release"
3. Select tag (e.g., `v0.4-semantic`)
4. Add release notes:
   - What's new
   - Link to blog post
   - Key features
   - Performance metrics

## Summary

**What's Done:**
- ✅ Tag `v0.4-semantic` created and pushed
- ✅ README updated with tag information

**What's Next:**
- Decide on Post #3 tag strategy (Option A or B above)
- Create GitHub Releases for tags
- Update blog posts with tag links
