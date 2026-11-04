# FireTV Custom Launcher (ATV Launcher Pro Style)

A high-performance, lightweight custom launcher designed specifically for **Amazon Fire TV sticks running FireOS 5** (Android 5.1 Lollipop, API 22).

---

## ✨ Features & ATV Launcher Pro Styling

* **ATV Launcher Pro Aesthetic**:
  * Deep cinematic dark gradient background (`#141720` -> `#0a0b10`).
  * Modern top status bar with a digital clock (`HH:mm`), date, and system settings shortcut.
  * Translucent dark rounded rectangular tiles with app icons and labels.
  * Vibrant electric cyan focus glow ring (`#00E5FF`) and smooth scale-up animation (1.10x).

* **Centered Favorites Bar**:
  * The main home screen displays a **single line of 5 favorite apps**, vertically centered on your TV screen.
  * Defaults to the first 5 installed apps on initial launch.
  * Automatically saves your favorites persistently in `SharedPreferences`.

* **Paging & Snapping Navigation**:
  * **Screen 1 (Home)**: Shows only the top bar and the centered 5-app Favorites row. The full app list is completely hidden off the bottom of the screen.
  * **Screen 2 (All Applications)**: Pressing **Down** from any favorite snaps the screen down directly to the full app grid with minimal top margin.
  * Pressing **Up** from the first row of All Apps snaps back up to the centered Favorites bar and hides the full list again.
  * Pressing **Up** from Favorites moves focus to the Settings gear icon.

* **Tile Options Modal**:
  * Pressing the remote's **Menu key (☰)** or long-pressing **Select** opens the ATV options dialog.
  * **Add to Favorites / Remove from Favorites**: Quickly pin or unpin any app to your 5-card Favorites bar.
  * **Open App**, **Application Info**, and **Uninstall App**.
  * Complete bidirectional D-pad navigation with high-contrast text.

* **Fire TV Native Settings Integration**:
  * The top-right gear icon launches directly into the native Fire TV Settings (`com.amazon.tv.settings.RootSettings`), safely bypassing the Home button watcher.

* **Auto-Start on Boot**:
  * A dedicated `BootReceiver` automatically starts your custom launcher whenever the Fire TV stick powers on or reboots.

---

## 🛠️ Project Structure

```
firetv-launcher/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/firetv/launcher/
│       │   ├── MainActivity.kt               # Paging, snap scrolling & lifecycle
│       │   ├── adapter/AppGridAdapter.kt     # Reactive focus & remote Menu key
│       │   ├── model/AppItem.kt              # App metadata model
│       │   ├── receiver/BootReceiver.kt      # Starts launcher on Fire TV boot
│       │   ├── service/
│       │   │   ├── HomeWatcherService.kt     # High-speed logcat Home button watcher
│       │   │   └── FireTvAccessibilityService.kt # Fallback accessibility service
│       │   └── util/
│       │       ├── AppLoader.kt              # Async package loader & deduplicator
│       │       ├── DpadFocusHelper.kt        # Smooth scaling & elevation animator
│       │       └── FavoritesManager.kt       # Persistent favorites storage
│       └── res/
│           ├── color/                        # Button text focus selectors
│           ├── drawable/                     # ATV Launcher Pro backgrounds & rings
│           ├── layout/                       # activity_main & dialog_app_options
│           └── values/                       # colors, dimens, strings, styles
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 🚀 Building & Installing

### 1. Build in Android Studio
1. Open **Android Studio** and choose `/home/peter/Desktop/firetv-launcher`.
2. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. The generated APK will be at:
   `app/build/outputs/apk/debug/app-debug.apk`

### 2. Install to Fire TV via ADB
Connect to your Fire TV stick over your local Wi-Fi:

```bash
# Connect to Fire TV (replace with your Fire TV's local IP address)
adb connect 192.168.1.xxx:5555

# Install the APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📺 How to Set as Default Launcher (FireOS 5)

### Why Old Methods Fail on Updated FireOS 5:
* `pm disable-user --user 0 com.amazon.tv.launcher` throws:
  `java.lang.SecurityException: Permission Denial: attempt to change component state`
* Amazon intentionally hides third-party services in **Settings > Accessibility** to block accessibility-based hijackers.

### The Working Solution: Event-Stream `HomeWatcherService`
This launcher uses the **`READ_LOGS` Event Stream method** (the same architecture used by tools like *Launcher Manager FOS5* / *Wolf Launcher*).

`HomeWatcherService` runs a lightweight background stream listening directly to Android's `system` buffer (`ActivityManager`). The millisecond you press the physical **Home** button on your remote, the service intercepts `cat=[android.intent.category.HOME]` and snaps your custom launcher forward with zero window transition delay.

### Complete Setup Commands (Run Once via ADB):

```bash
# 1. Grant permission to read ActivityManager system logs
adb shell pm grant com.firetv.launcher android.permission.READ_LOGS

# 2. Grant window overlay permission via appops
adb shell appops set com.firetv.launcher SYSTEM_ALERT_WINDOW allow

# 3. Launch the app once to initialize the background service
adb shell am start -n com.firetv.launcher/.MainActivity
```

**That's it!** Press the **Home** button on your Fire TV remote—it will now open your custom launcher every time.

---

## ⚡ Pro Tip: Eliminate System Transition Delays

FireOS adds an artificial 300ms–500ms window cross-fade animation when switching apps. You can disable this system animation across your Fire TV for an instant, snappier experience:

```bash
# Disable window transition lag (instant snap)
adb shell settings put global transition_animation_scale 0.0

# Cut general window and animator durations in half
adb shell settings put global window_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
```

---

## 🎵 Installing Spotify on FireOS 5

Modern Spotify releases require Android 7.0+, but you can sideload the official TV build that still supports **Android 5.0+ (API 21 / Lollipop)**:

1. Download **Spotify: Music & Podcasts (Android TV) — Version 1.80.2** (`arm-v7a` or `universal`) from APKMirror.
2. Install via ADB:
   ```bash
   adb install -r Spotify_TV_1.80.2.apk
   ```
3. Spotify will immediately appear in your launcher's **"All Applications"** section.
4. Press the **Menu key (☰)** on Spotify and select **"Add to Favorites"** to pin it to your centered home screen!
