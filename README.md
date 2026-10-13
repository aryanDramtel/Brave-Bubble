# Brave Bubble

Brave Bubble is an Android app that displays a movable floating bubble with quick device and astronomy information.

## Features

- Data transfer speed monitor
- World clock for a selected time zone
- Battery status
- Info Mode with optional altitude, lunar phase, estimated Earth-Sun and Earth-Moon distances, and sunrise/sunset
- Bubble size, shape, colors, font, opacity, and border settings
- Quick Settings tile for toggling the bubble service

The Weather Station screen currently lets you save an OpenWeather API key locally; weather data fetching is not implemented yet.

## Build

Requirements: Android SDK and JDK 17.

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions and privacy

The overlay requires Android's “Display over other apps” permission. Info Mode requests location permission for altitude and local sunrise/sunset estimates. Location is not uploaded by the app.

The OpenWeather API key is saved in app-private preferences and excluded from Android backup. Do not commit API keys or other credentials to the repository.
