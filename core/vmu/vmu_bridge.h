/*
    Flycast DualScreen Experimental
    VMU Bridge - Public Interface
    
    This file is part of Flycast DualScreen Experimental.
    
    Purpose:
    Provides a clean interface between Flycast's internal VMU emulation
    and external consumers (Android JNI layer). This abstraction allows
    the VMU framebuffer to be exposed to Android without modifying the
    core maple device emulation code.
    
    Architecture:
    - VMU emulation in maple_devs.cpp writes to lcd_data_decoded[]
    - push_vmu_screen() copies to vmu_lcd_data[] (RGBA u32 format)
    - VMUBridge provides read-only access to this data for JNI
    - Optional callback mechanism for update notifications
    
    Usage:
    1. Call vmu_bridge::init() at startup
    2. Poll getVmuFramebuffer() from JNI at desired framerate
    3. Use isVmuActive() to check if VMU has valid data
    4. Call vmu_bridge::term() at shutdown
    
    Thread Safety:
    - Read operations are thread-safe (polling model)
    - Write operations (from emulation) happen on emu thread
    - Callbacks are invoked on emulation thread
    
    Author: Flycast DualScreen Team
    License: GPL-2.0
*/
#pragma once

#include "types.h"
#include <functional>

namespace vmu_bridge
{
    // VMU display dimensions (hardware specs)
    constexpr int VMU_LCD_WIDTH = 48;
    constexpr int VMU_LCD_HEIGHT = 32;
    constexpr int VMU_LCD_PIXELS = VMU_LCD_WIDTH * VMU_LCD_HEIGHT;  // 1536 pixels
    
    // Maximum number of VMUs supported
    // 4 controllers × 2 VMU slots per controller = 8 total
    constexpr int MAX_VMU_COUNT = 8;
    
    // VMU button definitions (matching Dreamcast VMU hardware)
    enum class VmuButton : u8 {
        MODE   = 0x01,  // Mode button
        SLEEP  = 0x02,  // Sleep button  
        UP     = 0x04,  // D-pad up
        DOWN   = 0x08,  // D-pad down
        LEFT   = 0x10,  // D-pad left
        RIGHT  = 0x20,  // D-pad right
        A      = 0x40,  // A button
        B      = 0x80   // B button
    };
    
    // Callback type for VMU update notifications
    // Parameters: vmu_id (0-7), framebuffer pointer, timestamp
    using VmuUpdateCallback = std::function<void(int vmu_id, const u32* framebuffer, u64 timestamp)>;
    
    /**
     * Initialize the VMU bridge.
     * Call this once at emulator startup.
     */
    void init();
    
    /**
     * Terminate the VMU bridge.
     * Call this at emulator shutdown.
     */
    void term();
    
    /**
     * Check if the VMU bridge feature is enabled.
     * @return true if VMU bridge is active
     */
    bool isEnabled();
    
    /**
     * Enable or disable the VMU bridge.
     * When disabled, callbacks won't fire and data won't be updated.
     * @param enabled true to enable, false to disable
     */
    void setEnabled(bool enabled);
    
    /**
     * Get the number of connected VMUs.
     * @return Number of VMUs currently connected (0-8)
     */
    int getConnectedVmuCount();
    
    /**
     * Check if a specific VMU slot has an active VMU.
     * @param vmu_id VMU slot (0-7, calculated as bus_id * 2 + bus_port)
     * @return true if VMU is connected and has valid LCD data
     */
    bool isVmuActive(int vmu_id);
    
    /**
     * Check if a VMU's display has been updated recently.
     * @param vmu_id VMU slot (0-7)
     * @return true if the VMU display was updated in the last frame
     */
    bool isVmuDisplayDirty(int vmu_id);
    
    /**
     * Get the framebuffer data for a VMU.
     * Returns a pointer to 1536 u32 values (48×32 pixels, RGBA8888 format).
     * The alpha channel is always 0xFF (fully opaque).
     * 
     * @param vmu_id VMU slot (0-7)
     * @return Pointer to framebuffer data, or nullptr if VMU is not active
     * 
     * Note: The returned pointer is valid until the next frame.
     * Do not store this pointer - copy the data if needed.
     */
    const u32* getVmuFramebuffer(int vmu_id);
    
    /**
     * Get the timestamp of the last VMU display update.
     * @param vmu_id VMU slot (0-7)
     * @return Timestamp in milliseconds, or 0 if never updated
     */
    u64 getVmuLastUpdate(int vmu_id);
    
    /**
     * Copy the VMU framebuffer to a provided buffer.
     * Safer alternative to getVmuFramebuffer() for JNI use.
     * 
     * @param vmu_id VMU slot (0-7)
     * @param dest Destination buffer (must be at least 1536 u32 elements)
     * @return true if copy successful, false if VMU not active
     */
    bool copyVmuFramebuffer(int vmu_id, u32* dest);
    
    /**
     * Register a callback for VMU display updates.
     * The callback will be invoked on the emulation thread whenever
     * any VMU's display is updated.
     * 
     * @param callback Function to call on updates, or nullptr to unregister
     * 
     * Note: Keep callback execution minimal to avoid impacting emulation.
     */
    void registerUpdateCallback(VmuUpdateCallback callback);
    
    /**
     * Send a button press/release to a VMU.
     * Used for standalone VMU functionality (games stored on VMU).
     * 
     * @param vmu_id VMU slot (0-7)
     * @param button Button to press/release
     * @param pressed true for press, false for release
     * 
     * Note: This is for future expansion. Most VMU buttons are not
     * used during normal Dreamcast gameplay.
     */
    void sendVmuButtonState(int vmu_id, VmuButton button, bool pressed);
    
    /**
     * Get the current button state for a VMU.
     * @param vmu_id VMU slot (0-7)
     * @return Bitmask of currently pressed buttons
     */
    u8 getVmuButtonState(int vmu_id);
    
    // Internal functions - called by Flycast core
    namespace internal
    {
        /**
         * Called when a VMU display is updated.
         * This is hooked into push_vmu_screen() in osd.cpp.
         */
        void onVmuDisplayUpdate(int vmu_id, const u32* framebuffer, u64 timestamp);
        
        /**
         * Called when a VMU is connected or disconnected.
         */
        void onVmuConnectionChange(int vmu_id, bool connected);
    }
    
} // namespace vmu_bridge
