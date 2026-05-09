# Virtual Buttons

A native Android app that replaces damaged or unresponsive physical volume buttons with fast, reliable software controls that work from any app — without reopening anything.

## Features

### Floating Button
A draggable chat-head style overlay that responds to swipes, taps, and double-taps. Gesture mode is fully configurable. Position is saved automatically.

### Edge Gestures
Invisible left and right edge strips let you swipe up or down to change volume without aiming at the bubble.

### Quick Settings Tile
A `Volume Button` tile in your notification shade toggles the overlay in one tap.

### Notification Controls
A persistent foreground notification with up, down, mute, and stop actions — works even from the lock screen.

### Shake to Mute
Optional accelerometer-based toggle. Uses the sensor only while the service is active to minimize battery impact.

### Auto Night Profile
Optional scheduled profile that lowers your media volume each night at a time you choose.

## Customization

The settings screen offers:
- **Gesture type:** swipe, double-tap, or both
- **Gesture sensitivity:** adjustable movement threshold
- **Floating button:** size and opacity
- **Edge strips:** enable/disable and width
- **Volume step:** number of levels per adjustment
- **Stream targeting:** active, media, or system
- **Haptic feedback:** vibration confirmation per action
- **Visual indicator:** compact volume pill on change
- **Start on boot:** overlay restarts after reboot
- **Night profile:** scheduled volume reduction

## Privacy

The app uses `SYSTEM_ALERT_WINDOW` for the floating overlay and a foreground service for reliability. No accessibility service is included — volume control is handled entirely through Android's standard audio, overlay, notification, sensor, tile, and alarm APIs.

## Build

```bash
gradle :app:assembleDebug
```

## Download APK

### Latest Release
- **[Download VirtualButtons.apk](https://github.com/RG-Builder/Virtual-buttons/releases/latest)** — v1.0.5

### All Releases
- **[Releases page](https://github.com/RG-Builder/Virtual-buttons/releases)** — version history

**v1.0.5 Fixes:**
- **CRITICAL**: Remove FLAG_NOT_TOUCHABLE that blocked all gestures
- Swipe direction fixed
- Edge gestures sensitivity improved

On first install, Android may warn about unknown sources — tap Allow and reopen the APK.

---

## Release History

### latest (v1.0.3)
- **Gesture controls**: improved swipe/double-tap detection and sensitivity
- **Edge gestures**: enhanced vertical drag threshold calculation
- **Bubble state**: better reappear after hide, permanent pin, auto-hide scheduling
- **Settings UI**: gesture mode spinner, edge strip width, shake sensitivity controls
- Full changelog on [Releases page](https://github.com/RG-Builder/Virtual-buttons/releases)

### v1.0.2
- **Gesture controls fixed**: bubble swipe and edge gesture detection
- **Bubble state fixed**: reappears after hide, permanent pin, auto-hide scheduling
- **Background running**: service keeps running without bubble, tile toggles visibility
- **Hide notification**: option to hide persistent notification from status bar
- **Quick Settings tile**: improved background mode behavior

### v1.0.1
- **Bug fixes**: night profile midnight crossing, bubble off-screen bounds, enum crash protection, animation overlap, handler leak, adjust debounce, edge gesture cancel
- **Android 12+**: exact alarm permission request for night profile scheduling
- **Widget**: home screen widget live-updates on volume changes
- **Release build**: R8 minification (52KB vs 85KB debug)
- **i18n**: all hardcoded strings localized

### v1.0.0
- Initial release
- Floating button with swipe/tap gestures
- Edge gesture strips
- Quick Settings tile
- Notification controls
- Shake to mute
- Auto night profile
- Haptic feedback
- Visual volume indicator