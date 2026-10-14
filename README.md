# Naro Launcher

A Naro-branded Android launcher project for Minecraft: Java Edition.

**Current milestone:** clean Android baseline and CI build. This baseline is not yet a playable Minecraft launcher; the game engine, compatible Android Java runtimes, graphics stack, and launch pipeline must be integrated and tested in later milestones.

## Project
- Android / Kotlin / Jetpack Compose
- Application ID: `com.narostudio.narolauncher`
- Dark interface with Naro orange accents
- GitHub Actions debug APK artifact

## Build
Requires JDK 17 and Android SDK platform/build tools for API 35.

```bash
gradle --no-daemon :app:assembleDebug
```

The APK should be at `app/build/outputs/apk/debug/app-debug.apk`.

## Engine direction
Evaluate and integrate compatible, properly licensed components from the Android Java Edition launcher ecosystem rather than writing a JVM or graphics translation layer from scratch. Upstream source, current maintenance status, license obligations, runtime support, and graphics compatibility must be reviewed before integration. See [PojavLauncher source](https://github.com/TeamPojavLauncher/PojavLauncher) as an architectural reference.

## Compatibility honesty
A successful Android APK build does not prove Minecraft can launch. No game versions or rendering backends are claimed playable until launch tests on real ARM64 Android hardware are recorded. Official Minecraft metadata and legitimate account authentication will be used where applicable.
