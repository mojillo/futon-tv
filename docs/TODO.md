# Futon TV - TODO List

Running checklist of improvements and issues to track. Will be converted to GitHub issues later.

---

## 🔧 High Priority

- [ ] **Sync/Login is hard to find**  
  The sync/login option only shows up on the first-launch welcome screen and can't be found again later (noticed on tablet). Users who skip it or dismiss the welcome screen have no obvious way to access sync settings.
  
  **What to do:**
  - Make sync/login reachable from the main Settings screen (e.g., a "Sync & Backup" section)
  - Verify it's D-pad accessible on TV (focus order, keyboard navigation)
  - Consider adding a "Sign in" option to the account/profile area if one exists
  
  **Code locations:**
  - Welcome screen sync button: `app/src/main/kotlin/io/github/landwarderer/futon/main/ui/welcome/WelcomeSheet.kt` (line ~58: `binding.chipSync.setOnClickListener`)
  - Sync auth activity: `app/src/main/kotlin/io/github/landwarderer/futon/sync/ui/SyncAuthActivity.kt`
  - Sync preference header (deep link): `app/src/main/res/xml/pref_sync_header.xml` (intent: `futon://sync-settings`)
  - Main settings: `app/src/main/kotlin/io/github/landwarderer/futon/settings/SettingsActivity.kt`

---

## 📋 Backlog

_(Add items here as they come up)_

---

## ✅ Completed

_(Move finished items here with completion date)_
