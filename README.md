# Naro Launcher

Landscape-first Android Minecraft Java launcher UI and management foundation, styled to match the supplied NARO reference.

## Included
- Landscape-first navigation and responsive dark/orange UI
- Home dashboard, version browser, Modrinth search, skins/capes, accounts, profiles, downloads, settings
- Persistent settings, profiles, local/offline account label, skin entries, and download task list using SharedPreferences
- Runtime/graphics integration boundary for a future PojavLauncher engine adapter
- GitHub Actions debug APK workflow

## Important
This is a management/UI milestone, not yet a playable Minecraft launcher. Microsoft OAuth, Mojang skin upload, Modrinth project downloads, and actual version installation require their respective API/download/auth flows to be implemented and tested. No fake sign-in or fake game launch is presented as real. The Play action explains that the Pojav engine adapter is not integrated yet.

## Build
Open in Android Studio or run `gradle :app:assembleDebug` with Android SDK 35 and JDK 17. The workflow uses Gradle 8.9 installed by GitHub Actions.
