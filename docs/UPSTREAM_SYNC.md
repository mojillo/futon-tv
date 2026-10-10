# Upstream Sync Guide

**Purpose:** Keep Futon TV in sync with upstream Futon updates while minimizing merge conflicts.

---

## Strategy

All TV-specific code lives in the **`:tv` module** or new standalone files. Upstream files in `:app` are touched only with small, clearly marked hooks.

### Rules

1. **TV code belongs in `:tv` module**
   - Focus helpers, TV launcher, key handlers, TV-specific UI
   - No upstream conflicts since `:tv` doesn't exist upstream

2. **Minimal hooks in upstream files**
   - Mark every TV hook with `// FUTON-TV` comment
   - Use conditional guards: `if (TvModeDetector.isTvMode(this)) { ... }`
   - Keep hooks as small as possible (1-5 lines)

3. **Track all upstream file changes**
   - See [Touched Upstream Files](#touched-upstream-files) below
   - Update this list when adding new hooks

4. **Preserve upstream behavior**
   - TV hooks must not change phone/tablet behavior
   - Test on both TV and phone after every merge

---

## Setup Upstream Remote

Add `AppFuton/Futon` as a remote to pull updates:

```bash
git remote add upstream https://github.com/AppFuton/Futon.git
git fetch upstream
```

Verify remotes:
```bash
git remote -v
# origin    https://github.com/mojillo/futon-tv.git (fetch)
# origin    https://github.com/mojillo/futon-tv.git (push)
# upstream  https://github.com/AppFuton/Futon.git (fetch)
# upstream  https://github.com/AppFuton/Futon.git (push)
```

---

## Merge Upstream Updates

### 1. Fetch Latest Upstream

```bash
git fetch upstream
```

### 2. Create Merge Branch

```bash
git checkout devel
git pull origin devel
git checkout -b sync/upstream-$(date +%Y%m%d)
```

### 3. Merge Upstream `devel`

```bash
git merge upstream/devel
```

### 4. Resolve Conflicts

**Common conflict areas:**

- **`app/build.gradle`**: `applicationId` changed to `io.github.mojillo.futontv`
  - **Resolution:** Keep our `applicationId`, accept other upstream changes

- **`app/src/main/AndroidManifest.xml`**: TV features, Leanback intent, banner
  - **Resolution:** Keep our TV additions, merge upstream activity/service changes

- **Activities with `// FUTON-TV` hooks**: Focus setup in `onCreate()`
  - **Resolution:** Keep our TV hooks, merge upstream lifecycle changes

- **Layouts with `android:focusable`**: Added TV focus attributes
  - **Resolution:** Keep our focus attributes, merge upstream view changes

**Find TV-specific changes:**
```bash
git diff devel upstream/devel | grep -C 3 "FUTON-TV"
```

### 5. Test After Merge

**On Phone/Tablet:**
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
# Test main flows: browse library, read manga
```

**On TV (emulator or device):**
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
# Test D-pad navigation: launcher → library → manga → reader
```

### 6. Update Touched Files List

Check if upstream added new files that need TV hooks:
```bash
git diff --name-only upstream/devel HEAD | grep "^app/"
```

Update [Touched Upstream Files](#touched-upstream-files) if needed.

### 7. Commit and Push

```bash
git add -A
git commit -m "sync: merge upstream Futon $(git rev-parse --short upstream/devel)

Merged from AppFuton/Futon@$(git rev-parse --short upstream/devel)

Conflicts resolved:
- app/build.gradle: kept applicationId
- AndroidManifest.xml: kept TV features
- [list other conflicts]"

git push origin sync/upstream-$(date +%Y%m%d)
```

### 8. Open PR

Create PR to `devel`, request review, ensure tests pass.

---

## Touched Upstream Files

**Last updated:** 2026-10-10

List of `:app` files modified from upstream Futon (excluding new TV-only files):

### Build & Config (2 files)
- `app/build.gradle` — `applicationId` changed to `io.github.mojillo.futontv`
- `app/src/main/AndroidManifest.xml` — TV features, Leanback intent, banner, provider authorities

### Core Base Classes (1 file)
- `app/src/main/kotlin/io/github/landwarderer/futon/core/ui/BaseActivity.kt` — TV key handler dispatch

### Main Navigation (2 files)
- `app/src/main/kotlin/io/github/landwarderer/futon/main/ui/MainActivity.kt` — Focus setup for bottom nav
- `app/src/main/res/layout/activity_main.xml` — Focus attributes (if any)

### Reader (2 files)
- `app/src/main/kotlin/io/github/landwarderer/futon/reader/ui/ReaderActivity.kt` — TV focus setup, long-press menu
- `app/src/main/res/layout/activity_reader.xml` — Focus attributes (if any)

### Details (2 files)
- `app/src/main/kotlin/io/github/landwarderer/futon/details/ui/DetailsActivity.kt` — Focus setup for tabs/chapters
- `app/src/main/res/layout/activity_details.xml` — Focus attributes (if any)

### Fragments (3-5 files)
- `app/src/main/kotlin/io/github/landwarderer/futon/favourites/ui/list/FavouritesListFragment.kt` — RecyclerView focus
- `app/src/main/kotlin/io/github/landwarderer/futon/history/ui/HistoryListFragment.kt` — RecyclerView focus
- `app/src/main/kotlin/io/github/landwarderer/futon/details/ui/pager/chapters/ChaptersFragment.kt` — RecyclerView focus
- *(+ other list fragments as needed)*

### Item Layouts (5-10 files)
- `app/src/main/res/layout/item_manga*.xml` — `android:focusable="true"`
- `app/src/main/res/layout/item_chapter.xml` — `android:focusable="true"`
- `app/src/main/res/layout/item_history.xml` — `android:focusable="true"`
- *(+ other item layouts as needed)*

### Strings (1 file)
- `app/src/main/res/values/strings.xml` — Sync authority strings for TV app ID

**Total upstream files touched:** ~15-25 files (most changes are 1-5 lines per file)

---

## Automated Sync Check (Optional)

**GitHub Action** to check for upstream updates weekly:

Create `.github/workflows/upstream-sync-check.yml`:

```yaml
name: Upstream Sync Check

on:
  schedule:
    - cron: '0 12 * * 1' # Every Monday at noon UTC
  workflow_dispatch: # Manual trigger

jobs:
  check-upstream:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Add upstream remote
        run: |
          git remote add upstream https://github.com/AppFuton/Futon.git
          git fetch upstream
      
      - name: Check for new commits
        id: check
        run: |
          LOCAL=$(git rev-parse devel)
          UPSTREAM=$(git rev-parse upstream/devel)
          if [ "$LOCAL" != "$UPSTREAM" ]; then
            echo "has_updates=true" >> $GITHUB_OUTPUT
            echo "upstream_sha=$UPSTREAM" >> $GITHUB_OUTPUT
          else
            echo "has_updates=false" >> $GITHUB_OUTPUT
          fi
      
      - name: Create issue if updates available
        if: steps.check.outputs.has_updates == 'true'
        uses: actions/github-script@v7
        with:
          script: |
            const sha = '${{ steps.check.outputs.upstream_sha }}';
            const title = `Upstream sync available: Futon@${sha.substring(0, 7)}`;
            const body = `New commits detected in upstream Futon repository.

            **Upstream commit:** https://github.com/AppFuton/Futon/commit/${sha}

            **To merge:**
            \`\`\`bash
            git fetch upstream
            git checkout -b sync/upstream-$(date +%Y%m%d)
            git merge upstream/devel
            # Resolve conflicts, test, then open PR
            \`\`\`

            See [UPSTREAM_SYNC.md](docs/UPSTREAM_SYNC.md) for full instructions.`;

            // Check if issue already exists
            const issues = await github.rest.issues.listForRepo({
              owner: context.repo.owner,
              repo: context.repo.repo,
              state: 'open',
              labels: 'upstream-sync'
            });

            if (issues.data.length === 0) {
              await github.rest.issues.create({
                owner: context.repo.owner,
                repo: context.repo.repo,
                title: title,
                body: body,
                labels: ['upstream-sync']
              });
            }
```

**To enable:** Merge this workflow file, then check Actions tab weekly for upstream update notifications.

---

## Tips

- **Merge often:** Sync monthly to avoid large conflicts
- **Test rigorously:** Every merge should pass on both phone and TV
- **Document conflicts:** Note unusual conflict resolutions in commit message
- **Keep `:tv` pure:** Never reference upstream code directly from `:tv` module; use dependency injection

---

**Last sync:** Never (initial fork)  
**Upstream commit:** TBD (track with `git rev-parse upstream/devel`)
