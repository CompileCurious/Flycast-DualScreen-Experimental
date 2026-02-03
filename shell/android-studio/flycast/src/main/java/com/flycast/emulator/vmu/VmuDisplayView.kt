/*
 * Flycast DualScreen Experimental
 * VMU Display View
 *
 * Custom View that renders the VMU LCD display. This view polls the native
 * VMU framebuffer and renders it as a scaled bitmap.
 *
 * Features:
 * - Real-time VMU LCD rendering
 * - Configurable colors and scaling
 * - Optional touch input for VMU buttons
 * - Efficient rendering using Bitmap and Canvas
 *
 * Author: Flycast DualScreen Team
 * License: GPL-2.0
 */
package com.flycast.emulator.vmu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.flycast.emulator.emu.JNIdc

/**
 * Custom View that renders a VMU LCD display.
 *
 * Usage:
 * ```kotlin
 * val vmuView = VmuDisplayView(context)
 * vmuView.config = VmuConfig(enabled = true, selectedVmuId = 0)
 * vmuView.start()
 * // ... later ...
 * vmuView.stop()
 * ```
 */
class VmuDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Configuration for this VMU display.
     */
    var config: VmuConfig = VmuConfig()
        set(value) {
            field = value
            updateBitmapColors()
            requestLayout()
            invalidate()
        }

    // Bitmap for rendering VMU display
    private var vmulcdBitmap: Bitmap? = null

    // Pixel buffer for native data
    private val pixelBuffer = IntArray(VMU_LCD_PIXELS)

    // Color-mapped pixel buffer for bitmap
    private val colorBuffer = IntArray(VMU_LCD_PIXELS)

    // Paint for drawing the bitmap
    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false  // Use nearest-neighbor by default
        isDither = false
    }

    // Source and destination rectangles for drawing
    private val srcRect = Rect(0, 0, VMU_LCD_WIDTH, VMU_LCD_HEIGHT)
    private val dstRect = Rect()

    // Handler for update loop
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var isRunning = false

    // Currently displayed VMU ID (resolved from config)
    private var activeVmuId: Int = -1

    // Listener for VMU events
    var listener: VmuDisplayListener? = null

    init {
        // Initialize bitmap
        vmulcdBitmap = Bitmap.createBitmap(VMU_LCD_WIDTH, VMU_LCD_HEIGHT, Bitmap.Config.ARGB_8888)

        // Set default background
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = config.scaledWidth
        val desiredHeight = config.scaledHeight

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        dstRect.set(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        vmulcdBitmap?.let { bitmap ->
            // Set filtering based on config
            bitmapPaint.isFilterBitmap = !config.useNearestNeighbor

            // Draw the VMU bitmap scaled to fill the view
            canvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint)
        }
    }

    /**
     * Start the VMU display update loop.
     */
    fun start() {
        if (isRunning) return

        isRunning = true
        scheduleUpdate()
    }

    /**
     * Stop the VMU display update loop.
     */
    fun stop() {
        isRunning = false
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }

    /**
     * Force an immediate update of the VMU display.
     */
    fun forceUpdate() {
        updateVmuDisplay()
    }

    private fun scheduleUpdate() {
        if (!isRunning) return

        updateRunnable = Runnable {
            updateVmuDisplay()
            if (isRunning) {
                scheduleUpdate()
            }
        }
        handler.postDelayed(updateRunnable!!, config.updateIntervalMs)
    }

    private fun updateVmuDisplay() {
        if (!config.enabled) return

        // Resolve which VMU to display
        val vmuId = resolveVmuId()
        if (vmuId < 0) {
            // No active VMU
            if (activeVmuId >= 0) {
                activeVmuId = -1
                listener?.onVmuDisconnected()
                clearDisplay()
            }
            return
        }

        // Check if VMU changed
        if (vmuId != activeVmuId) {
            activeVmuId = vmuId
            listener?.onVmuConnected(vmuId)
        }

        // Check if display needs update
        if (!JNIdc.vmuIsDisplayDirty(vmuId)) {
            return  // No changes since last update
        }

        // Copy framebuffer from native
        if (!JNIdc.vmuCopyFramebuffer(vmuId, pixelBuffer)) {
            return  // Copy failed
        }

        // Map colors
        mapColors()

        // Update bitmap
        vmulcdBitmap?.setPixels(colorBuffer, 0, VMU_LCD_WIDTH, 0, 0, VMU_LCD_WIDTH, VMU_LCD_HEIGHT)

        // Trigger redraw
        invalidate()

        listener?.onVmuDisplayUpdated(vmuId)
    }

    private fun resolveVmuId(): Int {
        val selectedId = config.selectedVmuId

        if (selectedId >= 0 && selectedId < VMU_MAX_COUNT) {
            // Specific VMU selected
            return if (JNIdc.vmuIsActive(selectedId)) selectedId else -1
        }

        // Auto-select: find first active VMU
        for (i in 0 until VMU_MAX_COUNT) {
            if (JNIdc.vmuIsActive(i)) {
                return i
            }
        }

        return -1  // No active VMU
    }

    private fun mapColors() {
        val onColor = config.pixelOnColor
        val offColor = config.pixelOffColor

        for (i in 0 until VMU_LCD_PIXELS) {
            // Native format: 0xFFFFFFFF = on (white), 0xFF000000 = off (black)
            // We map based on whether the pixel is "bright" (on) or "dark" (off)
            val pixel = pixelBuffer[i]
            val brightness = (pixel and 0xFF) + ((pixel shr 8) and 0xFF) + ((pixel shr 16) and 0xFF)
            colorBuffer[i] = if (brightness > 384) onColor else offColor
        }
    }

    private fun updateBitmapColors() {
        // If we have existing data, re-map colors
        if (activeVmuId >= 0) {
            mapColors()
            vmulcdBitmap?.setPixels(colorBuffer, 0, VMU_LCD_WIDTH, 0, 0, VMU_LCD_WIDTH, VMU_LCD_HEIGHT)
            invalidate()
        }
    }

    private fun clearDisplay() {
        // Fill with "off" color
        colorBuffer.fill(config.pixelOffColor)
        vmulcdBitmap?.setPixels(colorBuffer, 0, VMU_LCD_WIDTH, 0, 0, VMU_LCD_WIDTH, VMU_LCD_HEIGHT)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!config.showButtonOverlay) {
            return super.onTouchEvent(event)
        }

        // Handle VMU button touches
        // This is a simplified implementation - a full implementation would
        // have proper button regions defined
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                handleButtonTouch(event.x, event.y, true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handleButtonTouch(event.x, event.y, false)
            }
        }

        return true
    }

    private fun handleButtonTouch(x: Float, y: Float, pressed: Boolean) {
        if (activeVmuId < 0) return

        // Simple 3x3 grid for D-pad + A/B
        // This is a placeholder - a full implementation would have proper button regions
        val w = width.toFloat()
        val h = height.toFloat()

        val col = (x / w * 3).toInt()
        val row = (y / h * 3).toInt()

        val button = when {
            col == 1 && row == 0 -> JNIdc.VMU_BUTTON_UP
            col == 1 && row == 2 -> JNIdc.VMU_BUTTON_DOWN
            col == 0 && row == 1 -> JNIdc.VMU_BUTTON_LEFT
            col == 2 && row == 1 -> JNIdc.VMU_BUTTON_RIGHT
            col == 0 && row == 2 -> JNIdc.VMU_BUTTON_A
            col == 2 && row == 2 -> JNIdc.VMU_BUTTON_B
            else -> return
        }

        JNIdc.vmuSendButton(activeVmuId, button, pressed)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
        vmulcdBitmap?.recycle()
        vmulcdBitmap = null
    }

    /**
     * Listener interface for VMU display events.
     */
    interface VmuDisplayListener {
        fun onVmuConnected(vmuId: Int)
        fun onVmuDisconnected()
        fun onVmuDisplayUpdated(vmuId: Int)
    }
}
