# Futon TV Plan - Android TV Support

**Author:** Cursor Cloud Agent  
**Date:** 2026-10-10  
**Purpose:** Plan for adding Android TV / Google TV support to Futon manga reader with D-pad navigation

---

## Executive Summary

Futon is a Kotatsu-based manga reader built entirely with Android XML views (no Jetpack Compose). The codebase has **1,223 Kotlin files** and **158 XML layouts** with a feature-based architecture. The reader already has partial D-pad support via `ReaderControlDelegate`, but the rest of the app has minimal focus management. TV support should be isolated in a new `:tv` module with focus styles, drawables, and helpers, with minimal hooks into the main codebase to maintain easy syncing with upstream Futon.

---

## 1. Module and Architecture Overview

### Current Structure

**Single Module:** `:app`  
- **Package:** `io.github.landwarderer.futon`
- **Min SDK:** 23 (Android 6.0+)
- **Target SDK:** 36
- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL

### Technology Stack

- **UI:** 100% Android XML views + ViewBinding (no Compose found)
- **DI:** Hilt (Activities, Fragments, ViewModels, Workers)
- **Database:** Room
- **Async:** Kotlin Coroutines + Flow
- **Navigation:** Fragment-based with `BottomNavigationView` / `NavigationRailView`
- **Image Loading:** Coil3
- **Reader:** Custom ViewPager2-based pager + RecyclerView-based webtoon scroller

### Feature Organization

```
app/src/main/kotlin/io/github/landwarderer/futon/
├── core/              # Shared: db, network, prefs, ui base classes
├── main/              # MainActivity, bottom nav, search
├── reader/            # Reader activity, pagers (standard/webtoon/double)
├── details/           # Manga details, chapter list
├── favourites/        # Library/favorites with categories
├── history/           # Reading history
├── explore/           # Browse sources
├── search/            # Search across sources
├── local/             # Local manga storage
├── download/          # Download manager
├── bookmarks/         # Bookmarks
├── settings/          # Settings screens
├── tracker/           # Manga tracking & updates feed
├── scrobbling/        # AniList, MAL, Shikimori, Kitsu
├── sync/              # Cloud sync
└── ...                # (16 more feature packages)
```

**Dependencies:** 682 XML resource files, heavy use of Material Design 3 components, RecyclerViews everywhere.

---

## 2. Screen-by-Screen Inventory

### 2.1 MainActivity (Entry Point)

**File:** `main/ui/MainActivity.kt`  
**Layout:** `layout/activity_main.xml` (portrait), `layout-w600dp-land/activity_main.xml` (landscape with `NavigationRailView`)

**Current State:**
- **Navigation:** `SlidingBottomNavigationView` (bottom nav) or `NavigationRailView` (rail on wide screens)
- **AppBar:** `SearchBar` + `SearchView` (Material3)
- **Search:** `RecyclerView` with suggestions
- **FAB:** Extended FAB for "Continue Reading"

**D-pad Usability:**
- **Focusable?** Bottom nav items are standard Material buttons → likely focusable
- **Visible Focus?** No custom focus drawables → system default only
- **Gesture-Only?** `SearchView` slide-up is gesture-heavy; search button is tappable but expand animation may confuse TV
- **Traps?** FAB is anchored to bottom nav; if focused, user may get stuck without clear back navigation

**Hooks Needed:**
1. Focus highlight drawable on bottom nav items
2. Initial focus on the currently selected nav item
3. D-pad CENTER on search bar to open search (bypass slide gesture)
4. Handle FAB focus trap (wrap around to nav bar, or hide FAB on TV)

---

### 2.2 History (NavItem.HISTORY)

**Files:** `history/ui/HistoryListFragment.kt`, `layout/fragment_list.xml`  
**UI:** `RecyclerView` with manga cards

**Current State:**
- Standard list fragment
- Action mode for multi-select (long-press)
- Pull-to-refresh

**D-pad Usability:**
- **Focusable?** RecyclerView items are likely focusable
- **Visible Focus?** No custom focus
- **Gesture-Only?** Pull-to-refresh requires swipe
- **Traps?** Multi-select action mode activated by long-press; on TV, DPAD_CENTER long-press may not work consistently

**Hooks Needed:**
1. Focus drawable on manga card items
2. Replace pull-to-refresh with a "Refresh" button or menu item
3. Action mode: add toolbar button for multi-select instead of long-press

---

### 2.3 Favorites (NavItem.FAVORITES)

**Files:** `favourites/ui/FavouritesActivity.kt`, `favourites/ui/container/FavouritesContainerFragment.kt`, `favourites/ui/list/FavouritesListFragment.kt`  
**Layout:** `layout/fragment_favourites_container.xml` (ViewPager2 + TabLayout)

**Current State:**
- **Tabs:** Horizontal tabs for categories
- **List:** RecyclerView grid of manga cards
- **Actions:** Long-press for multi-select, drag-and-drop reorder

**D-pad Usability:**
- **Focusable?** RecyclerView grid likely focusable; TabLayout tabs need explicit focus
- **Visible Focus?** No custom focus
- **Gesture-Only?** Swipe between tabs, drag-and-drop reorder
- **Traps?** TabLayout focus may require LEFT/RIGHT, conflicting with grid navigation

**Hooks Needed:**
1. Focus drawables on tabs and grid items
2. LEFT/RIGHT on tabs, then DOWN to enter grid
3. Replace swipe-to-tab with explicit LEFT/RIGHT on TabLayout
4. Drag-and-drop: add "Move" mode with UP/DOWN + OK to reorder

---

### 2.4 Local Manga (NavItem.LOCAL)

**Files:** `local/ui/LocalListFragment.kt`, same layout as history

**Current State:**
- RecyclerView list of local manga
- Import from device storage

**D-pad Usability:**
- Same as History fragment

**Hooks Needed:**
- Same as History (focus drawables, import button accessibility)

---

### 2.5 Explore (NavItem.EXPLORE)

**Files:** `explore/`, `list/ui/MangaListFragment.kt`, `remotelist/ui/RemoteListFragment.kt`  
**Layout:** `layout/fragment_explore.xml` (RecyclerView with sources/categories)

**Current State:**
- Browse manga by source
- Filter and sort options
- Chips for quick filters

**D-pad Usability:**
- **Focusable?** RecyclerView items yes, chips need focus
- **Visible Focus?** No custom focus
- **Gesture-Only?** Chip scrolling is horizontal RecyclerView (OK with D-pad LEFT/RIGHT)
- **Traps?** Nested RecyclerViews (chip row + manga grid) can cause focus confusion

**Hooks Needed:**
1. Focus drawables on source items and manga cards
2. Clear focus order: DOWN from chips enters manga grid
3. Ensure chips honor LEFT/RIGHT navigation

---

### 2.6 Search (Multi-Source)

**Files:** `search/ui/multi/SearchActivity.kt`, `search/ui/suggestion/`  
**Layout:** `layout/activity_search.xml` (SearchView + RecyclerView)

**Current State:**
- `SearchView` with EditText
- Suggestion list as you type
- Results in RecyclerView

**D-pad Usability:**
- **Focusable?** EditText is focusable, RecyclerView items too
- **Visible Focus?** No custom focus
- **Gesture-Only?** Search expand animation may be gesture-based
- **Traps?** Keyboard input on TV requires on-screen keyboard or voice input; search without keyboard is limited

**Hooks Needed:**
1. Auto-open on-screen keyboard when search EditText gains focus
2. Voice input button (already exists via `VoiceInputContract`)
3. Focus drawables on suggestion items
4. Pre-populate with popular searches or recent queries

---

### 2.7 Manga Details

**Files:** `details/ui/DetailsActivity.kt`, `details/ui/pager/chapters/ChaptersFragment.kt`, `details/ui/pager/pages/PagesFragment.kt`, `details/ui/pager/bookmarks/BookmarksFragment.kt`  
**Layout:** `layout/activity_details.xml` (NestedScrollView + ViewPager2 for tabs)

**Current State:**
- **Tabs:** Info, Chapters, Pages, Bookmarks
- **Chapter List:** RecyclerView with download/read buttons
- **Actions:** FAB to start reading, toolbar menu for favorite/share

**D-pad Usability:**
- **Focusable?** RecyclerView items, tabs, FAB, toolbar buttons
- **Visible Focus?** No custom focus
- **Gesture-Only?** Swipe between tabs, scroll through long chapter lists
- **Traps?** Focus order: tabs → chapter list → FAB → toolbar is unclear

**Hooks Needed:**
1. Focus drawables on tabs, chapter items, FAB
2. LEFT/RIGHT on tabs to switch
3. DOWN from tabs to enter chapter list
4. FAB focus last or on RIGHT edge
5. Long manga descriptions: ensure scrollable with DPAD_DOWN

---

### 2.8 Reader (Standard & Webtoon)

**Files:** `reader/ui/ReaderActivity.kt`, `reader/ui/ReaderControlDelegate.kt`, `reader/ui/pager/standard/`, `reader/ui/pager/webtoon/`  
**Layouts:** `layout/activity_reader.xml`, `layout/fragment_reader_pager.xml`, `layout/fragment_reader_webtoon.xml`

**Current State:**
- **Standard Reader:** ViewPager2 for page-by-page navigation
- **Webtoon Reader:** RecyclerView with vertical scroll
- **Controls:** Tap zones for prev/next, volume buttons, D-pad support already exists
- **Menus:** Toolbar (top), actions bar (bottom), config sheet

**D-pad Support (Already Implemented in `ReaderControlDelegate`):**
- `DPAD_LEFT` / `DPAD_RIGHT` → prev/next page (respects LTR/RTL)
- `DPAD_UP` / `DPAD_DOWN` → scroll or prev/next page
- `DPAD_CENTER` → toggle UI visibility
- `KEYCODE_NAVIGATE_NEXT` / `KEYCODE_NAVIGATE_PREVIOUS` → page navigation
- `PAGE_UP` / `PAGE_DOWN` → page navigation
- `SPACE` → next page
- Volume buttons (if enabled in settings)
- Ctrl+arrow keys → chapter skip

**Current Issues:**
- **Webtoon Zoom:** `WebtoonScalingFrame.onKeyDown` handles `KEYCODE_ZOOM_IN` / `KEYCODE_ZOOM_OUT` / `KEYCODE_ESCAPE`, but these are keyboard codes, not remote codes
- **Menu Access:** Menus are shown via tap or swipe; no explicit menu button mapping
- **Page Thumbnails:** `PagesFragment` has a grid of page thumbnails; needs focus support
- **Config Sheet:** `ReaderConfigSheet` is a BottomSheetDialogFragment with sliders and checkboxes; needs focus order

**D-pad Usability:**
- **Focusable?** Reader content is focusable (ViewPager2 / RecyclerView), menus need focus
- **Visible Focus?** No focus indicator on page content (not needed), but menus need focus
- **Gesture-Only?** Tap zones for prev/next; on TV, D-pad LEFT/RIGHT already works
- **Traps?** If UI is visible, focus may be on toolbar/actions bar; user must hide UI to return to reading

**Hooks Needed:**
1. Map `KEYCODE_MENU` or long-press CENTER to open reader menu
2. Focus drawables on toolbar buttons, actions bar buttons, config sheet items
3. Ensure config sheet sliders are navigable with LEFT/RIGHT, UP/DOWN for next control
4. Page thumbnails: grid focus with LEFT/RIGHT/UP/DOWN
5. Add on-screen hint: "Press BACK to hide controls, CENTER to show"

---

### 2.9 Settings

**Files:** `settings/SettingsActivity.kt`, `settings/` (many fragments)  
**Layout:** Preference screens (AndroidX Preference library)

**Current State:**
- PreferenceFragmentCompat-based screens
- Nested preference screens, switches, dropdowns

**D-pad Usability:**
- **Focusable?** Preferences are focusable by default
- **Visible Focus?** System default, may need custom focus
- **Gesture-Only?** No gestures in settings
- **Traps?** Nested screens may lack clear breadcrumbs; BACK navigation is essential

**Hooks Needed:**
1. Add TV-specific setting: "TV Mode" toggle (hide features irrelevant on TV, e.g., vibration)
2. Focus drawable on preference items
3. Ensure dropdowns (ListPreference) are navigable with DPAD

---

### 2.10 Downloads

**Files:** `download/ui/list/DownloadsActivity.kt`, `download/ui/DownloadQueueActivity.kt`  
**Layout:** `layout/activity_downloads.xml` (RecyclerView + queue)

**Current State:**
- List of downloaded manga
- Queue with progress indicators

**D-pad Usability:**
- Same as list fragments

**Hooks Needed:**
- Focus drawables on download items, queue items

---

### 2.11 Dialogs and Bottom Sheets

**Examples:** `ReaderConfigSheet`, favorite category picker, restore backup dialog

**Current State:**
- Material3 BottomSheetDialogFragments
- AlertDialogs with custom layouts

**D-pad Usability:**
- **Focusable?** Buttons are focusable, but focus order in dialogs is often unclear
- **Visible Focus?** No custom focus
- **Gesture-Only?** Dismiss by swipe-down (not accessible on TV)
- **Traps?** Focus may not land on the most relevant button (e.g., "OK" vs "Cancel")

**Hooks Needed:**
1. Focus drawables on dialog buttons
2. Initial focus on default action button (e.g., "OK")
3. Ensure BACK dismisses dialog
4. For bottom sheets, disable swipe-to-dismiss on TV; require explicit BACK or "Close" button

---

## 3. Reader Input Analysis

### Current Input Methods

1. **Touch:**
   - **Tap Zones:** Configurable grid (left/right/center for prev/next/menu)
   - **Swipe:** Page turn in standard reader
   - **Pinch-Zoom:** Zoom in/out (standard reader)
   - **Scroll:** Vertical scroll (webtoon reader)

2. **Hardware Keys (Already Supported):**
   - **D-pad:** LEFT/RIGHT (page), UP/DOWN (scroll or page), CENTER (toggle UI)
   - **Volume Buttons:** Page navigation (if enabled)
   - **Arrow Keys:** Page navigation, Ctrl+arrow for chapter skip
   - **Page Up/Down, Space, Navigate Next/Prev:** Page navigation
   - **Zoom In/Out, Escape:** Webtoon zoom controls

3. **Toolbar/Actions Bar:**
   - Previous/next chapter buttons
   - Settings, bookmarks, share

### Proposed Remote Key Mapping

| Remote Button       | Current Action                     | Proposed TV Action                                  |
|---------------------|------------------------------------|----------------------------------------------------|
| **D-pad LEFT**      | Previous page (respects LTR/RTL)   | ✅ Keep (works well)                                |
| **D-pad RIGHT**     | Next page (respects LTR/RTL)       | ✅ Keep (works well)                                |
| **D-pad UP**        | Scroll up or previous page         | ✅ Keep (webtoon scroll, pager page)                |
| **D-pad DOWN**      | Scroll down or next page           | ✅ Keep (webtoon scroll, pager page)                |
| **D-pad CENTER/OK** | Toggle UI visibility               | ✅ Keep; add long-press for reader menu             |
| **BACK**            | Exit reader (Android back)         | ✅ Keep; if UI visible, hide UI first, then exit    |
| **MENU** (if available) | *(not mapped)*                | 🆕 Open reader config sheet                        |
| **PLAY/PAUSE**      | *(not mapped)*                     | 🆕 Toggle auto-scroll timer (webtoon)               |
| **FAST_FORWARD**    | *(not mapped)*                     | 🆕 Next chapter                                     |
| **REWIND**          | *(not mapped)*                     | 🆕 Previous chapter                                 |
| **Channel UP/DOWN** | *(not mapped)*                     | 🆕 Jump 10 pages forward/back                       |

**Zoom Controls (Webtoon/Standard):**
- Currently: `KEYCODE_ZOOM_IN` / `KEYCODE_ZOOM_OUT` (keyboard only)
- Proposed: Add `ZoomControl` widget (already exists in `activity_reader.xml`, visibility controlled); show on TV if user presses CENTER on zoom-sensitive content, navigate with UP/DOWN, trigger with CENTER

**Menu Access:**
- Current: Tap center zone or tap top/bottom edge
- Proposed: Map `KEYCODE_MENU` or long-press CENTER to open `ReaderConfigSheet`

**Page Thumbnails (Chapter Browser):**
- Current: Tap "Pages" tab in `DetailsActivity`
- Proposed: Add shortcut in reader menu; grid navigable with D-pad, CENTER to jump to page

---

## 4. Proposed `:tv` Module Design

### Module Structure

```
tv/
├── src/main/
│   ├── kotlin/io/github/mojillo/futontv/
│   │   ├── TvModeDetector.kt              # Detect TV vs phone/tablet
│   │   ├── focus/
│   │   │   ├── FocusHelper.kt             # Extensions for focus management
│   │   │   └── TvKeyHandler.kt            # Global key event handler
│   │   ├── ui/
│   │   │   ├── TvLauncherActivity.kt      # Leanback launcher entry
│   │   │   └── TvWelcomeFragment.kt       # TV onboarding
│   │   └── settings/
│   │       └── TvSettingsFragment.kt      # TV-specific settings
│   ├── res/
│   │   ├── drawable/                       # Focus drawables (bg_focus_*.xml)
│   │   ├── layout/
│   │   │   ├── activity_tv_launcher.xml
│   │   │   └── fragment_tv_welcome.xml
│   │   ├── values/
│   │   │   ├── tv_styles.xml              # Focus styles, button styles
│   │   │   └── tv_dimens.xml              # Larger touch targets
│   │   └── xml/
│   │       └── tv_preferences.xml         # TV settings
│   └── AndroidManifest.xml                 # Leanback intent, banner
└── build.gradle.kts
```

### Key Components

#### 4.1 `TvModeDetector`

```kotlin
object TvModeDetector {
    fun isTvMode(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
```

**Usage:** `if (TvModeDetector.isTvMode(this)) { /* TV-specific behavior */ }`

#### 4.2 `FocusHelper`

Kotlin extensions for focus management:

```kotlin
fun View.applyTvFocus() {
    isFocusable = true
    isFocusableInTouchMode = false
    background = TvFocusDrawable(context, background)
}

fun RecyclerView.enableTvFocus() {
    descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
    // Ensure items are focusable
}

class TvFocusDrawable(context: Context, private val originalBackground: Drawable?) : StateListDrawable() {
    init {
        addState(intArrayOf(android.R.attr.state_focused), ContextCompat.getDrawable(context, R.drawable.bg_focus_stroke))
        addState(intArrayOf(), originalBackground)
    }
}
```

**Drawables:**
- `bg_focus_stroke.xml`: 4dp stroke, accent color, 12dp corner radius
- `bg_focus_scale.xml`: Scale + stroke (optional, for emphasis)

#### 4.3 `TvKeyHandler`

Global key event handler for app-wide shortcuts:

```kotlin
class TvKeyHandler {
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                // Open settings or context menu
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                // Toggle auto-scroll (if in reader)
                true
            }
            else -> false
        }
    }
}
```

Inject into `BaseActivity.dispatchKeyEvent()`.

#### 4.4 `TvLauncherActivity`

Separate launcher activity for TV (Leanback intent):

```kotlin
@AndroidEntryPoint
class TvLauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show TV welcome screen or jump to MainActivity
        if (isFirstLaunch()) {
            showFragment(TvWelcomeFragment())
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
```

**Purpose:** Introduce TV UI, explain D-pad controls, guide user to library setup.

---

## 5. Minimal-Hook Strategy for Upstream Files

**Goal:** Minimize changes to Futon's core files to ease merging from upstream.

### Upstream Files Requiring Changes (Estimated ~20–30 files)

#### 5.1 Manifest (`app/src/main/AndroidManifest.xml`)

**Changes:**
1. Add `:tv` module as dependency (Gradle only, not manifest merge needed if `tv/` is a feature module)
2. Declare `TvLauncherActivity` with Leanback intent
3. Add TV feature declarations:
   ```xml
   <uses-feature android:name="android.software.leanback" android:required="false" />
   <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
   ```
4. Add `android:banner` attribute to `<application>` (320x180 banner image)

**Lines Changed:** ~10 additions

---

#### 5.2 Activities (Add focus hooks)

**Files to Modify:**

1. **`core/ui/BaseActivity.kt`**
   ```kotlin
   override fun dispatchKeyEvent(event: KeyEvent): Boolean {
       if (TvModeDetector.isTvMode(this) && TvKeyHandler.handleKeyEvent(event.keyCode, event)) {
           return true
       }
       return super.dispatchKeyEvent(event)
   }
   ```

2. **`main/ui/MainActivity.kt`**
   - In `onCreate()`, after `setContentView()`:
     ```kotlin
     if (TvModeDetector.isTvMode(this)) {
         viewBinding.bottomNav?.enableTvFocus()
         viewBinding.navRail?.enableTvFocus()
     }
     ```

3. **`reader/ui/ReaderActivity.kt`**
   - In `onCreate()`, add:
     ```kotlin
     if (TvModeDetector.isTvMode(this)) {
         viewBinding.toolbar.applyTvFocus()
         viewBinding.actionsView.applyTvFocus()
     }
     ```
   - In `onKeyDown()`, add long-press CENTER for menu:
     ```kotlin
     override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
         if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event?.isLongPress == true) {
             openReaderMenu()
             return true
         }
         return super.onKeyDown(keyCode, event)
     }
     ```

4. **`details/ui/DetailsActivity.kt`**
   - Add `applyTvFocus()` to tabs, FAB, toolbar

5. **`favourites/ui/FavouritesActivity.kt`**
   - Add `applyTvFocus()` to TabLayout

6. **List Fragments** (`HistoryListFragment.kt`, `FavouritesListFragment.kt`, etc.)
   - In `onViewCreated()`:
     ```kotlin
     if (TvModeDetector.isTvMode(requireContext())) {
         recyclerView.enableTvFocus()
     }
     ```

**Estimated Files:** ~10 activities/fragments

---

#### 5.3 Layouts (Add `android:focusable` attributes)

**Files to Modify:**

Most common changes:
```xml
<Button android:focusable="true" android:focusableInTouchMode="false" />
<ImageView android:focusable="true" android:focusableInTouchMode="false" />
```

**Estimated Files:** ~15 layout files (item layouts for RecyclerViews, dialog buttons, bottom sheets)

**Examples:**
- `item_manga.xml`, `item_chapter.xml`, `item_history.xml`
- `sheet_reader_config.xml`, `dialog_favorite.xml`
- Navigation menus: already focusable, may need `nextFocusDown` / `nextFocusUp` hints

---

#### 5.4 Styles and Themes (`res/values/`)

**New Files (in `:tv` module):**
- `tv/res/values/tv_styles.xml`: Focus styles
- `tv/res/values-television/`: TV-specific overrides (larger text, button sizes)

**Existing Files (minimal changes):**
- `app/res/values/themes.xml`: Add TV theme parent:
  ```xml
  <style name="Theme.Futon.Tv" parent="Theme.Futon">
      <item name="colorSecondary">@color/tv_focus_color</item>
  </style>
  ```

---

#### 5.5 Dependency Injection (Hilt)

**Add TV-specific modules (in `:tv`):**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TvModule {
    @Provides
    @Singleton
    fun provideTvKeyHandler(): TvKeyHandler = TvKeyHandler()
}
```

**No changes needed in `app/` Hilt setup** (auto-merged).

---

#### 5.6 Settings

**Changes:**
1. Add "TV Mode" preference category in `SettingsActivity` (only if `TvModeDetector.isTvMode()`)
2. Add TV-specific settings:
   - "Hide touch-specific features" (e.g., pull-to-refresh hints)
   - "Auto-show on-screen keyboard in search"
   - "Enable media button navigation in reader"

**Files:**
- `settings/SettingsActivity.kt`: Add conditional menu item
- `tv/res/xml/tv_preferences.xml`: New preference screen

---

### Summary of Upstream File Changes

| Category               | Files Changed | Lines Added | Lines Modified |
|------------------------|---------------|-------------|----------------|
| Manifest               | 1             | ~10         | 0              |
| Activities/Fragments   | ~10           | ~50         | ~20            |
| Layouts                | ~15           | ~30         | 0 (attributes) |
| Styles/Themes          | ~2            | ~20         | 0              |
| Settings               | ~2            | ~30         | ~10            |
| **Total**              | **~30**       | **~140**    | **~30**        |

**Merge Conflict Risk:** Low. Most changes are additive (`if (isTvMode())` guards). Futon updates are unlikely to touch TV-specific code.

---

## 6. Manifest Requirements for TV

### Required Changes to `app/src/main/AndroidManifest.xml`

```xml
<manifest ...>
    <!-- Declare TV support (optional, for Google Play) -->
    <uses-feature android:name="android.software.leanback" android:required="false" />
    
    <!-- Declare touchscreen not required -->
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
    
    <!-- Optional: Declare gamepad not required (if only D-pad support) -->
    <uses-feature android:name="android.hardware.gamepad" android:required="false" />

    <application
        ...
        android:banner="@drawable/tv_banner">  <!-- 320x180 banner for TV launcher -->

        <!-- Existing MainActivity -->
        <activity android:name=".main.ui.MainActivity" ...>
            <!-- Keep existing LAUNCHER intent for phones/tablets -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- New TV Launcher Activity -->
        <activity
            android:name="io.github.mojillo.futontv.ui.TvLauncherActivity"
            android:exported="true"
            android:theme="@style/Theme.Futon.Tv">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

### Additional Assets

1. **TV Banner:** `app/src/main/res/drawable-xhdpi/tv_banner.png` (320x180 px)
   - Show Futon logo + "Manga Reader for TV" text
   - Follow [Android TV banner guidelines](https://developer.android.com/training/tv/start/start#banner)

2. **Icons:**
   - Existing app icon is fine for TV
   - Ensure round icon (`ic_launcher_round`) exists for TV home screen

---

## 7. Rebranding Steps for `io.github.mojillo.futontv`

**Goal:** Rebrand app so it can be installed alongside the original Futon.

### 7.1 Application ID

**File:** `app/build.gradle`

```groovy
android {
    defaultConfig {
        applicationId 'io.github.mojillo.futontv'  // Changed from io.github.landwarderer.futon
        ...
    }
}
```

### 7.2 App Name

**File:** `app/src/main/res/values/strings.xml`

```xml
<string name="app_name">Futon TV</string>
```

### 7.3 Package Name (Optional, for full rebrand)

**Current:** `io.github.landwarderer.futon`  
**New:** `io.github.mojillo.futontv`

**Tools:**
- Android Studio: Refactor → Rename Package
- **Warning:** This is invasive (1,200+ Kotlin files). Recommend keeping package name as-is and only changing `applicationId`.

### 7.4 Content Providers

**Files to Update:**

1. **`app/src/main/AndroidManifest.xml`**
   ```xml
   <!-- OLD -->
   <provider android:authorities="${applicationId}.MangaSuggestionsProvider" ... />
   <provider android:authorities="${applicationId}.files" ... />
   <provider android:authorities="@string/sync_authority_favourites" ... />
   <provider android:authorities="@string/sync_authority_history" ... />
   
   <!-- NEW (use ${applicationId} auto-replacement, or hardcode if needed) -->
   ```

2. **`app/src/main/res/values/strings.xml`**
   ```xml
   <string name="sync_authority_favourites">io.github.mojillo.futontv.sync.favourites</string>
   <string name="sync_authority_history">io.github.mojillo.futontv.sync.history</string>
   ```

**Impact:** Sync accounts will be separate; users can't migrate data from Futon to Futon TV without export/import.

### 7.5 Deep Links

**Current Deep Links:**
- `futon://manga`
- `https://futon.app/manga`
- `https://futonapp.pages.dev/manga`

**New Deep Links:**
```xml
<data android:scheme="futontv" android:host="manga" />
<!-- Keep external links as-is if you want to claim them, or remove if not -->
```

**Impact:** Deep links from websites/other apps will open Futon, not Futon TV (unless you own the domain and add new paths).

### 7.6 File Paths and Shared Preferences

**Shared Preferences:**
- Stored in `/data/data/{applicationId}/shared_prefs/`
- **Action:** No code changes needed; new app ID = new prefs storage

**Database:**
- Room DB stored in `/data/data/{applicationId}/databases/`
- **Action:** No changes needed; separate app = separate DB

**Local Manga Storage:**
- Currently: User-selected directories (no app-specific path restrictions)
- **Action:** No conflicts; both apps can coexist

### 7.7 Sync Account Types

**File:** `app/src/main/res/xml/authenticator_sync.xml`

```xml
<account-authenticator
    android:accountType="io.github.mojillo.futontv.account"  <!-- Changed -->
    ... />
```

**Impact:** Sync accounts will be separate; can't share cloud sync with original Futon.

### 7.8 Icon and Visual Identity

1. **App Icon:**
   - Keep Futon icon but add "TV" badge in corner (optional)
   - Or: Use different color scheme (e.g., darker blue)

2. **Theme Colors:**
   - Change `colorPrimary`, `colorSecondary` in `res/values/colors.xml` to distinguish from Futon

3. **Banner:**
   - Create unique TV banner with "Futon TV" branding

---

### Rebranding Checklist

| Item                     | File(s)                              | Change                                      |
|--------------------------|--------------------------------------|---------------------------------------------|
| Application ID           | `app/build.gradle`                   | `io.github.mojillo.futontv`                 |
| App Name                 | `res/values/strings.xml`             | "Futon TV"                                  |
| Content Provider Auth    | `AndroidManifest.xml`, `strings.xml` | Use `${applicationId}` or hardcode          |
| Sync Authorities         | `strings.xml`, `authenticator_sync.xml` | `io.github.mojillo.futontv.sync.*`        |
| Deep Links               | `AndroidManifest.xml`                | `futontv://` scheme                         |
| Icons                    | `res/mipmap-*/`                      | Add "TV" badge or new icon                  |
| Theme Colors             | `res/values/colors.xml`              | Change primary/secondary colors             |
| TV Banner                | `res/drawable-xhdpi/`                | 320x180 "Futon TV" banner                   |

**Conflicts with Futon:** None after rebranding. Both apps can coexist. User can migrate manga library via backup/restore.

---

## 8. Phased Implementation Plan

### Phase 1: Core TV Infrastructure (MVP)

**Goal:** Launcher + navigable library + readable reader with D-pad.

**Tasks:**

1. **Setup `:tv` module** (~1 day)
   - Create module structure
   - Add `TvModeDetector`, `FocusHelper`, `TvKeyHandler`
   - Create TV launcher activity + welcome screen

2. **Manifest updates** (~1 day)
   - Add Leanback intent, TV features, banner
   - Test TV launcher on emulator

3. **Reader enhancements** (~3 days)
   - Map long-press CENTER to reader menu
   - Add focus drawables to toolbar/actions bar
   - Test page navigation, zoom controls
   - Ensure UI toggle works smoothly

4. **MainActivity + Library** (~5 days)
   - Add focus drawables to bottom nav / nav rail
   - Enable RecyclerView focus in history, favorites, local
   - Test tab navigation in favorites (LEFT/RIGHT on TabLayout)
   - Ensure focus order: nav bar → content → toolbar

5. **Manga Details** (~3 days)
   - Add focus to tabs, chapter list items, FAB
   - Test chapter selection with D-pad

6. **Testing** (~2 days)
   - End-to-end flow: Launch TV app → browse library → open manga → read chapters
   - Fix focus traps, adjust focus order

**Deliverables:**
- TV-enabled APK
- D-pad navigation works for main screens (library, reader, details)
- Basic focus highlights

**Estimated Effort:** ~15 days (single developer)

---

### Phase 2: Search, Explore, and Advanced Features

**Goal:** Full app navigation, search with on-screen keyboard, downloads.

**Tasks:**

1. **Search** (~3 days)
   - Auto-open keyboard when search bar gains focus
   - Add focus to suggestion list
   - Voice input button

2. **Explore** (~2 days)
   - Focus on source list, manga grid
   - Filter chips navigation (LEFT/RIGHT)

3. **Downloads** (~2 days)
   - Focus on download items, queue controls

4. **Settings** (~2 days)
   - Add "TV Mode" preference category
   - Test preference navigation

5. **Dialogs and Sheets** (~3 days)
   - Focus order in dialogs (default button first)
   - Disable swipe-to-dismiss on TV
   - Test all bottom sheets (config, category picker, etc.)

6. **Testing** (~2 days)
   - Full app coverage with D-pad
   - Ensure no focus dead ends

**Estimated Effort:** ~14 days

---

### Phase 3: Polish and Optimization

**Goal:** Smooth UX, handle edge cases, optimize for 10-foot UI.

**Tasks:**

1. **10-Foot UI Adjustments** (~3 days)
   - Increase text size, button sizes (TV-specific dimens)
   - Larger manga cards for better readability from couch
   - Test on real TV (not just emulator)

2. **Advanced Reader Features** (~3 days)
   - Media button support (PLAY/PAUSE for auto-scroll, FFW/RWD for chapter skip)
   - Channel UP/DOWN for jump 10 pages
   - Page thumbnail grid navigation

3. **Error Handling** (~2 days)
   - Handle "no internet" gracefully (no touch-to-retry, use button)
   - Ensure all errors have focusable retry buttons

4. **Accessibility** (~2 days)
   - Test with TalkBack (voice guidance)
   - Ensure focus order is logical for screen readers

5. **Performance** (~2 days)
   - Optimize image loading for TV (larger screen, more memory)
   - Preload next page in reader for instant navigation

6. **Final Testing** (~3 days)
   - Regression testing on phone/tablet (ensure no breakage)
   - TV-specific QA (all screens, all edge cases)

**Estimated Effort:** ~15 days

---

### Phase 4: Rebranding and Release

**Goal:** Rebrand to Futon TV, publish to Play Store.

**Tasks:**

1. **Rebranding** (~2 days)
   - Change app ID, name, icons, colors
   - Update content providers, sync authorities
   - Test coexistence with original Futon

2. **Documentation** (~1 day)
   - Update README with TV usage instructions
   - Add TV screenshots to Play Store listing

3. **Play Store Submission** (~1 day)
   - Create TV-specific Play Store listing
   - Add TV screenshots, banner
   - Submit for review

**Estimated Effort:** ~4 days

---

### Total Estimated Effort

| Phase          | Effort (Days) | Risk Level |
|----------------|---------------|------------|
| Phase 1 (MVP)  | 15            | Medium     |
| Phase 2        | 14            | Low        |
| Phase 3        | 15            | Medium     |
| Phase 4        | 4             | Low        |
| **Total**      | **48 days**   | -          |

**Parallel Development:** If UI/UX designer handles 10-foot UI adjustments concurrently with Phase 1-2, total time can be reduced to ~40 days.

---

### Merge Risk from Upstream Futon

**Risk Level:** **Low to Medium**

**Reasons:**
1. **Isolated TV Module:** Most TV-specific code is in `:tv`, which doesn't exist in upstream.
2. **Conditional Hooks:** Changes in `app/` are guarded with `if (TvModeDetector.isTvMode())`, so they don't affect phone/tablet behavior.
3. **Additive Manifest Changes:** Leanback intent, TV features, and banner are additions, not modifications.

**Potential Conflicts:**
- **Layout Changes:** If upstream Futon refactors layouts, `android:focusable` attributes may be lost.
  - **Mitigation:** Keep track of modified layouts in `docs/TV_CHANGES.md`, re-apply after merge.
- **Activity Lifecycle Changes:** If upstream changes `onCreate()` significantly, TV focus hooks may need adjustment.
  - **Mitigation:** Use separate `BaseTvActivity` if divergence grows.
- **Reader Refactor:** If Futon rewrites reader to use Compose, TV focus logic will need rewrite.
  - **Mitigation:** Monitor Futon roadmap; consider forking if Compose migration happens.

**Recommended Merge Strategy:**
1. Merge upstream Futon into Futon TV fork monthly.
2. After merge, run automated tests (if any) or manual smoke test on TV.
3. Re-apply TV-specific layout changes if lost.
4. Keep `TV_CHANGES.md` updated with all upstream-touching changes.

---

## 9. Build Status on Current VM

**Tested Command:** `./gradlew assembleDebug`

**Result:** ❌ **Failed**

**Error:**
```
FAILURE: Build failed with an exception.
* What went wrong: Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/local.properties'.
```

**Root Cause:** Android SDK is not installed or not configured in the VM environment.

**Required Setup:**

1. **Install Android SDK:**
   - Download Android SDK Command-line tools from [developer.android.com](https://developer.android.com/studio#command-tools)
   - Extract to `/opt/android-sdk` or `~/android-sdk`

2. **Set `ANDROID_HOME`:**
   ```bash
   export ANDROID_HOME=/opt/android-sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
   ```

3. **Install SDK packages:**
   ```bash
   sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
   ```

4. **Create `local.properties`:**
   ```properties
   sdk.dir=/opt/android-sdk
   ```

5. **Re-run build:**
   ```bash
   ./gradlew assembleDebug
   ```

**Estimated Build Time (after SDK setup):** ~5–10 minutes (first build with Gradle cache population)

**APK Location:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 10. Key Findings Summary

### Strengths (For TV Porting)

1. **XML-based UI:** Easy to add `android:focusable` attributes, no Compose navigation complexity.
2. **Existing D-pad Support:** Reader already handles D-pad, volume buttons, and keyboard shortcuts well.
3. **RecyclerView-heavy:** RecyclerViews are naturally focusable; most lists will work with minimal changes.
4. **Material3:** Material components have built-in focus states; we just need to enhance them.

### Challenges

1. **Minimal Focus Indicators:** No custom focus drawables; system default may be hard to see on large screens.
2. **Gesture-heavy UX:** Swipe gestures (bottom sheets, pull-to-refresh, tab switching) need alternatives.
3. **Touch-centric Dialogs:** Dialogs and bottom sheets need explicit button focus, no swipe-to-dismiss.
4. **Search Input:** On-screen keyboard required for search; voice input is a must-have.
5. **Nested Navigation:** TabLayout + ViewPager2, chips + RecyclerView → focus order must be carefully designed.

### Opportunities

1. **TV-first Launcher:** `TvLauncherActivity` can provide a tailored onboarding experience (explain controls, recommend portrait mode lock).
2. **Media Button Shortcuts:** Map PLAY/PAUSE, FFW/RWD to reader controls for a "lean back" experience.
3. **Leanback Integration:** Future enhancement: Use Leanback `BrowseSupportFragment` for a TV-native browse UI (Phase 5, not in current plan).
4. **Auto-scroll:** Perfect for webtoon reading on TV; "set it and forget it" mode.

---

## 11. References

- [Android TV Design Guidelines](https://developer.android.com/design/ui/tv)
- [Supporting Controllers and Game Controllers](https://developer.android.com/training/game-controllers)
- [Leanback Library](https://developer.android.com/training/tv/playback/browse)
- [Focus and Input Handling](https://developer.android.com/training/tv/start/navigation)
- [Futon Repository](https://github.com/AppFuton/Futon)
- [Kotatsu (Upstream)](https://github.com/KotatsuApp/Kotatsu)

---

## 12. Next Steps

1. **Approve Plan:** Review this document with stakeholders.
2. **Create `:tv` Module:** Set up module structure, add to Gradle build.
3. **Prototype:** Implement Phase 1 tasks on a test branch.
4. **Test on Emulator:** Use Android TV emulator (Android Studio AVD) for initial testing.
5. **Real Device Testing:** Test on Google TV Chromecast or Android TV box before release.
6. **Iterate:** Gather feedback, refine focus order and key mappings.

---

**End of TV Plan**
