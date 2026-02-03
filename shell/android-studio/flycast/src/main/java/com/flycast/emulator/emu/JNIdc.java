package com.flycast.emulator.emu;

import android.view.Surface;

import com.flycast.emulator.Emulator;
import com.flycast.emulator.periph.SipEmulator;

public final class JNIdc
{
	static { System.loadLibrary("flycast"); }

	public static native String initEnvironment(Emulator emulator, String filesDirectory, String homeDirectory, String locale);
	public static native void setExternalStorageDirectories(Object[] pathList);
	public static native void setGameUri(String fileName);
	public static native void pause();
	public static native void resume();
	public static native void stop();
	public static native void disableOmpAffinity();

	public static native void rendinitNative(Surface surface, int w, int h);

	public static native void setupMic(SipEmulator sip);

	public static native void screenCharacteristics(float screenDpi, float refreshRate);
	public static native void guiOpenSettings();
	public static native boolean guiIsOpen();
	public static native boolean guiIsContentBrowser();
	public static native void guiSetInsets(int left, int right, int top, int bottom);

	// ============================================================
	// VMU Second-Screen Support (Flycast DualScreen Experimental)
	// ============================================================

	/**
	 * Initialize the VMU bridge for second-screen support.
	 * Call this once at startup.
	 */
	public static native void vmuBridgeInit();

	/**
	 * Terminate the VMU bridge.
	 * Call this at shutdown.
	 */
	public static native void vmuBridgeTerm();

	/**
	 * Check if the VMU bridge is enabled.
	 * @return true if VMU bridge is active
	 */
	public static native boolean vmuBridgeIsEnabled();

	/**
	 * Enable or disable the VMU bridge.
	 * @param enabled true to enable, false to disable
	 */
	public static native void vmuBridgeSetEnabled(boolean enabled);

	/**
	 * Get the number of connected VMUs.
	 * @return Number of VMUs currently connected (0-8)
	 */
	public static native int vmuGetConnectedCount();

	/**
	 * Check if a specific VMU slot has an active VMU.
	 * @param vmuId VMU slot (0-7, calculated as busId * 2 + busPort)
	 * @return true if VMU is connected and has valid LCD data
	 */
	public static native boolean vmuIsActive(int vmuId);

	/**
	 * Check if a VMU's display has been updated since last check.
	 * @param vmuId VMU slot (0-7)
	 * @return true if the VMU display was updated
	 */
	public static native boolean vmuIsDisplayDirty(int vmuId);

	/**
	 * Get the framebuffer data for a VMU.
	 * Returns an array of 1536 integers (48×32 pixels, ARGB8888 format).
	 * 
	 * @param vmuId VMU slot (0-7)
	 * @return Framebuffer data array, or null if VMU is not active
	 */
	public static native int[] vmuGetFramebuffer(int vmuId);

	/**
	 * Copy the VMU framebuffer to a provided buffer.
	 * More efficient than vmuGetFramebuffer() if called frequently.
	 * 
	 * @param vmuId VMU slot (0-7)
	 * @param dest Destination buffer (must be at least 1536 integers)
	 * @return true if copy successful, false if VMU not active
	 */
	public static native boolean vmuCopyFramebuffer(int vmuId, int[] dest);

	/**
	 * Get the timestamp of the last VMU display update.
	 * @param vmuId VMU slot (0-7)
	 * @return Timestamp in milliseconds, or 0 if never updated
	 */
	public static native long vmuGetLastUpdate(int vmuId);

	/**
	 * Send a button press/release to a VMU.
	 * Used for standalone VMU functionality.
	 * 
	 * @param vmuId VMU slot (0-7)
	 * @param button Button code (see VmuButton constants)
	 * @param pressed true for press, false for release
	 */
	public static native void vmuSendButton(int vmuId, int button, boolean pressed);

	/**
	 * VMU button constants (matching hardware)
	 */
	public static final int VMU_BUTTON_MODE  = 0x01;
	public static final int VMU_BUTTON_SLEEP = 0x02;
	public static final int VMU_BUTTON_UP    = 0x04;
	public static final int VMU_BUTTON_DOWN  = 0x08;
	public static final int VMU_BUTTON_LEFT  = 0x10;
	public static final int VMU_BUTTON_RIGHT = 0x20;
	public static final int VMU_BUTTON_A     = 0x40;
	public static final int VMU_BUTTON_B     = 0x80;

	/**
	 * VMU display dimensions (hardware specs)
	 */
	public static final int VMU_LCD_WIDTH  = 48;
	public static final int VMU_LCD_HEIGHT = 32;
	public static final int VMU_LCD_PIXELS = VMU_LCD_WIDTH * VMU_LCD_HEIGHT;  // 1536

}
