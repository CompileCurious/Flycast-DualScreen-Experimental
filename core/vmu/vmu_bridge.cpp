/*
    Flycast DualScreen Experimental
    VMU Bridge - Implementation
    
    This file is part of Flycast DualScreen Experimental.
    
    Purpose:
    Implements the VMU bridge layer that exposes VMU display data
    to external consumers like the Android JNI layer.
    
    Implementation Notes:
    - Uses atomic operations for thread safety where needed
    - Maintains local copy of framebuffer for safe JNI access
    - Callback invocation is synchronous (on emu thread)
    - Dirty flags allow efficient polling from JNI
    
    Author: Flycast DualScreen Team
    License: GPL-2.0
*/

#include "vmu_bridge.h"
#include "rend/osd.h"    // For vmu_lcd_data, vmu_lcd_status, vmuLastChanged
#include "cfg/option.h"
#include "log/Log.h"

#include <atomic>
#include <mutex>
#include <cstring>

namespace vmu_bridge
{
    // Internal state
    namespace 
    {
        // Feature enable flag
        std::atomic<bool> g_enabled{false};
        
        // Dirty flags - set when VMU display updates
        std::atomic<bool> g_dirty[MAX_VMU_COUNT] = {};
        
        // Connection status
        std::atomic<bool> g_connected[MAX_VMU_COUNT] = {};
        
        // Button states for each VMU
        std::atomic<u8> g_buttonState[MAX_VMU_COUNT] = {};
        
        // Local framebuffer copies for safe JNI access
        u32 g_framebufferCopy[MAX_VMU_COUNT][VMU_LCD_PIXELS];
        std::mutex g_framebufferMutex[MAX_VMU_COUNT];
        
        // Last update timestamps
        std::atomic<u64> g_lastUpdate[MAX_VMU_COUNT] = {};
        
        // Update callback
        VmuUpdateCallback g_updateCallback;
        std::mutex g_callbackMutex;
        
        // Initialized flag
        std::atomic<bool> g_initialized{false};
    }
    
    void init()
    {
        if (g_initialized.exchange(true))
            return; // Already initialized
            
        INFO_LOG(MAPLE, "VMU Bridge: Initializing");
        
        // Clear all state
        for (int i = 0; i < MAX_VMU_COUNT; i++)
        {
            g_dirty[i] = false;
            g_connected[i] = false;
            g_buttonState[i] = 0;
            g_lastUpdate[i] = 0;
            std::memset(g_framebufferCopy[i], 0, sizeof(g_framebufferCopy[i]));
        }
        
        g_enabled = true;
        INFO_LOG(MAPLE, "VMU Bridge: Initialized successfully");
    }
    
    void term()
    {
        if (!g_initialized.exchange(false))
            return; // Not initialized
            
        INFO_LOG(MAPLE, "VMU Bridge: Terminating");
        
        g_enabled = false;
        
        // Clear callback
        {
            std::lock_guard<std::mutex> lock(g_callbackMutex);
            g_updateCallback = nullptr;
        }
        
        INFO_LOG(MAPLE, "VMU Bridge: Terminated");
    }
    
    bool isEnabled()
    {
        return g_initialized && g_enabled;
    }
    
    void setEnabled(bool enabled)
    {
        g_enabled = enabled;
        DEBUG_LOG(MAPLE, "VMU Bridge: %s", enabled ? "Enabled" : "Disabled");
    }
    
    int getConnectedVmuCount()
    {
        int count = 0;
        for (int i = 0; i < MAX_VMU_COUNT; i++)
        {
            if (g_connected[i])
                count++;
        }
        return count;
    }
    
    bool isVmuActive(int vmu_id)
    {
        if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT)
            return false;
            
        // Check both our tracking and the original status array
        return g_connected[vmu_id] || vmu_lcd_status[vmu_id];
    }
    
    bool isVmuDisplayDirty(int vmu_id)
    {
        if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT)
            return false;
            
        return g_dirty[vmu_id].exchange(false);
    }
    
    const u32* getVmuFramebuffer(int vmu_id)
    {
        if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT)
            return nullptr;
            
        if (!isVmuActive(vmu_id))
            return nullptr;
            
        // Return the original data directly (for efficient access)
        // Caller must not store this pointer
        return vmu_lcd_data[vmu_id];
    }
    
    u64 getVmuLastUpdate(int vmu_id)
    {
        if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT)
            return 0;
            
        return g_lastUpdate[vmu_id];
    }
    
    bool copyVmuFramebuffer(int vmu_id, u32* dest)
    {
        if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT || dest == nullptr)
            return false;
            
        if (!isVmuActive(vmu_id))
            return false;
            
        // Copy from our local buffer (thread-safe)
        std::lock_guard<std::mutex> lock(g_framebufferMutex[vmu_id]);
        std::memcpy(dest, g_framebufferCopy[vmu_id], sizeof(g_framebufferCopy[vmu_id]));
        return true;
    }
    
    void registerUpdateCallback(VmuUpdateCallback callback)
    {
        std::lock_guard<std::mutex> lock(g_callbackMutex);
        g_updateCallback = std::move(callback);
        DEBUG_LOG(MAPLE, "VMU Bridge: Update callback %s", 
                  callback ? "registered" : "unregistered");
    }
    
    void sendVmuButtonState(int vmu_id, VmuButton button, bool pressed)
    {
        if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT)
            return;
            
        u8 mask = static_cast<u8>(button);
        if (pressed)
            g_buttonState[vmu_id] |= mask;
        else
            g_buttonState[vmu_id] &= ~mask;
            
        DEBUG_LOG(MAPLE, "VMU Bridge: VMU %d button %02X %s", 
                  vmu_id, mask, pressed ? "pressed" : "released");
    }
    
    u8 getVmuButtonState(int vmu_id)
    {
        if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT)
            return 0;
            
        return g_buttonState[vmu_id];
    }
    
    namespace internal
    {
        void onVmuDisplayUpdate(int vmu_id, const u32* framebuffer, u64 timestamp)
        {
            if (!g_initialized || !g_enabled)
                return;
                
            if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT)
                return;
                
            // Update our tracking
            g_dirty[vmu_id] = true;
            g_lastUpdate[vmu_id] = timestamp;
            g_connected[vmu_id] = true;
            
            // Copy to our local buffer for safe JNI access
            {
                std::lock_guard<std::mutex> lock(g_framebufferMutex[vmu_id]);
                std::memcpy(g_framebufferCopy[vmu_id], framebuffer, sizeof(g_framebufferCopy[vmu_id]));
            }
            
            // Invoke callback if registered
            VmuUpdateCallback callback;
            {
                std::lock_guard<std::mutex> lock(g_callbackMutex);
                callback = g_updateCallback;
            }
            
            if (callback)
            {
                callback(vmu_id, framebuffer, timestamp);
            }
        }
        
        void onVmuConnectionChange(int vmu_id, bool connected)
        {
            if (!g_initialized)
                return;
                
            if (vmu_id < 0 || vmu_id >= MAX_VMU_COUNT)
                return;
                
            g_connected[vmu_id] = connected;
            
            if (!connected)
            {
                // Clear framebuffer when VMU disconnects
                std::lock_guard<std::mutex> lock(g_framebufferMutex[vmu_id]);
                std::memset(g_framebufferCopy[vmu_id], 0, sizeof(g_framebufferCopy[vmu_id]));
            }
            
            DEBUG_LOG(MAPLE, "VMU Bridge: VMU %d %s", 
                      vmu_id, connected ? "connected" : "disconnected");
        }
    }
    
} // namespace vmu_bridge
