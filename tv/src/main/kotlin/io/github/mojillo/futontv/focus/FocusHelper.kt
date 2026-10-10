package io.github.mojillo.futontv.focus

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.mojillo.futontv.R

/**
 * Helper functions for TV focus management.
 */
object FocusHelper {
    
    /**
     * Apply TV-style focus to a view.
     * Adds a focus drawable that shows a stroke when focused.
     */
    fun View.applyTvFocus() {
        isFocusable = true
        isFocusableInTouchMode = false
        
        // Wrap existing background with focus drawable
        background = createFocusDrawable(context, background)
    }
    
    /**
     * Enable TV focus for RecyclerView and its children.
     */
    fun RecyclerView.enableTvFocus() {
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        isFocusable = false
        isFocusableInTouchMode = false
    }
    
    /**
     * Create a state list drawable that shows focus overlay.
     */
    private fun createFocusDrawable(context: Context, originalBackground: Drawable?): Drawable {
        return StateListDrawable().apply {
            // Focused state: show stroke overlay
            addState(
                intArrayOf(android.R.attr.state_focused),
                LayerDrawable(
                    arrayOf(
                        originalBackground,
                        ContextCompat.getDrawable(context, R.drawable.tv_focus_overlay)
                    ).filterNotNull().toTypedArray()
                )
            )
            // Default state: original background
            addState(intArrayOf(), originalBackground)
        }
    }
}
