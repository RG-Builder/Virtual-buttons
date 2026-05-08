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

**[Download VirtualButtons.apk](https://github.com/RG-Builder/Virtual-buttons/releases/download/latest/VirtualButtons.apk)**

On first install, Android may warn about unknown sources — tap Allow and reopen the APK.