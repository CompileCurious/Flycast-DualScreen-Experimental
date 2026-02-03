/*
 * Flycast DualScreen Experimental
 * VMU Display Manager
 *
 * Manages the lifecycle of VMU display views and handles secondary display
 * detection for devices like the AYN Thor.
 *
 * Features:
 * - Automatic secondary display detection
 * - VMU display on main screen overlay
 * - VMU display on secondary screen (Presentation API)
 * - Configuration persistence
 *
 * Author: Flycast DualScreen Team
 * License: GPL-2.0
 */
package com.flycast.emulator.vmu

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.flycast.emulator.emu.JNIdc
import java.lang.ref.WeakReference

/**
 * Manages VMU display instances and secondary display support.
 *
 * Usage:
 * ```kotlin
 * val manager = VmuDisplayManager(activity)
 * manager.config = VmuConfig(enabled = true)
 * manager.start()
 * // ... during gameplay ...
 * manager.stop()
 * manager.release()
 * ```
 */
class VmuDisplayManager(activity: Activity) {

    companion object {
        private const val TAG = "VmuDisplayManager"

        // Singleton instance (optional, for global access)
        private var instance: VmuDisplayManager? = null

        fun getInstance(activity: Activity): VmuDisplayManager {
            return instance ?: VmuDisplayManager(activity).also { instance = it }
        }

        fun releaseInstance() {
            instance?.release()
            instance = null
        }
    }

    private val activityRef = WeakReference(activity)
    private val context: Context = activity.applicationContext

    /**
     * Configuration for VMU display.
     */
    var config: VmuConfig = VmuConfig()
        set(value) {
            field = value
            applyConfig()
        }

    // Display manager for secondary display detection
    private val displayManager: DisplayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    // VMU display view for main screen overlay
    private var mainScreenView: VmuDisplayView? = null

    // Presentation for secondary display
    private var secondaryPresentation: VmuPresentation? = null

    // Currently active secondary display
    private var secondaryDisplay: Display? = null

    // Display callback for detecting display changes
    private val displayCallback = object : DisplayManager.DisplayCallback() {
        override fun onDisplayAdded(displayId: Int) {
            Log.d(TAG, "Display added: $displayId")
            checkSecondaryDisplays()
        }

        override fun onDisplayRemoved(displayId: Int) {
            Log.d(TAG, "Display removed: $displayId")
            if (secondaryDisplay?.displayId == displayId) {
                dismissSecondaryPresentation()
                checkSecondaryDisplays()
            }
        }

        override fun onDisplayChanged(displayId: Int) {
            Log.d(TAG, "Display changed: $displayId")
        }
    }

    // Listener for VMU events
    private val vmuListener = object : VmuDisplayView.VmuDisplayListener {
        override fun onVmuConnected(vmuId: Int) {
            Log.d(TAG, "VMU connected: $vmuId")
        }

        override fun onVmuDisconnected() {
            Log.d(TAG, "VMU disconnected")
        }

        override fun onVmuDisplayUpdated(vmuId: Int) {
            // Optional: log or handle update events
        }
    }

    private var isRunning = false

    init {
        // Initialize VMU bridge
        JNIdc.vmuBridgeInit()

        // Register display callback
        displayManager.registerDisplayCallback(displayCallback, null)

        Log.d(TAG, "VmuDisplayManager initialized")
    }

    /**
     * Start VMU display rendering.
     */
    fun start() {
        if (isRunning) return

        isRunning = true
        JNIdc.vmuBridgeSetEnabled(true)

        if (config.enabled) {
            if (config.useSecondaryDisplay) {
                showOnSecondaryDisplay()
            } else {
                showOnMainScreen()
            }
        }

        Log.d(TAG, "VMU display started")
    }

    /**
     * Stop VMU display rendering.
     */
    fun stop() {
        if (!isRunning) return

        isRunning = false

        hideFromMainScreen()
        dismissSecondaryPresentation()

        JNIdc.vmuBridgeSetEnabled(false)

        Log.d(TAG, "VMU display stopped")
    }

    /**
     * Release all resources. Call when the manager is no longer needed.
     */
    fun release() {
        stop()

        displayManager.unregisterDisplayCallback(displayCallback)

        JNIdc.vmuBridgeTerm()

        Log.d(TAG, "VmuDisplayManager released")
    }

    /**
     * Get a list of available secondary displays.
     */
    fun getSecondaryDisplays(): List<Display> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).toList()
        } else {
            displayManager.displays.filter { it.displayId != Display.DEFAULT_DISPLAY }
        }
    }

    /**
     * Check if a secondary display is available.
     */
    fun hasSecondaryDisplay(): Boolean {
        return getSecondaryDisplays().isNotEmpty()
    }

    /**
     * Show VMU display on the main screen as an overlay.
     */
    private fun showOnMainScreen() {
        val activity = activityRef.get() ?: return

        if (mainScreenView != null) return

        mainScreenView = VmuDisplayView(activity).apply {
            this.config = this@VmuDisplayManager.config
            listener = vmuListener
        }

        // Find the root view and add the VMU display
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        val params = FrameLayout.LayoutParams(
            config.scaledWidth,
            config.scaledHeight
        ).apply {
            // Position in bottom-right corner with margin
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
            marginEnd = 16
            bottomMargin = 16
        }

        mainScreenView?.let {
            rootView.addView(it, params)
            it.start()
        }

        Log.d(TAG, "VMU display shown on main screen")
    }

    /**
     * Hide VMU display from the main screen.
     */
    private fun hideFromMainScreen() {
        mainScreenView?.let { view ->
            view.stop()
            (view.parent as? ViewGroup)?.removeView(view)
        }
        mainScreenView = null

        Log.d(TAG, "VMU display hidden from main screen")
    }

    /**
     * Show VMU display on the secondary display.
     */
    private fun showOnSecondaryDisplay() {
        val activity = activityRef.get() ?: return
        val displays = getSecondaryDisplays()

        if (displays.isEmpty()) {
            Log.w(TAG, "No secondary display available, falling back to main screen")
            showOnMainScreen()
            return
        }

        val display = displays.first()
        secondaryDisplay = display

        secondaryPresentation = VmuPresentation(activity, display, config).apply {
            vmuListener = this@VmuDisplayManager.vmuListener
            show()
        }

        Log.d(TAG, "VMU display shown on secondary display: ${display.name}")
    }

    /**
     * Dismiss the secondary display presentation.
     */
    private fun dismissSecondaryPresentation() {
        secondaryPresentation?.dismiss()
        secondaryPresentation = null
        secondaryDisplay = null

        Log.d(TAG, "Secondary presentation dismissed")
    }

    /**
     * Check for secondary displays and update accordingly.
     */
    private fun checkSecondaryDisplays() {
        if (!isRunning || !config.enabled) return

        if (config.useSecondaryDisplay) {
            if (secondaryPresentation == null && hasSecondaryDisplay()) {
                hideFromMainScreen()
                showOnSecondaryDisplay()
            } else if (secondaryPresentation != null && !hasSecondaryDisplay()) {
                dismissSecondaryPresentation()
                showOnMainScreen()
            }
        }
    }

    /**
     * Apply the current configuration.
     */
    private fun applyConfig() {
        if (!isRunning) return

        // Update main screen view if visible
        mainScreenView?.config = config

        // Handle display mode change
        if (config.useSecondaryDisplay && mainScreenView != null) {
            hideFromMainScreen()
            showOnSecondaryDisplay()
        } else if (!config.useSecondaryDisplay && secondaryPresentation != null) {
            dismissSecondaryPresentation()
            showOnMainScreen()
        }

        // Enable/disable based on config
        if (config.enabled) {
            JNIdc.vmuBridgeSetEnabled(true)
            if (config.useSecondaryDisplay) {
                if (secondaryPresentation == null) showOnSecondaryDisplay()
            } else {
                if (mainScreenView == null) showOnMainScreen()
            }
        } else {
            hideFromMainScreen()
            dismissSecondaryPresentation()
        }
    }
}
