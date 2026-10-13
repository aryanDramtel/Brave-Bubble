# Brave Bubble

Brave Bubble is an Android app that puts a customizable, draggable information bubble over other apps. It includes quick device monitors, a world clock, and an astronomy-focused information mode.

## Features

- **Data Monitor** — current network transfer rate and a configurable usage limit.
- **World Clock** — display the time in a selected time zone.
- **Smart Battery** — battery level and charging status.
- **Info Mode** — choose which readings to show:
  - Device altitude (when location permission and an altitude-capable location fix are available)
  - Current lunar phase
  - Approximate Earth-Sun and Earth-Moon distances
  - Estimated sunrise, sunset, and daylight duration
- **Bubble appearance** — size, circle/rounded/square shape, background and ring colors, border, font scale, and background/font opacity.
- **Quick Settings tile** — toggle the floating bubble from Android Quick Settings.

## Run the app

Open the project in Android Studio, let Gradle sync, then run the `app` configuration on an Android device or emulator.

To build a debug APK from the command line, install JDK 17 and the Android SDK, then run:

```sh
./gradlew assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Using the bubble

1. Launch the app and choose a module.
2. Grant **Display over other apps** permission when Android prompts you.
3. Grant notification permission if requested by your Android version.
4. Tap **Launch / Restart Bubble**.
5. Drag the bubble to move it. A quick double tap cycles through the available modes. Long-press to stop the bubble service.

Info Mode requests location permission for altitude and sunrise/sunset estimates. Other Info Mode readings do not require location.

## Weather Station status

The Weather Station currently provides a field to save an OpenWeather API key on the device. Fetching or displaying weather data is not implemented yet.

## Privacy

Location is used on-device for the Info Mode readings described above; the app does not upload location. The OpenWeather API key is stored in app-private preferences and excluded from Android backup. Avoid putting credentials in source code or committing them to Git.
