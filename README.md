<<<<<<< HEAD
<<<<<<< HEAD
# Flycast-DualScreen-Experimental
Experimental Flycast fork with duel-screen support for the Ayn Thor, adding VMU capabilities and RetroAchievements.
=======
# Flycast

[![Android CI](https://github.com/flyinghead/flycast/actions/workflows/android.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/android.yml)
[![C/C++ CI](https://github.com/flyinghead/flycast/actions/workflows/c-cpp.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/c-cpp.yml)
[![Nintendo Switch CI](https://github.com/flyinghead/flycast/actions/workflows/switch.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/switch.yml)
[![Windows UWP CI](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml)
[![BSD CI](https://github.com/flyinghead/flycast/actions/workflows/bsd.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/bsd.yml)

<img src="shell/linux/flycast.png" alt="flycast logo" width="150"/>

**Flycast** is a multi-platform Sega Dreamcast, Naomi, Naomi 2, and Atomiswave emulator derived from [**reicast**](https://github.com/skmp/reicast-emulator).

Information about configuration and supported features can be found on [**TheArcadeStriker's flycast wiki**](https://github.com/TheArcadeStriker/flycast-wiki/wiki).

Join us on our [**Discord server**](https://discord.gg/X8YWP8w) for a chat. 

## Install

### Android ![android](https://flyinghead.github.io/flycast-builds/android.jpg)
Install Flycast from [**Google Play**](https://play.google.com/store/apps/details?id=com.flycast.emulator).
### Flatpak (Linux ![ubuntu logo](https://flyinghead.github.io/flycast-builds/ubuntu.png))

1. [Set up Flatpak](https://www.flatpak.org/setup/).

2. Install Flycast from [Flathub](https://flathub.org/apps/details/org.flycast.Flycast):

`flatpak install -y org.flycast.Flycast`

3. Run Flycast:

`flatpak run org.flycast.Flycast`

### Homebrew (MacOS ![apple logo](https://flyinghead.github.io/flycast-builds/apple.png))

1. [Set up Homebrew](https://brew.sh).

2. Install Flycast via Homebrew:

`brew install --cask flycast`
````markdown
# Flycast DualScreen Experimental

This repository is a fork of Flycast with experimental dual-screen and VMU features (see ARCHITECTURE.md).

---

The remainder of this README contains the upstream Flycast README content.

=======
# Flycast

[![Android CI](https://github.com/flyinghead/flycast/actions/workflows/android.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/android.yml)
[![C/C++ CI](https://github.com/flyinghead/flycast/actions/workflows/c-cpp.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/c-cpp.yml)
[![Nintendo Switch CI](https://github.com/flyinghead/flycast/actions/workflows/switch.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/switch.yml)
[![Windows UWP CI](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml)
[![BSD CI](https://github.com/flyinghead/flycast/actions/workflows/bsd.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/bsd.yml)

<img src="shell/linux/flycast.png" alt="flycast logo" width="150"/>

**Flycast** is a multi-platform Sega Dreamcast, Naomi, Naomi 2, and Atomiswave emulator derived from [**reicast**](https://github.com/skmp/reicast-emulator).

Information about configuration and supported features can be found on [**TheArcadeStriker's flycast wiki**](https://github.com/TheArcadeStriker/flycast-wiki/wiki).

Join us on our [**Discord server**](https://discord.gg/X8YWP8w) for a chat. 

## Install

### Android ![android](https://flyinghead.github.io/flycast-builds/android.jpg)
Install Flycast from [**Google Play**](https://play.google.com/store/apps/details?id=com.flycast.emulator).
### Flatpak (Linux ![ubuntu logo](https://flyinghead.github.io/flycast-builds/ubuntu.png))

1. [Set up Flatpak](https://www.flatpak.org/setup/).

2. Install Flycast from [Flathub](https://flathub.org/apps/details/org.flycast.Flycast):

`flatpak install -y org.flycast.Flycast`

3. Run Flycast:

`flatpak run org.flycast.Flycast`

### Homebrew (MacOS ![apple logo](https://flyinghead.github.io/flycast-builds/apple.png))

1. [Set up Homebrew](https://brew.sh).

2. Install Flycast via Homebrew:

`brew install --cask flycast`

### iOS

Due to persistent harassment from an iOS user, support for this platform has been dropped. 

### Xbox One/Series ![xbox logo](https://flyinghead.github.io/flycast-builds/xbox.png)

Grab the latest build from [**the builds page**](https://flyinghead.github.io/flycast-builds/), or the [**GitHub Actions**](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml). Then install it using the **Xbox Device Portal**.

### Binaries ![android](https://flyinghead.github.io/flycast-builds/android.jpg) ![windows](https://flyinghead.github.io/flycast-builds/windows.png) ![linux](https://flyinghead.github.io/flycast-builds/ubuntu.png) ![apple](https://flyinghead.github.io/flycast-builds/apple.png) ![switch](https://flyinghead.github.io/flycast-builds/switch.png) ![xbox](https://flyinghead.github.io/flycast-builds/xbox.png)

Get fresh builds for your system [**on the builds page**](https://flyinghead.github.io/flycast-builds/).

**New:** Now automated test results are available as well. 

### Build instructions:
```
$ git clone --recursive https://github.com/flyinghead/flycast.git
$ cd flycast
$ mkdir build && cd build
$ cmake ..
$ make
```

````
=======
# Flycast DualScreen Experimental

[![Build Status](https://img.shields.io/badge/build-in_progress-yellow)]()
[![License](https://img.shields.io/badge/license-GPL--2.0-blue)](LICENSE)

Experimental Flycast fork with **dual-screen support** for the **AYN Thor**, adding enhanced VMU display capabilities and RetroAchievements integration.

## Features

### 🏆 RetroAchievements Support
- Full rcheevos library integration (inherited from upstream Flycast)
- Dreamcast ROM hashing support
- In-game achievement notifications
- Achievement list and progress tracking
- Login with RA credentials

### 📺 VMU Second-Screen Display
- Real-time VMU LCD rendering on Android
- Secondary display support (AYN Thor companion screen)
- Support for multiple VMUs (up to 8)
- Customizable display colors and scaling
- Touch input for VMU buttons (optional)
- Modular design - can be toggled on/off

## Building

### Prerequisites
- Android NDK r29+
- CMake 3.22.1+
- Java 11+

### Build Debug APK

```bash
cd shell/android-studio
./gradlew assembleDebug
```

Output: `flycast/build/outputs/apk/debug/flycast-debug.apk`

### Build Release APK

```bash
cd shell/android-studio
export ANDROID_KEYSTORE_PASSWORD="your_password"
./gradlew assembleRelease
```

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed technical documentation.

# Flycast DualScreen Experimental

This repository is a fork of Flycast with experimental dual-screen and VMU features for devices like the AYN Thor. See ARCHITECTURE.md for design and implementation details.

---

The remainder of this file contains the upstream Flycast README (trimmed here for brevity).

# Flycast

[![Android CI](https://github.com/flyinghead/flycast/actions/workflows/android.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/android.yml)
[![C/C++ CI](https://github.com/flyinghead/flycast/actions/workflows/c-cpp.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/c-cpp.yml)
[![Nintendo Switch CI](https://github.com/flyinghead/flycast/actions/workflows/switch.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/switch.yml)
[![Windows UWP CI](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/uwp.yml)
[![BSD CI](https://github.com/flyinghead/flycast/actions/workflows/bsd.yml/badge.svg)](https://github.com/flyinghead/flycast/actions/workflows/bsd.yml)

<img src="shell/linux/flycast.png" alt="flycast logo" width="150"/>

**Flycast** is a multi-platform Sega Dreamcast, Naomi, Naomi 2, and Atomiswave emulator derived from [**reicast**](https://github.com/skmp/reicast-emulator).

Information about configuration and supported features can be found on [**TheArcadeStriker's flycast wiki**](https://github.com/TheArcadeStriker/flycast-wiki/wiki).

Join us on our [**Discord server**](https://discord.gg/X8YWP8w) for a chat. 

## Install

### Android ![android](https://flyinghead.github.io/flycast-builds/android.jpg)
Install Flycast from [**Google Play**](https://play.google.com/store/apps/details?id=com.flycast.emulator).
### Flatpak (Linux ![ubuntu logo](https://flyinghead.github.io/flycast-builds/ubuntu.png))

1. [Set up Flatpak](https://www.flatpak.org/setup/).

2. Install Flycast from [Flathub](https://flathub.org/apps/details/org.flycast.Flycast):

`flatpak install -y org.flycast.Flycast`

3. Run Flycast:

`flatpak run org.flycast.Flycast`

### Homebrew (MacOS ![apple logo](https://flyinghead.github.io/flycast-builds/apple.png))

1. [Set up Homebrew](https://brew.sh).

2. Install Flycast via Homebrew:

`brew install --cask flycast`
