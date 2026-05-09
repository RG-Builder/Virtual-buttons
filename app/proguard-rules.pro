-keepclassmembers class com.example.virtualbuttons.** { *; }
-keep class com.example.virtualbuttons.SettingsStore$GestureMode { *; }
-keep class com.example.virtualbuttons.SettingsStore$StreamMode { *; }
-keep class com.example.virtualbuttons.VolumeController$VolumeState { *; }
-keep class com.example.virtualbuttons.CircleDrawable { *; }
-keep class com.example.virtualbuttons.RoundRectDrawable { *; }
-keep class com.example.virtualbuttons.SpaceView { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
