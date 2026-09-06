# NSE F&O Options Scanner & Trading Terminal

Automated NSE F&O algorithmic scanner with high-confluence Intraday (15m ORB + VWAP) and Positional (Price & OI co-expansion) signal execution, Direct FCM instant wake-up alerting, and Android Jetpack Compose client terminal.

---

## 🚀 How to Build the APK on GitHub

This repository includes a complete Android Gradle project and a ready-to-run **GitHub Actions Workflow** (`.github/workflows/build-apk.yml`) that automatically builds the APK. No Firebase file, API key, signing key, or repository secret is required for the debug build.

### Step 1: Push this Project to GitHub
1. In Google AI Studio, click the **GitHub** button in the top navigation bar.
2. Select or connect your GitHub account and repository.
3. Push the code to your repository's `main` branch.

### Step 2: Automatic APK Build
- Whenever you push code to GitHub, GitHub Actions will **automatically start building the APK**.
- You can monitor the live build progress by clicking on the **Actions** tab in your GitHub repository.

### Step 3: Trigger Build Manually (Optional)
If you want to trigger a fresh build at any time:
1. Go to your repository on GitHub.
2. Click the **Actions** tab.
3. In the left sidebar, click **"Build Android APK"**.
4. Click the **"Run workflow"** button on the right, select `main` branch, and click **"Run workflow"**.

### Step 4: Download Your APK
1. When the workflow run completes (with a green checkmark ✅), click on the completed run (e.g., *"Build Debug APK"*).
2. Scroll down to the **Artifacts** section at the bottom of the summary page.
3. Click on **`minimum-loss-strategy-debug-apk`** to download the ZIP file.
4. Extract the ZIP to get your `app-debug.apk` file!

### Step 5: Install on Android Phone
1. Transfer `app-debug.apk` to your phone (via USB, Google Drive, Telegram, or direct download).
2. Tap the APK file to install.
3. If prompted with *"Install unknown apps"*, toggle **Allow from this source**.
4. Open the **NSE F&O Scanner** app!

### Local build

Open the repository root in Android Studio, allow Gradle Sync to finish, and select **Build → Build APK(s)**. The project uses JDK 17, Android SDK 35, and Gradle 8.11.1.

---

## 📱 Features

- **Intraday Engine (MIS)**: 15-minute ORB breakouts with VWAP confirmation, volume spike (>2.5x), tight SL (<5% premium risk), and 1:2.5 targets.
- **Positional Engine (NRML)**: Price and OI co-expansion (Long Buildup), monthly expiry contracts, and 1:4 risk-reward ratios.
- **Direct FCM & WakeLock**: High-priority instant wake-up alerts that bypass Android Doze mode and wake locked devices.
- **1-Click Broker Integration**: Pre-formatted order execution payloads for SmartAPI and Zerodha Kite.
