# AeroLight Android App — Setup Instructions

This project is code-complete: live charts, real-time Firebase sync, and
WARN/DANGER notifications are all wired up.

## Option A — Build the APK from your phone only (GitHub cloud build)

No computer or Android Studio needed. GitHub's servers compile the APK
for you; you just download the finished file.

1. **Create a free GitHub account** at github.com (phone browser is fine).
2. **Create a new repository** — tap the "+" icon → "New repository".
   Name it `AeroLight`, keep it Public, create it.
3. **Upload this whole project folder** into that repo: on the repo
   page, tap "Add file" → "Upload files", then select every file/folder
   from this zip (keep the folder structure — GitHub preserves it when
   you drag a whole folder in, or upload folder-by-folder if your
   browser only lets you pick files). This includes the hidden
   `.github/workflows/build.yml` file — make sure it's included, it's
   what tells GitHub how to build the app.
4. **Add your Firebase config file**: go to the Firebase console →
   your `aerolight-eth` project → Project settings (gear icon) →
   "Your apps" → Add app → Android → package name `com.aerolight.app`
   → download `google-services.json`. Upload that file into the
   `app/` folder of your GitHub repo (same level as `build.gradle.kts`).
5. **Trigger the build**: on your repo page, tap the "Actions" tab →
   "Build AeroLight APK" → "Run workflow" → "Run workflow" again to
   confirm. Wait 3-5 minutes (refresh to check progress — a green
   checkmark means it succeeded).
6. **Download the APK**: tap the completed run → scroll to
   "Artifacts" → tap `AeroLight-debug-apk` to download a zip
   containing your `app-debug.apk`.
7. **Install it**: extract that zip with your file manager, tap
   `app-debug.apk`, allow "install unknown apps" if prompted, and
   install. Done — real native app, no computer used.

If step 5 fails (red X), tap into the run to see the error — the most
common cause is a missing or misplaced `google-services.json` (step 4)
or the field names in `MainActivity.kt` not matching your device's
actual data (see step 3 below).

## Option B — Build from a computer with Android Studio

## 1. Open the project
- Install [Android Studio](https://developer.android.com/studio) if you don't have it.
- File → Open → select the unzipped `AeroLight` folder.
- Let Gradle sync (first sync can take a few minutes).

## 2. Add your Firebase config file
- Go to the [Firebase console](https://console.firebase.google.com) → your `aerolight-eth` project.
- Project settings (gear icon) → scroll to "Your apps" → Add app → Android.
- Package name: `com.aerolight.app` (must match exactly).
- Download the generated **`google-services.json`** file.
- Drop it into `AeroLight/app/google-services.json` (same folder as `build.gradle.kts`).

This file is what actually authorizes the app to talk to your database —
the `databaseUrl` in `MainActivity.kt` is already set to
`https://aerolight-eth-default-rtdb.firebaseio.com`.

## 3. Match the data field names
Open `app/src/main/java/com/aerolight/app/MainActivity.kt`, look at
`parseSnapshot()`. It currently expects your device to write JSON like:

```json
{
  "temperature": 24.5,
  "humidity": 55.0,
  "gas": 320.0,
  "status": "OK"
}
```

Open your Realtime Database tab in the Firebase console and check the
actual key names your ESP32/Arduino code writes. If they differ (e.g.
`"temp"` instead of `"temperature"`, or a different node than
`sensorData`), update the field names and `dataPath` in `MainActivity.kt`
to match. If your device doesn't write a `status` field, the app falls
back to computing it from the gas value via `deriveStatus()` — tune
those thresholds (currently WARN at 400, DANGER at 700) to your sensor's
actual range.

## 4. Check Realtime Database rules
For a first test, make sure your database rules allow reads (Firebase
console → Realtime Database → Rules). For anything beyond local testing,
lock this down again with proper auth rather than leaving it open.

## 5. Build the APK
- Build menu → Build Bundle(s) / APK(s) → Build APK(s).
- Once it finishes, click "locate" in the notification, or find it at
  `app/build/outputs/apk/debug/app-debug.apk`.
- Copy that file to your phone (or use Android Studio's Run ▶ button
  with your phone connected via USB debugging) and install it.

## What's already done for you
- ✅ Live line charts for temperature, humidity, and gas (MPAndroidChart)
- ✅ Real-time listener on your Firebase Realtime Database
- ✅ Notification channel + alert firing when status flips to WARN/DANGER
  (only fires once per status change, not on every reading)
- ✅ Android 13+ notification permission request handled
- ✅ Basic status-colored UI (green/amber/red)

## Common first-run issues
- **"Default FirebaseApp is not initialized"** → `google-services.json`
  is missing or in the wrong folder (step 2).
- **Charts stay empty** → your device's field names don't match
  `parseSnapshot()` (step 3) — print a snapshot's raw JSON via Logcat
  to check.
- **No notifications appear** → on Android 13+, check the permission was
  actually granted (Settings → Apps → AeroLight → Notifications).
