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

## Download

[Download the latest APK from GitHub Releases](../../releases/download/latest/VirtualButtons-debug.apk).

The direct-download link always points to the debug-signed build from the most recent successful CI run. On first install, Android may ask you to allow installation from this source — tap Allow and reopen the APK.

### Publishing a versioned release

GitHub Actions builds an APK on every push. Pushes to `main` update the stable `latest` release. To publish a versioned release, push a tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow will attach the APK to that GitHub Release while keeping the direct-download link available.