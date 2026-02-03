# Flycast DualScreen Experimental - Architecture Document

## Overview

This document outlines the architecture for two major features being added to Flycast:
1. **RetroAchievements Support** - Already present in upstream Flycast, needs Android UI integration
2. **VMU Second-Screen Support** - New feature for devices like AYN Thor with secondary displays

---

## Current State Analysis

### RetroAchievements (Already Integrated)

Flycast already has full rcheevos integration in:
- `core/achievements/achievements.cpp` - Full RA client implementation
- `core/achievements/achievements.h` - Public API
- `core/ui/gui_achievements.cpp` - ImGui-based achievement UI/notifications
- `core/deps/rcheevos/` - rcheevos library source

**What's implemented:**
- ✅ rcheevos library integration
- ✅ Dreamcast ROM hashing (via `rc_hash.c`)
- ✅ Dreamcast RAM exposure (`clientReadMemory` callback)
- ✅ Achievement unlock events and notifications
- ✅ Login with username/password and token persistence
- ✅ In-game overlay via ImGui (notifications, progress, challenges)
- ✅ Achievement list screen in settings

**What needs Android integration:**
- Native Android UI for RA login (optional, ImGui works)
- Expose achievement data via JNI for native Android UI (optional enhancement)

### VMU Implementation (Existing)

Current VMU implementation in `core/hw/maple/maple_devs.cpp`:
- VMU LCD is 48x32 pixels, 1-bit monochrome
- LCD data decoded to `lcd_data_decoded[48*32]` (u8 per pixel: 0x00=black, 0xFF=white)
- `config->SetImage(lcd_data_decoded)` → `push_vmu_screen()` → `vmu_lcd_data[8][48*32]` (u32 RGBA)
- Currently rendered as small overlay in ImGui (`core/rend/osd.cpp`)

---

## Architecture for VMU Second-Screen Support

### Design Goals
1. Expose VMU framebuffer to Android/JNI
2. Allow rendering on a separate Android View
3. Support AYN Thor's secondary display via Android Presentation API
4. Keep modular - can be toggled on/off
5. Support multiple VMUs (up to 8: 4 controllers × 2 VMU slots each)

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          C++ Core (Flycast)                             │
├─────────────────────────────────────────────────────────────────────────┤
│  maple_sega_vmu                                                         │
│  ├── lcd_data[192]           Raw VMU LCD data (6 bytes × 32 rows)      │
│  ├── lcd_data_decoded[1536]  Decoded pixels (48×32 u8)                 │
│  └── SetImage() ─────────────────────────────────────────┐              │
│                                                          │              │
│  push_vmu_screen(bus, port, buffer)                      │              │
│  └── vmu_lcd_data[8][1536]   RGBA u32 buffer ◄───────────┘              │
│                                                                         │
│  NEW: VMUBridge (vmu_bridge.cpp/h)                                      │
│  ├── getVmuFramebuffer(vmu_id) → const u32*                            │
│  ├── isVmuActive(vmu_id) → bool                                        │
│  ├── getVmuLastUpdate(vmu_id) → u64                                    │
│  ├── registerVmuCallback(callback) → void                              │
│  └── setVmuButtonState(vmu_id, buttons) → void                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ JNI
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         JNI Layer (Android.cpp)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  NEW Functions:                                                         │
│  ├── Java_..._JNIdc_getVmuFramebuffer(vmuId) → int[]                   │
│  ├── Java_..._JNIdc_isVmuActive(vmuId) → boolean                       │
│  ├── Java_..._JNIdc_getVmuCount() → int                                │
│  ├── Java_..._JNIdc_setVmuEnabled(enabled) → void                      │
│  └── Java_..._JNIdc_sendVmuButton(vmuId, button, pressed) → void       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Java/Kotlin
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       Android UI Layer (Kotlin)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  JNIdc.java                                                             │
│  └── Native method declarations                                         │
│                                                                         │
│  NEW: VmuDisplayView.kt (Custom View)                                   │
│  ├── Bitmap for VMU display (48×32 scaled)                             │
│  ├── Polls framebuffer at 30fps                                        │
│  ├── Touch input for VMU buttons                                       │
│  └── Configurable colors (pixel on/off)                                │
│                                                                         │
│  NEW: VmuDisplayManager.kt (Lifecycle Manager)                          │
│  ├── Manages VMU view instances                                        │
│  ├── Handles secondary display detection                               │
│  └── Creates Presentation for external displays                        │
│                                                                         │
│  NEW: VmuPresentation.kt (For AYN Thor)                                 │
│  ├── Extends android.app.Presentation                                  │
│  ├── Shows VMU on secondary display                                    │
│  └── Full-screen or custom layout                                      │
│                                                                         │
│  NEW: VmuSettingsFragment.kt                                            │
│  ├── Enable/disable VMU display                                        │
│  ├── Select which VMU to show                                          │
│  ├── Color customization                                               │
│  └── Display selection (main/secondary)                                │
└─────────────────────────────────────────────────────────────────────────┘
```

### File Structure

```
shell/android-studio/flycast/src/main/
├── java/com/flycast/emulator/
│   ├── emu/
│   │   ├── JNIdc.java              # Add VMU native methods
│   │   └── ...
│   └── vmu/                        # NEW PACKAGE
│       ├── VmuDisplayView.kt       # Custom View for VMU rendering
│       ├── VmuDisplayManager.kt    # Lifecycle and display management
│       ├── VmuPresentation.kt      # Secondary display presentation
│       └── VmuConfig.kt            # VMU display settings
├── jni/src/
│   └── Android.cpp                 # Add VMU JNI functions
└── res/
    └── layout/
        └── vmu_display.xml         # VMU overlay layout

core/
├── vmu/                            # NEW MODULE
│   ├── vmu_bridge.cpp              # VMU data bridge for JNI
│   ├── vmu_bridge.h                # Public interface
│   └── CMakeLists.txt              # Build config
└── hw/maple/
    └── maple_devs.cpp              # Minor hook for VMU updates
```

---

## Implementation Plan

### Phase 1: VMU Bridge Layer (C++)

**Files to create:**
- `core/vmu/vmu_bridge.h`
- `core/vmu/vmu_bridge.cpp`
- `core/vmu/CMakeLists.txt`

**Purpose:** Clean interface between Flycast's VMU emulation and external consumers (Android).

### Phase 2: JNI Bindings

**Files to modify:**
- `shell/android-studio/flycast/src/main/jni/src/Android.cpp`
- `shell/android-studio/flycast/src/main/java/com/flycast/emulator/emu/JNIdc.java`

**New native methods:**
```java
// VMU Display
public static native int[] getVmuFramebuffer(int vmuId);
public static native boolean isVmuActive(int vmuId);
public static native int getVmuCount();
public static native long getVmuLastUpdate(int vmuId);
public static native void setVmuDisplayEnabled(boolean enabled);

// VMU Input (for standalone VMU buttons)
public static native void sendVmuButton(int vmuId, int button, boolean pressed);
```

### Phase 3: Android VMU Display

**Files to create:**
1. `VmuDisplayView.kt` - Custom SurfaceView that renders VMU LCD
2. `VmuDisplayManager.kt` - Manages VMU display lifecycle
3. `VmuPresentation.kt` - Presentation class for secondary displays
4. `VmuConfig.kt` - Settings data class

**Key features:**
- Real-time rendering at configurable frame rate
- Scalable display (48×32 → any size with nearest-neighbor filtering)
- Touch input zones for VMU buttons (optional)
- Color customization (classic green LCD, inverted, custom colors)

### Phase 4: AYN Thor Secondary Display Integration

**Android Presentation API usage:**
```kotlin
class VmuPresentation(
    context: Context,
    display: Display
) : Presentation(context, display) {
    private lateinit var vmuView: VmuDisplayView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vmuView = VmuDisplayView(context)
        setContentView(vmuView)
    }
}
```

**Display detection:**
```kotlin
val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
val displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
// AYN Thor secondary display will appear here
```

### Phase 5: Settings Integration

Add VMU display settings to Flycast's existing settings UI:
- Enable/disable VMU display
- Select primary/secondary display
- Choose which VMU(s) to show
- Color scheme selection
- Size/position options

---

## RetroAchievements Android Integration (Optional Enhancements)

The existing ImGui-based RA UI works on Android. Optional native Android enhancements:

### Potential Native Android Components

1. **Achievement Notification Service** - Native Android notifications for unlocks
2. **Achievement List Activity** - Native RecyclerView-based achievement browser
3. **Login Fragment** - Native Material Design login form

### JNI Additions (if needed)

```java
// Achievements (optional native UI)
public static native boolean raIsLoggedIn();
public static native String raGetUsername();
public static native void raLogin(String username, String password);
public static native void raLogout();
public static native String[] raGetAchievements(); // JSON array
```

---

## Build Instructions

### Prerequisites
- Android NDK r29 (already configured in build.gradle)
- CMake 3.22.1+
- Java 11+

### Building Debug APK

```bash
cd shell/android-studio
./gradlew assembleDebug
```

APK output: `flycast/build/outputs/apk/debug/flycast-debug.apk`

### Building Release APK

```bash
cd shell/android-studio
export ANDROID_KEYSTORE_PASSWORD="your_password"
./gradlew assembleRelease
```

---

## Configuration Options

### New Config Options (config::)

```cpp
// VMU Second Screen
config::VmuDisplayEnabled       // bool, default: false
config::VmuDisplayScreen        // int, 0=main, 1=secondary
config::VmuDisplayVmuId         // int, which VMU to show (-1=all)
config::VmuPixelOnColor         // u32 ARGB, default: 0xFF00FF00 (green)
config::VmuPixelOffColor        // u32 ARGB, default: 0xFF001100 (dark green)
config::VmuDisplayScale         // float, default: 4.0
```

---

## Testing Checklist

### VMU Display
- [ ] VMU display renders correctly on main screen
- [ ] VMU display renders on AYN Thor secondary screen
- [ ] Multiple VMU support (test with 2+ VMUs)
- [ ] VMU updates at correct rate
- [ ] No performance regression
- [ ] Settings persist across app restarts
- [ ] Toggle on/off works without restart

### RetroAchievements (Existing)
- [ ] Login/logout works
- [ ] Achievement notifications appear
- [ ] Achievement list shows correctly
- [ ] Achievements unlock properly
- [ ] Hardcore mode toggle works

---

## Files to Create/Modify Summary

### New Files
| File | Purpose |
|------|---------|
| `core/vmu/vmu_bridge.h` | VMU bridge public interface |
| `core/vmu/vmu_bridge.cpp` | VMU bridge implementation |
| `core/vmu/CMakeLists.txt` | VMU module build config |
| `shell/.../vmu/VmuDisplayView.kt` | Android VMU renderer |
| `shell/.../vmu/VmuDisplayManager.kt` | VMU display lifecycle |
| `shell/.../vmu/VmuPresentation.kt` | Secondary display support |
| `shell/.../vmu/VmuConfig.kt` | VMU settings model |

### Modified Files
| File | Changes |
|------|---------|
| `CMakeLists.txt` | Add vmu module subdirectory |
| `shell/.../jni/src/Android.cpp` | Add VMU JNI functions |
| `shell/.../emu/JNIdc.java` | Add VMU native method declarations |
| `core/rend/osd.cpp` | Add callback hook for VMU updates |
| `core/cfg/option.h` | Add VMU config options |

---

## Next Steps

1. **Implement Phase 1**: Create VMU bridge layer in C++
2. **Implement Phase 2**: Add JNI bindings
3. **Implement Phase 3**: Create Android VMU display components
4. **Test on emulator**: Verify basic functionality
5. **Implement Phase 4**: Add AYN Thor secondary display support
6. **Test on hardware**: Verify on actual AYN Thor device
7. **Polish**: Add settings UI, optimize performance

Ready to begin implementation. Which phase would you like to start with?
