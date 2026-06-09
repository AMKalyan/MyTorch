# Original User Request

## Initial Request — 2026-06-09T16:47:59+05:30

Build a complete Android flashlight app ("MyTorch") that toggles the device's camera flash LED via a double-shake gesture or a manual tap on a power button. The shake detector must run as an always-on foreground service that works with the screen off and auto-restarts on device reboot. The app should feel stable, polished, and production-worthy.

Working directory: c:\Users\moksh\AndroidStudioProjects\MyTorch
Integrity mode: demo

**Existing project**: A fresh Android Studio project already exists at the working directory with Jetpack Compose set up (Kotlin, AGP 9.2.1, Compose BOM 2026.02.01, minSdk 24, targetSdk 36). The package name is `com.example.mytorch`. Use the existing project structure — do NOT create a new project from scratch.

**Technology constraints**: Pure Kotlin + Android framework APIs only. No third-party libraries, no Hilt/Dagger, no Room, no annotation processors. Use Jetpack Compose for the UI. All dependencies needed are already in the project's version catalog.

**Reference**: Use the implementation plan at `C:\Users\moksh\.gemini\antigravity\brain\877c6523-5ae3-4ce4-b9de-848726909b37\implementation_plan.md` as a detailed design reference for architecture decisions, algorithm specifics, and UI design. Follow it closely.

## Requirements

### R1. Torch Control
The app must toggle the device's rear camera flash LED on and off. Use Android's `CameraManager.setTorchMode()` API. The app must track the torch's actual state (including changes made by other apps) by registering a `TorchCallback`. If the device lacks a camera flash, the app must display a clear message and gracefully disable torch-related features without crashing.

### R2. Double-Shake Gesture Detection
The app must detect a "double shake" gesture using the device's accelerometer. A valid trigger is 2 distinct shake motions within a 1-second window. Individual shakes must be debounced with a minimum 200ms gap between them. After a successful toggle, enforce a 1-second cooldown before accepting new shake events. The shake threshold must be tuned to avoid false triggers from walking, driving, or normal phone handling. Provide subtle haptic feedback (short vibration) on each successful toggle.

### R3. Always-On Foreground Service
Shake detection must work even when the app is in the background or the screen is off. Implement this as an Android foreground service with a persistent notification. The notification must include a "Toggle" action button that lets the user turn the torch on/off directly from the notification shade without opening the app. The notification content should reflect the current torch state (ON/OFF). Use a partial wake lock to keep the accelerometer alive when the screen is off.

### R4. Boot Persistence
When the user has enabled the background shake service, it must automatically restart after a device reboot. Persist the user's service-enabled preference using SharedPreferences and register a BOOT_COMPLETED BroadcastReceiver to restart the service on boot.

### R5. User Interface
Build a single-screen UI with Jetpack Compose that includes:
- A large, prominent power button (~180dp) as the central element
- Dark-only theme (near-black background, no light mode)
- When torch is OFF: the power button shows as a dim outline with subtle border
- When torch is ON: the power button glows warm amber/yellow with a pulse animation and shadow effect
- Smooth animated transitions between ON/OFF states (color, glow, scale)
- A status text ("TORCH ON" / "TORCH OFF") with animated color
- A small hint text explaining the shake gesture
- A toggle switch to enable/disable the background shake service
- The UI must stay in sync with the actual torch state at all times (including when toggled via shake or notification)

### R6. Permissions & Manifest
Declare all required permissions (CAMERA, VIBRATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, POST_NOTIFICATIONS). Declare `android.hardware.camera.flash` as a non-required feature. Register the service and broadcast receivers in the manifest. Request POST_NOTIFICATIONS permission at runtime on Android 13+. Use `foregroundServiceType="specialUse"` for the service on Android 14+.

## Acceptance Criteria

### Build Verification
- [ ] The project compiles successfully with `gradlew assembleDebug` with zero errors
- [ ] No unresolved import statements or missing dependencies
- [ ] No Kotlin compiler warnings related to deprecated API usage

### Code Quality
- [ ] All Kotlin source files compile without syntax errors
- [ ] ShakeDetector, TorchManager, TorchService, and UI components are in separate files with clear single-responsibility
- [ ] The shake detection algorithm implements all specified parameters: 1-second window, 200ms per-shake debounce, 1-second post-toggle cooldown, and configurable threshold
- [ ] CameraAccessException and other potential exceptions are caught and handled gracefully — the app must never crash from a torch operation failure
- [ ] The foreground service properly acquires and releases its wake lock in onStartCommand/onDestroy
- [ ] The BootReceiver checks SharedPreferences before starting the service

### Functional Correctness
- [ ] The AndroidManifest.xml declares all 7 required permissions and both receivers
- [ ] The foreground service creates a notification channel and calls startForeground() before the timeout
- [ ] The notification includes a working "Toggle" action button via PendingIntent
- [ ] The TorchCallback is registered to track external torch state changes
- [ ] The Compose UI observes torch state reactively (state flows or equivalent) — no polling
- [ ] The power button has animated transitions between ON (amber glow + pulse) and OFF (dim outline) states
- [ ] The service toggle switch persists its state across app restarts via SharedPreferences
- [ ] The `uses-feature` for camera flash has `android:required="false"` and the app checks for flash availability at runtime
