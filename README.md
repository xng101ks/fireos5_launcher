# FireTV Custom Launcher (ATV Launcher Pro Style)

A high-performance, lightweight custom launcher designed specifically for **Amazon Fire TV sticks running FireOS 5** (Android 5.1 Lollipop, API 22).

---

## ✨ Features & ATV Launcher Pro Styling

* **ATV Launcher Pro Aesthetic**:
  * Deep cinematic dark gradient background (`#141720` -> `#0a0b10`).
  * Modern top status bar with a digital clock (`HH:mm`), date, and system settings shortcut.
  * Rounded translucent app tiles with app icons and labels.
* **D-Pad Focus Engine**:
  * Smooth animated scaling (1.10x) on focus with subtle deceleration curve.
  * Vibrant electric cyan focus glow ring (`#00E5FF`) and elevated drop shadow.
  * Label marquee animation activates exclusively when focused.
* **FireTV Remote Handling**:
  * **Select / D-pad Center**: Launches the selected app.
  * **Menu Key (☰) / Long-press Select**: Opens the App Options dialog (Open, App Info, Uninstall).
  * **Back Button**: Gracefully keeps the launcher active and refocuses the grid instead of quitting or showing a black screen.
* **Dynamic App Discovery**:
  * Queries both Leanback TV apps and sideloaded standard Android apps.
  * BroadcastReceiver automatically refreshes the grid when apps are installed or uninstalled.
* **Ultra-Lightweight**:
  * Built using pure Android Views, RecyclerView, and Kotlin Coroutines.
  * Optimized for low-RAM (1GB) Fire TV hardware.

---

## 🛠️ Project Structure

```
firetv-launcher/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/firetv/launcher/
│       │   ├── MainActivity.kt               # Main Launcher lifecycle & remote events
│       │   ├── adapter/AppGridAdapter.kt     # ATV tile adapter & focus listeners
│       │   ├── model/AppItem.kt              # App model
│       │   ├── service/FireTvAccessibilityService.kt # Fallback home button interceptor
│       │   └── util/
│       │       ├── AppLoader.kt              # Async package loader & deduplicator
│       │       └── DpadFocusHelper.kt        # Smooth scaling & elevation animator
│       └── res/
│           ├── drawable/                     # ATV Launcher Pro selectors & backgrounds
│           ├── layout/                       # activity_main & item_app_tile
│           └── values/                       # colors, dimens, strings, styles
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 🚀 Building & Installing

### 1. Open in Android Studio
1. Open **Android Studio**.
2. Select **Open an Existing Project** and choose `/home/peter/Desktop/firetv-launcher`.
3. Wait for Gradle sync to complete.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

### 2. Install to Fire TV via ADB
Make sure your computer and Fire TV stick are on the same Wi-Fi network.

```bash
# Connect to your Fire TV stick (replace IP with Fire TV's IP)
adb connect 192.168.1.xxx:5555

# Verify connection
adb devices

# Install the launcher APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📺 Making It Default on FireOS 5

Because FireOS intercepts the Home button, choose one of the following methods to activate your custom launcher:

### Option A: Disable Amazon Launcher via ADB (Recommended for FireOS 5)
FireOS 5 permits disabling the stock launcher package cleanly:

```bash
# Disable Amazon's default home screen
adb shell pm disable-user --user 0 com.amazon.tv.launcher
```

Now, pressing the **Home** button on your Fire TV remote will directly open your new custom launcher!

> **Note to revert**:
> ```bash
> adb shell pm enable com.amazon.tv.launcher
> ```

---

### Option B: Built-in Accessibility Service
If you do not want to disable the Amazon package, this launcher includes `FireTvAccessibilityService`:

1. Go to Fire TV **Settings > Accessibility**.
2. Turn on **FireTV Launcher Auto-Focus**.
3. Whenever FireOS attempts to launch the stock home screen on Home button press, the service immediately redirects focus back to your custom launcher.
