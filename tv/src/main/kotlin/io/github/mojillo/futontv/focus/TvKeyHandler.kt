package io.github.mojillo.futontv.focus

import android.view.KeyEvent

/**
 * Global key event handler for TV-specific shortcuts.
 */
class TvKeyHandler {
    
    /**
     * Handle a key event globally.
     * @return true if the event was handled, false otherwise.
     */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        // Future: Add global TV shortcuts here
        // For now, let activities handle their own key events
        return false
    }
}
