# Virtual Buttons

Virtual Buttons is a native Android app that replaces damaged or unresponsive physical volume buttons with fast software controls that work without reopening the app.

## Core interactions

- **Floating button:** a draggable chat-head style overlay that reacts to taps, swipes, and double taps.
- **Edge gestures:** invisible left and right edge strips let users swipe up/down to adjust volume.
- **Quick Settings tile:** a `Volume Button` tile toggles the overlay directly from the notification shade.
- **Notification controls:** persistent foreground notification actions provide lock-screen-friendly volume up, volume down, mute, and stop controls.
- **Shake to mute:** optional accelerometer-based mute/unmute while the foreground service is enabled.
- **Auto-profile:** optional nightly media-volume profile scheduled with `AlarmManager`.

## Customization

The settings screen provides Material-inspired cards for:

- Gesture type and sensitivity.
- Floating button size, opacity, and saved position.
- Volume step increments.
- Active/media/system stream targeting.
- Haptic feedback and visual indicators.
- Edge gestures, shake-to-mute, boot restart, and night profile behavior.

## Android behavior

The app uses `SYSTEM_ALERT_WINDOW` for the floating overlay and a foreground service for reliability. No accessibility service is included because the volume-control use case can be handled through Android audio, overlay, notification, sensor, tile, and alarm APIs without observing user input globally.

## Build

```bash
gradle :app:assembleDebug
```
