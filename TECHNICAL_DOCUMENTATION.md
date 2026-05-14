# Virtual Buttons - Technical Documentation

## Overview

Virtual Buttons is an Android application that provides on-screen software controls to replace broken or hard-to-reach physical buttons. The app supports Power, Volume Up, Volume Down, Home, Recents, and Back button functionality through touch-based interactions and gestures.

## Architecture

### Core Components

1. **MainActivity** - Main settings interface with all configuration options
2. **FloatingVolumeService** - Floating bubble overlay for volume control
3. **ButtonPanelService** - Full button panel with all 6 buttons (Power, Vol+/-, Home, Recents, Back)
4. **VirtualButtonAccessibilityService** - Accessibility service for system navigation actions
5. **SettingsStore** - Persistent settings storage
6. **ActionManager** - Action dispatch and permission handling
7. **VolumeController** - Volume adjustment logic

### Key Files

```
app/src/main/java/com/example/virtualbuttons/
├── MainActivity.java          - Settings UI
├── FloatingVolumeService.java - Floating volume bubble
├── ButtonPanelService.java    - On-screen button panel
├── VirtualButtonAccessibilityService.java - System navigation
├── SettingsStore.java         - Preferences
├── ActionManager.java         - Action dispatch
├── VolumeController.java      - Volume logic
└── ...

app/src/main/res/
├── drawable/                  - Button icons
├── xml/accessibility_service_config.xml - Accessibility config
└── values/strings.xml         - UI strings
```

## Implementation Details

### Button Panel

The ButtonPanelService displays a 3x2 grid of buttons:
- Row 1: Power, Volume Up, Volume Down
- Row 2: Home, Recents, Back

**Features:**
- Draggable panel (position stored in settings)
- Position options: Top, Bottom, Center
- Adjustable size and opacity
- Compact mode option
- Individual button enable/disable

### Gesture Support

**Edge Gestures:**
- Swipe from screen edges for Volume Up/Down
- Configurable sensitivity and strip width

**Button Gestures:**
- Swipe Up/Down on bubble: Volume control
- Double-tap: Mute toggle
- Long-press: Hide bubble

### Accessibility Service

The VirtualButtonAccessibilityService provides:
- Home button (GLOBAL_ACTION_HOME)
- Back button (GLOBAL_ACTION_BACK)
- Recents button (GLOBAL_ACTION_RECENTS)
- Volume control via AudioManager

### Permissions Required

1. **SYSTEM_ALERT_WINDOW** - Draw over other apps
2. **FOREGROUND_SERVICE** - Run as persistent service
3. **VIBRATE** - Haptic feedback
4. **RECEIVE_BOOT_COMPLETED** - Auto-start on boot
5. **SCHEDULE_EXACT_ALARM** - Night profile scheduling
6. **ACCESSIBILITY_SERVICE** - For Home/Back/Recents (optional)

## Customization Options

### Button Panel Settings
- Show/hide panel
- Panel position (top/bottom/center)
- Button size (40-80dp)
- Panel opacity (40-100%)
- Compact mode

### Button Toggles
Individual enable/disable for each button:
- Power
- Volume Up
- Volume Down
- Home
- Recents
- Back

### Gesture Settings
- Edge gestures enable/disable
- Edge strip width (4-24dp)
- Global gesture sensitivity
- Shake to mute option
- Gesture mode (swipe/double-tap/both)

### Volume Settings
- Volume step size (1-5)
- Controlled stream (Media/System/Auto)
- Haptic feedback
- Visual indicator

## Usage Instructions

### First Time Setup
1. Install the APK
2. Grant "Draw over other apps" permission
3. Start the service
4. Optionally enable Accessibility Service for Home/Back/Recents

### Using the Button Panel
- Tap any button to trigger its action
- Long-press and drag to reposition
- The panel stays visible across all apps

### Edge Gestures
- Swipe from left/right screen edges to adjust volume
- Configure sensitivity in settings

## Technical Notes

### API Level Support
- Minimum: API 26 (Android 8.0)
- Target: API 34 (Android 14)
- Compile: API 36

### Background Operation
- Uses foreground service with persistent notification
- Auto-restarts on device boot (if enabled)
- Battery optimization can be disabled for reliability

### Known Limitations
- Power button: Uses KEYCODE_POWER intent (may not work on all devices)
- Home/Recents: Requires Accessibility Service for full functionality
- Some gestures may conflict with system gestures on certain devices

## Debug APK Location

```
app/build/outputs/apk/debug/VirtualButtons.apk
```

## Version

- Version Code: 2
- Version Name: 1.0.1
- Package: com.example.virtualbuttons