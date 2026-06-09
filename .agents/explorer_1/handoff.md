# Handoff Report — Explorer 1: Core Logic Files

**Type**: Hard handoff  
**Scope**: `ShakeDetector.kt`, `TorchManager.kt`, `TorchService.kt`  
**Package**: `com.example.mytorch`  
**Target path**: `app/src/main/java/com/example/mytorch/`

---

## 1. Observation

### Project State
- **Fresh template project** — only `MainActivity.kt` exists with a "Hello Android" Greeting composable.
- **minSdk = 24**, **targetSdk = 36**, **compileSdk 36** (AGP 9.2.1, Kotlin 2.2.10).
- **No existing core logic files** — all three are NEW files.
- **Manifest** has no permissions, services, or receivers declared yet.
- **Dependencies**: `core-ktx 1.10.1`, `lifecycle-runtime-ktx 2.6.1`, `activity-compose 1.8.0`, Compose BOM `2026.02.01`. No third-party libs allowed.

### Key API Availability (minSdk 24)
| API | Min API | Available? |
|---|---|---|
| `CameraManager.setTorchMode()` | 23 | ✅ |
| `CameraManager.TorchCallback` | 23 | ✅ |
| `NotificationChannel` | 26 | ⚠️ Must guard with `Build.VERSION.SDK_INT >= 26` — wait, minSdk is 24, so YES guard is needed |
| `Vibrator.vibrate(VibrationEffect)` | 26 | ⚠️ Must guard; use deprecated `vibrate(long)` for API 24-25 |
| `VibratorManager` | 31 | ⚠️ Must guard; fallback to `context.getSystemService(Vibrator::class.java)` |
| `startForeground(id, notification, type)` (3-arg with serviceType) | 34 | ⚠️ Must guard; use 2-arg on API < 34 |
| `PendingIntent.FLAG_IMMUTABLE` | 23 | ✅ |
| `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` | 34 | ⚠️ Use constant value `0x40000000` or guard |

**CORRECTION on NotificationChannel**: minSdk is 24, but `NotificationChannel` is API 26. However, since foreground services on API 26+ REQUIRE a channel, and on API 24-25 channels are ignored, the code MUST create the channel conditionally (if `Build.VERSION.SDK_INT >= Build.VERSION_CODES.O`). Actually, on API 24-25, `startForeground()` still works with a notification that has no channel. The notification just needs a valid `Notification` object. The channel creation should be guarded.

---

## 2. Logic Chain

### 2a. Evidence → Conclusion for each design decision
1. **minSdk 24** (from `build.gradle.kts:16`) means we cannot unconditionally use `NotificationChannel` (API 26) or `VibrationEffect` (API 26) or `VibratorManager` (API 31) — version guards are mandatory.
2. Implementation plan (line 86-88) specifies `ShakeDetector` takes `SensorManager` and a callback — dependency injection via constructor, NOT via `Context`.
3. Implementation plan (line 93-97) specifies `TorchManager` wraps `CameraManager` — similarly injected.
4. Implementation plan (line 107) mentions `LocalBroadcastManager` but that is **deprecated** and requires the `localbroadcastmanager` library not in the deps. The plan also mentions "or a custom broadcast." Since no third-party libs are allowed, use a **`MutableStateFlow`** exposed as `StateFlow` for state broadcasting (available via `kotlinx.coroutines.flow` which ships with `lifecycle-runtime-ktx`).
5. Implementation plan (line 108) specifies `WAKE_LOCK` (partial) — `PowerManager.PARTIAL_WAKE_LOCK` is the correct type. The wake lock must be acquired in `onStartCommand()` and released in `onDestroy()`.

---

## 3. Detailed Implementation Strategy

---

### FILE 1: `ShakeDetector.kt`

**Path**: `app/src/main/java/com/example/mytorch/ShakeDetector.kt`

#### Imports
```kotlin
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
```

#### Class Structure
```
class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShakeDetected: () -> Unit
) : SensorEventListener
```

#### Companion Object Constants
```kotlin
companion object {
    /** Acceleration threshold above gravity to count as a shake */
    const val SHAKE_THRESHOLD = 12.0f          // m/s²
    /** Minimum gap between individual shake peaks */
    const val SHAKE_DEBOUNCE_MS = 200L          // ms
    /** Time window in which N shakes must occur */
    const val SHAKE_WINDOW_MS = 1000L           // ms
    /** Number of shakes required to fire the callback */
    const val SHAKE_COUNT_REQUIRED = 2
    /** Cooldown after a successful trigger before accepting new shakes */
    const val COOLDOWN_MS = 1000L               // ms
    /** Approximate value of gravitational acceleration */
    private const val GRAVITY = 9.81f
}
```

#### Mutable State
```kotlin
private var shakeCount: Int = 0
private var firstShakeTimestamp: Long = 0L   // SystemClock.elapsedRealtime() of first shake in current window
private var lastShakeTimestamp: Long = 0L    // timestamp of most recent shake peak
private var lastTriggerTimestamp: Long = 0L  // timestamp of last successful trigger (for cooldown)
```

#### `start()` Method
```kotlin
fun start(): Boolean {
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        ?: return false  // device has no accelerometer
    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    return true
}
```
- Returns `Boolean` to signal whether accelerometer is available.
- `SENSOR_DELAY_UI` (~60 Hz) is sufficient for gesture detection without excessive battery drain.

#### `stop()` Method
```kotlin
fun stop() {
    sensorManager.unregisterListener(this)
    // Reset state
    shakeCount = 0
    firstShakeTimestamp = 0L
    lastShakeTimestamp = 0L
}
```

#### `onSensorChanged(event: SensorEvent)` — The Core Algorithm
```kotlin
override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

    val x = event.values[0]
    val y = event.values[1]
    val z = event.values[2]

    // Compute acceleration magnitude minus gravity
    val magnitude = sqrt(x * x + y * y + z * z) - GRAVITY

    if (magnitude < SHAKE_THRESHOLD) return

    val now = android.os.SystemClock.elapsedRealtime()

    // Check cooldown
    if (now - lastTriggerTimestamp < COOLDOWN_MS) return

    // Check debounce (ignore peaks too close together)
    if (now - lastShakeTimestamp < SHAKE_DEBOUNCE_MS) return

    lastShakeTimestamp = now

    // Start new window or continue existing one
    if (shakeCount == 0) {
        firstShakeTimestamp = now
    }

    // Check if window expired → reset
    if (now - firstShakeTimestamp > SHAKE_WINDOW_MS) {
        shakeCount = 1
        firstShakeTimestamp = now
    } else {
        shakeCount++
    }

    // Check if we hit the required count
    if (shakeCount >= SHAKE_COUNT_REQUIRED) {
        shakeCount = 0
        firstShakeTimestamp = 0L
        lastTriggerTimestamp = now
        onShakeDetected()
    }
}
```

#### `onAccuracyChanged` — Required but no-op
```kotlin
override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // No-op
}
```

#### Thread Safety Considerations
- `onSensorChanged` is called on the **sensor thread** (typically a background handler thread), NOT the main thread.
- The `onShakeDetected` callback will therefore be invoked on the sensor thread.
- The caller (TorchService) must ensure any UI-touching work is dispatched to the main thread. Since `TorchManager.toggle()` calls `CameraManager.setTorchMode()`, which is thread-safe, this is fine for the toggle itself.
- The `MutableStateFlow.value` setter is also thread-safe.
- **No synchronization needed** within ShakeDetector itself because `onSensorChanged` is called sequentially on the same sensor thread.

#### Pitfalls & Gotchas
| Issue | Detail |
|---|---|
| **Gravity subtraction is approximate** | Using a constant `9.81` works for magnitude thresholds but doesn't account for the phone's actual gravitational reading on each axis. A high-pass filter would be more accurate but the constant subtraction is sufficient for this threshold-based approach. |
| **`SystemClock.elapsedRealtime()`** | Use this, NOT `System.currentTimeMillis()`, because `elapsedRealtime()` is monotonic and unaffected by wall-clock changes. |
| **Sensor availability** | Always null-check `getDefaultSensor()`. Some devices (e.g., emulators, cheap tablets) lack an accelerometer. |
| **Sensor batching** | On some devices, `SENSOR_DELAY_UI` events can be batched. The algorithm handles this because it uses timestamps, not event frequency. |

---

### FILE 2: `TorchManager.kt`

**Path**: `app/src/main/java/com/example/mytorch/TorchManager.kt`

#### Imports
```kotlin
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
```

#### Class Structure
```
class TorchManager(private val cameraManager: CameraManager) {
    ...
}
```

#### Properties
```kotlin
// The camera ID for the rear-facing camera with flash
private var cameraId: String? = null

// Reactive state for UI observation
private val _torchState = MutableStateFlow(false)
val torchState: StateFlow<Boolean> = _torchState.asStateFlow()

// Whether the device has flash capability
val hasFlash: Boolean
    get() = cameraId != null
```

#### Initialization (init block)
```kotlin
init {
    // Find the first camera with flash support
    cameraId = findCameraWithFlash()

    // Register torch callback to track external state changes
    cameraId?.let { id ->
        cameraManager.registerTorchCallback(torchCallback, null)
        // Passing null for handler → callback on the calling thread's Looper,
        // or the main thread if no Looper is found
    }
}
```

#### `findCameraWithFlash()` — Private helper
```kotlin
private fun findCameraWithFlash(): String? {
    return try {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: CameraAccessException) {
        null
    }
}
```

#### Torch Callback
```kotlin
private val torchCallback = object : CameraManager.TorchCallback() {
    override fun onTorchModeChanged(camId: String, enabled: Boolean) {
        if (camId == cameraId) {
            _torchState.value = enabled
        }
    }

    override fun onTorchModeUnavailable(camId: String) {
        if (camId == cameraId) {
            _torchState.value = false
        }
    }
}
```
- **Critical**: The `TorchCallback` fires for ALL camera ID events, so always filter by `cameraId`.
- `onTorchModeUnavailable` is called when another app opens the camera exclusively.

#### `toggle()` Method
```kotlin
fun toggle() {
    val newState = !_torchState.value
    if (newState) turnOn() else turnOff()
}
```

#### `turnOn()` Method
```kotlin
fun turnOn() {
    val id = cameraId ?: return
    try {
        cameraManager.setTorchMode(id, true)
        // Note: _torchState will be updated by the TorchCallback
    } catch (e: CameraAccessException) {
        _torchState.value = false
    }
}
```

#### `turnOff()` Method
```kotlin
fun turnOff() {
    val id = cameraId ?: return
    try {
        cameraManager.setTorchMode(id, false)
    } catch (e: CameraAccessException) {
        // Already off or camera unavailable — safe to ignore
        _torchState.value = false
    }
}
```

#### `release()` Method — Cleanup
```kotlin
fun release() {
    cameraManager.unregisterTorchCallback(torchCallback)
    turnOff()
}
```

#### Thread Safety Considerations
- `MutableStateFlow.value` is **thread-safe** (atomic CAS under the hood).
- `CameraManager.setTorchMode()` is **thread-safe** — documented as safe to call from any thread.
- `TorchCallback` is invoked on the handler thread passed to `registerTorchCallback()`. Passing `null` delivers on the main thread. This is fine since we only update a `StateFlow`.
- **No manual synchronization needed**.

#### Error Handling Strategy
| Scenario | Handling |
|---|---|
| No camera with flash | `cameraId = null`, `hasFlash = false`, all toggle methods are no-ops via early return |
| `CameraAccessException` on `setTorchMode` | Catch, set state to `false`, log silently |
| Another app holds camera | `onTorchModeUnavailable` callback updates state to `false` |
| Camera disconnected | `CameraAccessException` caught in toggle methods |

#### Pitfalls & Gotchas
| Issue | Detail |
|---|---|
| **`registerTorchCallback` fires immediately** | On registration, it fires `onTorchModeChanged` with the current state for each camera. This is good — it initializes `_torchState` correctly. |
| **Multiple cameras** | Some devices have multiple cameras with flash. We pick the FIRST one. This is standard practice. |
| **State race** | Don't read `_torchState.value` right after `setTorchMode` — the callback may not have fired yet. Use the callback-driven state, not a local boolean. |
| **`CameraCharacteristics.FLASH_INFO_AVAILABLE`** | This key may be `null` on some devices. The `== true` comparison safely handles null as false. |

---

### FILE 3: `TorchService.kt`

**Path**: `app/src/main/java/com/example/mytorch/TorchService.kt`

This is the most complex file. It orchestrates `ShakeDetector`, `TorchManager`, notification management, vibration feedback, and wake lock.

#### Imports
```kotlin
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
```

#### Class Structure
```
class TorchService : Service() {
    ...
}
```

#### Companion Object
```kotlin
companion object {
    const val CHANNEL_ID = "torch_service_channel"
    const val NOTIFICATION_ID = 1
    const val ACTION_TOGGLE = "com.example.mytorch.ACTION_TOGGLE"
    const val VIBRATE_DURATION_MS = 50L
    private const val WAKE_LOCK_TAG = "MyTorch::ShakeDetector"

    // Static StateFlow so MainActivity can observe without binding
    private val _torchState = MutableStateFlow(false)
    val torchState: StateFlow<Boolean> = _torchState.asStateFlow()
}
```

**Design Decision — Static StateFlow**: Using a `companion object` StateFlow lets `MainActivity` observe torch state without `bindService()`. This avoids the complexity of a bound service + Binder. The Flow is updated whenever `TorchManager.torchState` changes.

#### Private Fields
```kotlin
private var shakeDetector: ShakeDetector? = null
private var torchManager: TorchManager? = null
private var wakeLock: PowerManager.WakeLock? = null
```

#### `onCreate()`
```kotlin
override fun onCreate() {
    super.onCreate()

    // 1. Create notification channel (API 26+)
    createNotificationChannel()

    // 2. Initialize TorchManager
    val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    torchManager = TorchManager(cameraManager)

    // 3. Initialize ShakeDetector
    val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    shakeDetector = ShakeDetector(sensorManager) {
        // Callback — runs on sensor thread
        torchManager?.toggle()
        vibrateOnToggle()
        updateNotification()
    }

    // 4. Observe TorchManager state and relay to companion StateFlow
    // Since we can't use coroutines without a scope easily here,
    // we'll use a simpler approach: collect in the TorchCallback.
    // Actually, TorchManager already exposes StateFlow, and the 
    // TorchCallback updates it. We should launch a coroutine to 
    // collect torchManager.torchState and relay to _torchState.
    //
    // BUT: lifecycle-runtime-ktx is available, so we can use
    // kotlinx.coroutines.CoroutineScope + Dispatchers.Main.
    // SEE: torchStateCollectionJob below.
}
```

**Important coroutine setup note**: The Worker should create a `CoroutineScope(SupervisorJob() + Dispatchers.Main)` as a class property and cancel it in `onDestroy()`. Use this scope to collect `torchManager!!.torchState` and relay to the companion `_torchState`, as well as to update the notification.

```kotlin
// As a class property:
private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

// In onCreate(), after torchManager init:
serviceScope.launch {
    torchManager!!.torchState.collect { isOn ->
        _torchState.value = isOn
        updateNotification()
    }
}
```

**Additional import needed**: 
```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
```

#### `onStartCommand()`
```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Handle toggle action from notification
    if (intent?.action == ACTION_TOGGLE) {
        torchManager?.toggle()
        vibrateOnToggle()
        return START_STICKY
    }

    // Start as foreground service
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
        startForeground(NOTIFICATION_ID, notification)
    }

    // Acquire partial wake lock
    acquireWakeLock()

    // Start shake detection
    shakeDetector?.start()

    return START_STICKY
}
```

**`START_STICKY`**: If the system kills the service, it will restart it. The service will be restarted with a null intent.

#### `onBind()` — Returns null (not a bound service)
```kotlin
override fun onBind(intent: Intent?): IBinder? = null
```

#### `onDestroy()`
```kotlin
override fun onDestroy() {
    // 1. Stop shake detection
    shakeDetector?.stop()
    shakeDetector = null

    // 2. Turn off torch and release camera resources
    torchManager?.release()
    torchManager = null

    // 3. Release wake lock
    releaseWakeLock()

    // 4. Cancel coroutine scope
    serviceScope.cancel()

    // 5. Reset static state
    _torchState.value = false

    super.onDestroy()
}
```

#### `createNotificationChannel()` — Private
```kotlin
private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Torch Service",
            NotificationManager.IMPORTANCE_LOW  // LOW = no sound, no heads-up
        ).apply {
            description = "Keeps the shake-to-toggle service running"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
```

#### `buildNotification()` — Private
```kotlin
private fun buildNotification(): Notification {
    // Toggle action PendingIntent → routes to NotificationActionReceiver
    val toggleIntent = Intent(this, NotificationActionReceiver::class.java).apply {
        action = ACTION_TOGGLE
    }
    val togglePendingIntent = PendingIntent.getBroadcast(
        this,
        0,
        toggleIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Tap notification → opens MainActivity
    val contentIntent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
        this,
        1,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val isOn = _torchState.value
    val statusText = if (isOn) "Torch is ON" else "Torch is OFF"
    val actionLabel = if (isOn) "Turn OFF" else "Turn ON"

    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(this)
    }

    return builder
        .setContentTitle("MyTorch")
        .setContentText(statusText)
        .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a built-in icon; replace with custom later
        .setContentIntent(contentPendingIntent)
        .setOngoing(true)
        .addAction(
            Notification.Action.Builder(
                null, // No icon for the action (modern Android)
                actionLabel,
                togglePendingIntent
            ).build()
        )
        .build()
}
```

**Note on icons**: Using `android.R.drawable.ic_dialog_info` as a placeholder. A custom drawable (e.g., `R.drawable.ic_flashlight`) can be added later. The Worker should use a system icon for now.

#### `updateNotification()` — Private
```kotlin
private fun updateNotification() {
    val manager = getSystemService(NotificationManager::class.java)
    manager.notify(NOTIFICATION_ID, buildNotification())
}
```

#### `vibrateOnToggle()` — Private
```kotlin
private fun vibrateOnToggle() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26
        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(VIBRATE_DURATION_MS)
    }
}
```

#### `acquireWakeLock()` / `releaseWakeLock()` — Private
```kotlin
private fun acquireWakeLock() {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock = pm.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        WAKE_LOCK_TAG
    ).apply {
        acquire() // No timeout — held until explicitly released
    }
}

private fun releaseWakeLock() {
    wakeLock?.let {
        if (it.isHeld) {
            it.release()
        }
    }
    wakeLock = null
}
```

**⚠️ Critical Wake Lock Note**: Calling `acquire()` without a timeout is flagged by lint (`WakelockTimeout`). However, for this use case (always-on shake detection), an indefinite partial wake lock is intentional and correct. The Worker should suppress the lint warning with `@SuppressLint("WakelockTimeout")` on the `acquireWakeLock()` method.

#### Thread Safety Considerations
| Aspect | Thread | Safety |
|---|---|---|
| `onSensorChanged` callback from ShakeDetector | Sensor thread | `torchManager?.toggle()` is thread-safe (see TorchManager analysis) |
| `TorchCallback.onTorchModeChanged` | Main thread (null handler) | `StateFlow.value` is atomic |
| `updateNotification()` from sensor thread | Sensor thread | `NotificationManager.notify()` is thread-safe |
| `vibrateOnToggle()` from sensor thread | Sensor thread | `Vibrator.vibrate()` is thread-safe |
| `onStartCommand()` with ACTION_TOGGLE | Main thread | No conflict — `toggle()` is thread-safe |

The design is **thread-safe without explicit synchronization** because:
1. All shared mutable state uses `StateFlow` (atomic).
2. All Android system service calls (`setTorchMode`, `notify`, `vibrate`) are documented as thread-safe.
3. `ShakeDetector` internal state is only accessed from the single sensor thread.

#### API Version Guard Summary
```
API 24-25: No NotificationChannel, use deprecated Notification.Builder(context),
           use deprecated Vibrator.vibrate(long), use deprecated getSystemService(VIBRATOR_SERVICE)
API 26-30: NotificationChannel, VibrationEffect, Notification.Builder(context, channelId)
API 31+:   VibratorManager replaces Vibrator systemService
API 34+:   startForeground(id, notification, serviceType) 3-arg overload required
```

---

## 4. Caveats

| Caveat | Detail |
|---|---|
| **No binding** | This design uses a static `StateFlow` in `companion object` instead of a bound service. This is simpler but means there's only ONE service instance's state shared globally. Acceptable for a single-screen app. |
| **LocalBroadcastManager skipped** | The implementation plan mentioned it but it's deprecated and requires an extra dependency. `StateFlow` is the correct modern replacement. |
| **Notification icon** | Using `android.R.drawable.ic_dialog_info` as placeholder. The Worker should note this for eventual replacement with a custom icon. |
| **`Notification.Builder` vs `NotificationCompat.Builder`** | We use `android.app.Notification.Builder` (framework) instead of `androidx.core.app.NotificationCompat.Builder` because `core-ktx 1.10.1` doesn't include the compat builder separately, and the framework builder with version guards is sufficient. However, if `NotificationCompat` is available via `core-ktx`, the Worker MAY prefer it to reduce version guards. The Worker should check. |
| **Coroutine dependency** | `kotlinx.coroutines` is available transitively via `lifecycle-runtime-ktx`. If a build error occurs, add `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")` — but this is likely unnecessary. |
| **`@Suppress("DEPRECATION")`** | Multiple deprecation suppressions are needed for pre-API-26 vibration and pre-API-31 vibrator access. This is unavoidable with minSdk 24. |

---

## 5. Conclusion

The three core files form a clean layered architecture:

```
TorchService (orchestrator)
├── ShakeDetector (sensor input → gesture recognition)
├── TorchManager (camera output → flash control)
├── Notification (user-facing status)
├── Vibrator (haptic feedback)
└── WakeLock (keeps CPU alive for sensor)
```

**Key design principles for the Worker:**
1. **Constructor injection** for `ShakeDetector` (takes `SensorManager` + callback) and `TorchManager` (takes `CameraManager`) — enables testability.
2. **`StateFlow`** for all reactive state — no `LiveData`, no broadcasts, no listeners.
3. **Version guards everywhere** — minSdk 24 means 4 distinct API level brackets to handle.
4. **No coroutine complexity in ShakeDetector or TorchManager** — they are synchronous, stateless-callback-driven. Only `TorchService` uses a coroutine scope for state collection.
5. **Thread safety via design** — no explicit locks needed.

---

## 6. Verification Method

1. **Compile check**: After implementing all 3 files, run `./gradlew assembleDebug`. All imports should resolve without new dependencies.
2. **Lint check**: `./gradlew lint` — expect `WakelockTimeout` warning (suppressed intentionally). No other warnings expected.
3. **Manual test on physical device**: Emulators lack accelerometer and flash hardware. Must test on real device.
4. **Key behaviors to verify**:
   - Toggle via notification action works
   - Screen-off shake detection works (wake lock keeps sensor alive)
   - External torch changes (via system quick settings) update the StateFlow
   - No false triggers during walking
   - Cooldown prevents rapid re-triggers

---

## Appendix A: File Dependency Graph

```
ShakeDetector.kt  ──→  (no internal deps, only Android SDK)
TorchManager.kt   ──→  (no internal deps, only Android SDK + kotlinx.coroutines.flow)
TorchService.kt   ──→  ShakeDetector.kt, TorchManager.kt, 
                        NotificationActionReceiver (import for PendingIntent target),
                        MainActivity (import for content PendingIntent target)
```

**Build order**: ShakeDetector → TorchManager → TorchService (or all at once since Kotlin compiles the whole module).

## Appendix B: Complete Import Lists

### ShakeDetector.kt
```kotlin
package com.example.mytorch

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt
```

### TorchManager.kt
```kotlin
package com.example.mytorch

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
```

### TorchService.kt
```kotlin
package com.example.mytorch

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
```
