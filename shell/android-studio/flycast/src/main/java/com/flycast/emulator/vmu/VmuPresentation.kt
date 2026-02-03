/*
 * Flycast DualScreen Experimental
 * VMU Presentation
 *
 * Presentation class for displaying VMU on a secondary screen.
 * Uses Android's Presentation API to render on external displays
 * like the AYN Thor's companion screen.
 *
 * Features:
 * - Full-screen VMU display on secondary screen
 * - Automatic scaling to fill the display
 * - Same color customization as main screen
 *
 * Author: Flycast DualScreen Team
 * License: GPL-2.0
 */
package com.flycast.emulator.vmu

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Presentation for displaying VMU on a secondary screen.
 *
 * This uses Android's Presentation API to render on external displays.
 * The AYN Thor's secondary display will be detected as a presentation display.
 *
 * @param context The activity context
 * @param display The secondary display to render on
 * @param config VMU display configuration
 */
class VmuPresentation(
    context: Context,
    display: Display,
    private var config: VmuConfig
) : Presentation(context, display) {

    private lateinit var vmuView: VmuDisplayView
    private lateinit var rootLayout: FrameLayout

    /**
     * Listener for VMU events. Set this before calling show().
     */
    var vmuListener: VmuDisplayView.VmuDisplayListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window for full-screen display
        window?.apply {
            setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)
            setBackgroundDrawableResource(android.R.color.black)

            // Make it full-screen
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Create root layout
        rootLayout = FrameLayout(context).apply {
            setBackgroundColor(config.pixelOffColor)
        }

        // Create VMU display view
        vmuView = VmuDisplayView(context).apply {
            this.config = this@VmuPresentation.config.copy(
                // Override scale to fit the secondary display
                scaleFactor = calculateOptimalScale()
            )
            listener = vmuListener
        }

        // Center the VMU display in the presentation
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        rootLayout.addView(vmuView, params)
        setContentView(rootLayout)
    }

    override fun onStart() {
        super.onStart()
        vmuView.listener = vmuListener
        vmuView.start()
    }

    override fun onStop() {
        super.onStop()
        vmuView.stop()
    }

    /**
     * Update the VMU display configuration.
     */
    fun updateConfig(newConfig: VmuConfig) {
        config = newConfig
        vmuView.config = newConfig.copy(scaleFactor = calculateOptimalScale())
        rootLayout.setBackgroundColor(newConfig.pixelOffColor)
    }

    /**
     * Calculate the optimal scale factor to fill the secondary display
     * while maintaining the VMU's aspect ratio.
     */
    private fun calculateOptimalScale(): Float {
        val displayMetrics = display.let { display ->
            android.util.DisplayMetrics().also { display.getMetrics(it) }
        }

        val displayWidth = displayMetrics.widthPixels
        val displayHeight = displayMetrics.heightPixels

        // Calculate scale to fit while maintaining aspect ratio
        // Leave some margin (90% of display)
        val maxWidth = displayWidth * 0.9f
        val maxHeight = displayHeight * 0.9f

        val scaleX = maxWidth / VMU_LCD_WIDTH
        val scaleY = maxHeight / VMU_LCD_HEIGHT

        // Use the smaller scale to maintain aspect ratio
        val scale = minOf(scaleX, scaleY)

        // Round to nearest integer for pixel-perfect rendering
        return scale.toInt().toFloat().coerceAtLeast(1f)
    }

    /**
     * Get the display information for debugging.
     */
    fun getDisplayInfo(): String {
        val displayMetrics = display.let { display ->
            android.util.DisplayMetrics().also { display.getMetrics(it) }
        }

        return "Display: ${display.name} (${display.displayId})\n" +
                "Size: ${displayMetrics.widthPixels}x${displayMetrics.heightPixels}\n" +
                "Density: ${displayMetrics.densityDpi}dpi\n" +
                "Scale: ${calculateOptimalScale()}x"
    }
}
