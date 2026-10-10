package io.github.mojillo.futontv

import android.content.Context
import android.content.res.Configuration

/**
 * Detects if the app is running on Android TV / Google TV.
 */
object TvModeDetector {
    
    /**
     * Returns true if the app is running on a TV device.
     */
    fun isTvMode(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
