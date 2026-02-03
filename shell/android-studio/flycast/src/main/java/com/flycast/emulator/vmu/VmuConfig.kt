/*
 * Flycast DualScreen Experimental
 * VMU Display Configuration
 *
 * Data class for VMU display settings. Used to configure the appearance
 * and behavior of the VMU display view.
 *
 * Author: Flycast DualScreen Team
 * License: GPL-2.0
 */
package com.flycast.emulator.vmu

import android.graphics.Color

/**
 * Configuration options for VMU display rendering.
 */
data class VmuConfig(
    /**
     * Whether VMU display is enabled.
     */
    var enabled: Boolean = false,

    /**
     * Which VMU slot to display (0-7, or -1 for auto-select first active).
     */
    var selectedVmuId: Int = -1,

    /**
     * Color for "on" pixels (classic VMU uses green).
     */
    var pixelOnColor: Int = Color.parseColor("#00FF00"),

    /**
     * Color for "off" pixels (classic VMU uses dark green).
     */
    var pixelOffColor: Int = Color.parseColor("#001100"),

    /**
     * Display scale factor (1.0 = native 48x32, 4.0 = 192x128).
     */
    var scaleFactor: Float = 4.0f,

    /**
     * Target frame rate for display updates (frames per second).
     */
    var targetFps: Int = 30,

    /**
     * Whether to show on secondary display (for AYN Thor).
     */
    var useSecondaryDisplay: Boolean = false,

    /**
     * Whether to use nearest-neighbor filtering (true) or bilinear (false).
     */
    var useNearestNeighbor: Boolean = true,

    /**
     * Show VMU button overlay for touch input.
     */
    var showButtonOverlay: Boolean = false
) {
    companion object {
        // Preset color schemes
        val CLASSIC_GREEN = VmuConfig(
            pixelOnColor = Color.parseColor("#00FF00"),
            pixelOffColor = Color.parseColor("#001100")
        )

        val CLASSIC_GRAY = VmuConfig(
            pixelOnColor = Color.parseColor("#000000"),
            pixelOffColor = Color.parseColor("#8B9B8B")
        )

        val HIGH_CONTRAST = VmuConfig(
            pixelOnColor = Color.parseColor("#FFFFFF"),
            pixelOffColor = Color.parseColor("#000000")
        )

        val AMBER = VmuConfig(
            pixelOnColor = Color.parseColor("#FFBF00"),
            pixelOffColor = Color.parseColor("#1A0F00")
        )

        val BLUE = VmuConfig(
            pixelOnColor = Color.parseColor("#00BFFF"),
            pixelOffColor = Color.parseColor("#000F1A")
        )
    }

    /**
     * Get the scaled width in pixels.
     */
    val scaledWidth: Int
        get() = (VMU_LCD_WIDTH * scaleFactor).toInt()

    /**
     * Get the scaled height in pixels.
     */
    val scaledHeight: Int
        get() = (VMU_LCD_HEIGHT * scaleFactor).toInt()

    /**
     * Get the update interval in milliseconds based on target FPS.
     */
    val updateIntervalMs: Long
        get() = (1000L / targetFps)
}

// VMU hardware constants
const val VMU_LCD_WIDTH = 48
const val VMU_LCD_HEIGHT = 32
const val VMU_LCD_PIXELS = VMU_LCD_WIDTH * VMU_LCD_HEIGHT
const val VMU_MAX_COUNT = 8
