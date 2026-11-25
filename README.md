# IVC SIYAM Game Booster

A native Android gaming optimization application built with **Kotlin**, **Jetpack Compose**, and **Material 3** in a black & electric-blue aesthetic.

## Core Features

- **BOOST GAME Dashboard**: Live RAM usage (`ActivityManager` + `/proc/meminfo`), CPU cores/load/sysfs clock frequencies, battery & SoC thermal telemetry (`BatteryManager` + `PowerManager.getCurrentThermalStatus()`), storage/network status, real display refresh rate (`Hz`), and honest booster UI frame timing (`Choreographer`).
- **Game Profiles**: Dedicated reversible presets for **Free Fire**, **PUBG Mobile**, and **Minecraft** with automatic installed-package detection and 1-tap launch.
- **Reversible Settings Snapshot**: Captures pre-boost DND mode, screen brightness, keep-awake flag, and optional Shizuku settings before modifying them, enabling 1-tap **Revert** back to baseline.
- **Official Shizuku API Integration (`dev.rikka.shizuku:api` & `provider` 13.1.5)**: Binder status monitoring, explicit `Shizuku.requestPermission(...)` flow, AIDL `UserService` (`IShizukuBoostService`), and mandatory consent confirmation dialogs before executing any privileged tweak.
- **Optional Termux Integration (`com.termux.RUN_COMMAND`)**: Curated read-only diagnostic templates and custom command draft runner that always require explicit user confirmation before dispatching to `com.termux.app.RunCommandService`.
- **Safe Background App Management**: Play-compliant `LAUNCHER` app discovery, automatic protection for core system apps, launcher, active games, Shizuku, and Termux, and safe `ActivityManager.killBackgroundProcesses()` trimming.
- **Integrity & Safety**: Never fabricates game FPS, never disables thermal throttling protections, and never executes Shizuku or Termux commands without explicit consent.

## Project Structure

- `app/src/main/java/com/example/MainActivity.kt`: Edge-to-edge activity host, reversible `FLAG_KEEP_SCREEN_ON`, and real dashboard UI frame timing.
- `app/src/main/java/com/example/booster/data/`: `HardwareMonitorRepository`, `GameProfileRepository` (DataStore), and `BackgroundAppRepository`.
- `app/src/main/java/com/example/booster/system/`: `ShizukuManager`, `ShizukuBoostUserService`, `TermuxBridge`, and `DeviceTweaksController`.
- `app/src/main/java/com/example/booster/ui/`: `BoosterViewModel`, `BoosterNavHost`, components, and `DashboardScreen`, `ProfilesScreen`, `AppsScreen`, `ToolsScreen`.
- `app/src/main/aidl/com/example/IShizukuBoostService.aidl`: Official Shizuku UserService AIDL contract.
- `app/src/main/AndroidManifest.xml`: Least-privilege permissions, Play-compliant `<queries>`, and `rikka.shizuku.ShizukuProvider`.

## Android Studio Build Instructions

1. Open the project root directory in **Android Studio** (AGP 9.1+ / JDK 17+ / Android SDK 36).
2. Let **Gradle Sync** complete (`compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`).
3. Build and install via Android Studio **Run ▶** or terminal:
   ```bash
   ./gradlew assembleDebug
   ./gradlew :app:testDebugUnitTest
   ```
4. **Optional Shizuku Setup**:
   - Install and start **Shizuku** (`moe.shizuku.privileged.api`) via Wireless Debugging or ADB.
   - Open **Tools -> Shizuku API Integration -> Authorize Shizuku** inside IVC SIYAM Game Booster.
5. **Optional Termux Setup**:
   - Install **Termux** (`com.termux`) and run:
     ```bash
     echo "allow-external-apps=true" >> ~/.termux/termux.properties
     ```
   - Grant the `RUN_COMMAND` permission to **IVC SIYAM Game Booster** in Android **App Info -> Permissions**.
