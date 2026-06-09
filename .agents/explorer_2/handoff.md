# Handoff Report — Explorer 2: UI & Theme Layer

**Summary**: The project is a default Android Studio template with a purple Material3 palette and boilerplate Greeting composable. Four files need modification/creation to build the torch UI: Color.kt (replace palette), Theme.kt (dark-only), TorchScreen.kt (new composable), and MainActivity.kt (complete rewrite). All Compose animation APIs needed are available via the existing BOM 2026.02.01. No new dependencies required.

---

## 1. Observation

### Current File State

| File | Path | Lines | Status |
|---|---|---|---|
| Color.kt | `app/src/main/java/com/example/mytorch/ui/theme/Color.kt` | 11 | Default purple palette (Purple80, PurpleGrey80, Pink80, Purple40, PurpleGrey40, Pink40) |
| Theme.kt | `app/src/main/java/com/example/mytorch/ui/theme/Theme.kt` | 58 | Has DarkColorScheme, LightColorScheme, dynamic color logic, `isSystemInDarkTheme` |
| Type.kt | `app/src/main/java/com/example/mytorch/ui/theme/Type.kt` | 34 | Default typography — **no changes needed** |
| MainActivity.kt | `app/src/main/java/com/example/mytorch/MainActivity.kt` | 47 | Boilerplate: Greeting composable, Scaffold, enableEdgeToEdge |
| TorchScreen.kt | `app/src/main/java/com/example/mytorch/ui/TorchScreen.kt` | — | **Does not exist yet** |

### Build Configuration
- **Compose BOM**: `2026.02.01` (from `gradle/libs.versions.toml` line 10)
- **minSdk**: 24 (from `app/build.gradle.kts` line 16)
- **targetSdk**: 36 (line 17)
- **Kotlin**: 2.2.10 with Compose compiler plugin
- **Dependencies already present**: `activity-compose`, `material3`, `ui`, `ui-graphics`, `ui-tooling-preview`, `core-ktx`, `lifecycle-runtime-ktx`
- **No new dependencies needed** — all animation, Canvas, and service APIs are in the existing deps

### AndroidManifest.xml (line 1-28)
- Currently only registers `MainActivity` with LAUNCHER intent filter
- No permissions, services, or receivers yet
- Uses `@style/Theme.MyTorch` (XML theme for splash — keep this)

---

## 2. Logic Chain

### 2.1 Color.kt — Complete Replacement

**What to do**: Delete all 6 existing color vals. Replace with 6 torch-themed colors.

**Exact file content** (replace lines 5–11):

```kotlin
package com.example.mytorch.ui.theme

import androidx.compose.ui.graphics.Color

val TorchAmber = Color(0xFFFFC107)
val TorchAmberDark = Color(0xFFFF8F00)
val SurfaceDark = Color(0xFF0D0D0D)
val SurfaceDarkElevated = Color(0xFF1A1A1A)
val OnSurfaceDim = Color(0xFF666666)
val OnSurfaceBright = Color(0xFFE0E0E0)
```

**Imports needed**: Only `androidx.compose.ui.graphics.Color` (already present).

**Rationale**: TorchAmber (0xFFFFC107) is Material amber-500 — a warm flashlight color. SurfaceDark (0xFF0D0D0D) is near-black for the torch-off aesthetic. OnSurfaceDim vs OnSurfaceBright provide contrast between OFF and ON states.

---

### 2.2 Theme.kt — Dark-Only, No Dynamic Colors

**What to do**: Strip all light-scheme logic, remove dynamic color support, simplify to a single dark color scheme.

**Exact file content** (complete replacement of lines 1–58):

```kotlin
package com.example.mytorch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TorchDarkColorScheme = darkColorScheme(
    primary = TorchAmber,
    onPrimary = Color.Black,
    secondary = TorchAmberDark,
    onSecondary = Color.Black,
    background = SurfaceDark,
    onBackground = OnSurfaceBright,
    surface = SurfaceDark,
    onSurface = OnSurfaceBright,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = OnSurfaceDim
)

@Composable
fun MyTorchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TorchDarkColorScheme,
        typography = Typography,
        content = content
    )
}
```

**Key changes**:
- Remove imports: `android.app.Activity`, `android.os.Build`, `isSystemInDarkTheme`, `dynamicDarkColorScheme`, `dynamicLightColorScheme`, `lightColorScheme`, `LocalContext`
- Add import: `androidx.compose.ui.graphics.Color` (for `Color.Black`)
- Remove `darkTheme` and `dynamicColor` parameters from `MyTorchTheme` — no longer needed
- Remove `LightColorScheme` val entirely
- Remove `when` block — directly use `TorchDarkColorScheme`
- Keep `typography = Typography` reference (from Type.kt, unchanged)

**Why no `LocalContext`**: Dynamic colors require context; we don't use dynamic colors, so no context needed.

---

### 2.3 TorchScreen.kt — New File

**File path**: `app/src/main/java/com/example/mytorch/ui/TorchScreen.kt`  
**Package**: `com.example.mytorch.ui`

#### 2.3.1 Complete Import List

```kotlin
package com.example.mytorch.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytorch.ui.theme.OnSurfaceBright
import com.example.mytorch.ui.theme.OnSurfaceDim
import com.example.mytorch.ui.theme.SurfaceDark
import com.example.mytorch.ui.theme.SurfaceDarkElevated
import com.example.mytorch.ui.theme.TorchAmber
import com.example.mytorch.ui.theme.TorchAmberDark
```

#### 2.3.2 Main Composable Signature

```kotlin
@Composable
fun TorchScreen(
    torchOn: Boolean,
    serviceEnabled: Boolean,
    hasFlash: Boolean,
    onToggle: () -> Unit,
    onServiceToggle: (Boolean) -> Unit
)
```

**Parameter contract**:
- `torchOn` — observed from StateFlow/BroadcastReceiver in MainActivity; drives all visual state
- `serviceEnabled` — from SharedPreferences (observed as mutableStateOf in MainActivity)
- `hasFlash` — checked once in onCreate via CameraManager; determines if controls are enabled
- `onToggle` — sends intent to TorchService to toggle torch
- `onServiceToggle` — starts/stops TorchService and persists preference

#### 2.3.3 Layout Structure

```
Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    // 1. Status Text
    // 2. Spacer(32.dp)
    // 3. Power Button (Canvas inside Box, 180.dp)
    // 4. Spacer(24.dp)
    // 5. Hint Text ("Shake to toggle")
    // 6. Spacer(48.dp)
    // 7. Service Toggle Row (label + Switch)
    // 8. Conditional: No-flash warning
}
```

#### 2.3.4 Component 1: Status Text

```kotlin
val statusColor by animateColorAsState(
    targetValue = if (torchOn) TorchAmber else OnSurfaceDim,
    animationSpec = tween(durationMillis = 300),
    label = "statusColor"
)

Text(
    text = if (torchOn) "TORCH ON" else "TORCH OFF",
    color = statusColor,
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 4.sp
)
```

**API**: `animateColorAsState` from `androidx.compose.animation`. Returns a `State<Color>` that smoothly transitions when `torchOn` changes. The `tween(300)` provides a 300ms linear interpolation.

#### 2.3.5 Component 2: Power Button with Glow/Pulse

This is the most complex component. It uses a `Canvas` inside a `Box` for custom drawing.

**Pulse animation setup** (only active when ON):

```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "pulse")
val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.7f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 1200, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
)
```

**Key design decision**: `rememberInfiniteTransition` runs continuously even when torch is OFF, but we only *use* `pulseAlpha` in the ON drawing path. This avoids conditional composition issues. The animation oscillates alpha between 0.3 and 0.7 over 1.2 seconds with reversal, creating a breathing glow.

**Button composable**:

```kotlin
val buttonColor by animateColorAsState(
    targetValue = if (torchOn) TorchAmber else SurfaceDarkElevated,
    animationSpec = tween(durationMillis = 300),
    label = "buttonColor"
)
val borderColor by animateColorAsState(
    targetValue = if (torchOn) TorchAmberDark else OnSurfaceDim,
    animationSpec = tween(durationMillis = 300),
    label = "borderColor"
)

Box(
    modifier = Modifier
        .size(180.dp)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,   // no ripple — custom visual feedback
            enabled = hasFlash,
            onClick = onToggle
        ),
    contentAlignment = Alignment.Center
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 8.dp.toPx()

        // Glow layer (ON state only)
        if (torchOn) {
            drawIntoCanvas { canvas ->
                val glowPaint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = TorchAmber.copy(alpha = pulseAlpha).toArgb()
                    setShadowLayer(
                        40.dp.toPx() * pulseAlpha,  // blur radius pulses
                        0f, 0f,
                        TorchAmber.copy(alpha = pulseAlpha).toArgb()
                    )
                }
                canvas.nativeCanvas.drawCircle(
                    center.x, center.y, radius, glowPaint
                )
            }
        }

        // Main circle fill
        drawCircle(
            color = buttonColor,
            radius = radius,
            center = center
        )

        // Border ring
        drawCircle(
            color = borderColor,
            radius = radius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )

        // Power icon (vertical line + arc)
        val iconColor = if (torchOn) Color.Black else OnSurfaceDim

        // Vertical line of power icon
        drawLine(
            color = iconColor,
            start = Offset(center.x, center.y - radius * 0.35f),
            end = Offset(center.x, center.y - radius * 0.05f),
            strokeWidth = 4.dp.toPx()
        )

        // Arc of power icon
        drawArc(
            color = iconColor,
            startAngle = -60f,         // starts upper-right
            sweepAngle = 300f,         // sweeps 300° leaving gap at top
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            size = androidx.compose.ui.geometry.Size(radius * 0.7f, radius * 0.7f),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}
```

**How the glow works**:
1. `drawIntoCanvas` drops into the native Android `Canvas` to use `Paint.setShadowLayer()`
2. `setShadowLayer(blurRadius, dx, dy, color)` creates a soft glow behind the circle
3. The `blurRadius` is multiplied by `pulseAlpha` (0.3→0.7), making the glow breathe
4. The glow color also uses `pulseAlpha` for its alpha, so it fades in/out
5. This approach works without hardware layer flags because Compose Canvas handles it

**Alternative approach if `setShadowLayer` doesn't render on some devices**: Use multiple `drawCircle` calls with decreasing alpha and increasing radius to simulate a radial glow:

```kotlin
// Fallback glow approach
if (torchOn) {
    for (i in 3 downTo 1) {
        drawCircle(
            color = TorchAmber.copy(alpha = pulseAlpha * 0.15f * i),
            radius = radius + (i * 12.dp.toPx()),
            center = center
        )
    }
}
```

#### 2.3.6 Component 3: Hint Text

```kotlin
Text(
    text = "Shake to toggle",
    color = OnSurfaceDim,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal
)
```

#### 2.3.7 Component 4: Service Toggle

```kotlin
Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(horizontal = 16.dp)
) {
    Text(
        text = "Background Service",
        color = OnSurfaceBright,
        fontSize = 16.sp,
        modifier = Modifier.weight(1f)
    )
    Switch(
        checked = serviceEnabled,
        onCheckedChange = onServiceToggle,
        enabled = hasFlash,
        colors = SwitchDefaults.colors(
            checkedThumbColor = TorchAmber,
            checkedTrackColor = TorchAmberDark.copy(alpha = 0.5f),
            uncheckedThumbColor = OnSurfaceDim,
            uncheckedTrackColor = SurfaceDarkElevated
        )
    )
}
```

#### 2.3.8 Component 5: No-Flash Warning

```kotlin
if (!hasFlash) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "⚠ No flash hardware detected",
        color = Color(0xFFFF5252),  // red accent
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
}
```

#### 2.3.9 Preview Composable

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun TorchScreenPreview() {
    MyTorchTheme {
        TorchScreen(
            torchOn = true,
            serviceEnabled = true,
            hasFlash = true,
            onToggle = {},
            onServiceToggle = {}
        )
    }
}
```

Needs additional import: `import androidx.compose.ui.tooling.preview.Preview` and `import com.example.mytorch.ui.theme.MyTorchTheme`.

---

### 2.4 MainActivity.kt — Complete Rewrite

**File path**: `app/src/main/java/com/example/mytorch/MainActivity.kt`  
**Package**: `com.example.mytorch`

#### 2.4.1 Complete Import List

```kotlin
package com.example.mytorch

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.mytorch.ui.TorchScreen
import com.example.mytorch.ui.theme.MyTorchTheme
```

#### 2.4.2 Class Structure

```kotlin
class MainActivity : ComponentActivity() {

    // --- State ---
    private var torchOn by mutableStateOf(false)
    private var serviceEnabled by mutableStateOf(false)
    private var hasFlash by mutableStateOf(true)

    // --- Preferences ---
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREFS_NAME = "mytorch_prefs"
        const val KEY_SERVICE_ENABLED = "service_enabled"
        const val ACTION_TORCH_STATE = "com.example.mytorch.TORCH_STATE"
        const val EXTRA_TORCH_ON = "torch_on"
        const val ACTION_TOGGLE_TORCH = "com.example.mytorch.TOGGLE_TORCH"
    }
```

**Key design decision**: Using `mutableStateOf` at the Activity level (not ViewModel) because:
1. No ViewModel dependency needed (architecture is intentionally simple)
2. Activity-scoped state is sufficient for a single-screen app
3. `mutableStateOf` is observable by Compose — recomposition happens automatically
4. The `by` delegate provides direct get/set syntax

#### 2.4.3 BroadcastReceiver for Torch State

```kotlin
    private val torchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_TORCH_STATE) {
                torchOn = intent.getBooleanExtra(EXTRA_TORCH_ON, false)
            }
        }
    }
```

**Communication pattern**: TorchService broadcasts torch state changes → MainActivity's BroadcastReceiver updates `torchOn` mutableStateOf → Compose automatically recomposes TorchScreen.

**Why BroadcastReceiver instead of bound service**: Simpler lifecycle management. The service runs independently (foreground), and broadcasts are fire-and-forget. No need to manage ServiceConnection binding/unbinding.

**Registration approach**: Use `ContextCompat.registerReceiver()` with `RECEIVER_NOT_EXPORTED` flag (API 33+ requirement, backwards compatible via compat library):

```kotlin
    // In onCreate or onStart:
    ContextCompat.registerReceiver(
        this,
        torchStateReceiver,
        IntentFilter(ACTION_TORCH_STATE),
        ContextCompat.RECEIVER_NOT_EXPORTED
    )

    // In onDestroy or onStop:
    unregisterReceiver(torchStateReceiver)
```

#### 2.4.4 Permission Request Flow (POST_NOTIFICATIONS)

```kotlin
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Permission result — service starts regardless, but notification
            // won't show on Android 13+ without permission.
            // No UI change needed; the service handles graceful degradation.
        }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
```

**Flow**:
1. `registerForActivityResult` is called at class level (before `onCreate` returns)
2. `requestNotificationPermissionIfNeeded()` is called in `onCreate`
3. On Android 12 and below (API < 33): no-op, notifications work without runtime permission
4. On Android 13+ (API 33 / TIRAMISU): checks if permission is granted, launches system dialog if not
5. The callback receives the result but doesn't gate functionality — the service starts either way

#### 2.4.5 Flash Hardware Check

```kotlin
    private fun checkFlashHardware(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }
```

**Alternative** (more reliable on some devices):
```kotlin
    private fun checkFlashHardware(): Boolean {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } catch (e: Exception) {
            false
        }
    }
```

**Recommendation**: Use `PackageManager.FEATURE_CAMERA_FLASH` (first approach) for simplicity. It's sufficient for v1 and doesn't require CAMERA permission.

#### 2.4.6 Service Management

```kotlin
    private fun startTorchService() {
        val intent = Intent(this, TorchService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopTorchService() {
        stopService(Intent(this, TorchService::class.java))
    }

    private fun toggleTorch() {
        val intent = Intent(this, TorchService::class.java).apply {
            action = ACTION_TOGGLE_TORCH
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
```

**Why `startForegroundService` instead of `bindService`**: The TorchService must outlive the Activity (screen-off, app backgrounded). Bound services are tied to the binding component's lifecycle. `startForegroundService` ensures the service runs independently.

**Toggle mechanism**: Sends an intent with `ACTION_TOGGLE_TORCH` action to the service. The service's `onStartCommand` dispatches based on the action.

#### 2.4.7 onCreate — Full Flow

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Load preferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false)

        // 2. Check flash hardware
        hasFlash = checkFlashHardware()

        // 3. Request notification permission (Android 13+)
        requestNotificationPermissionIfNeeded()

        // 4. Start service if enabled and flash available
        if (serviceEnabled && hasFlash) {
            startTorchService()
        }

        // 5. Register broadcast receiver for torch state
        ContextCompat.registerReceiver(
            this,
            torchStateReceiver,
            IntentFilter(ACTION_TORCH_STATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // 6. Set Compose content
        setContent {
            MyTorchTheme {
                TorchScreen(
                    torchOn = torchOn,
                    serviceEnabled = serviceEnabled,
                    hasFlash = hasFlash,
                    onToggle = { toggleTorch() },
                    onServiceToggle = { enabled ->
                        serviceEnabled = enabled
                        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
                        if (enabled) {
                            startTorchService()
                        } else {
                            stopTorchService()
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(torchStateReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver not registered
        }
    }
```

**Note**: No `Scaffold` is used — the background color comes from `MaterialTheme.colorScheme.background` which is `SurfaceDark`. The `TorchScreen` Column uses `Modifier.fillMaxSize()` with the theme's background. However, if edge-to-edge is enabled, the TorchScreen's Column should handle system bar insets. Consider:

```kotlin
// Inside TorchScreen, add to Column modifier:
Modifier
    .fillMaxSize()
    .background(MaterialTheme.colorScheme.background)
    .systemBarsPadding()
```

This requires importing `import androidx.compose.foundation.layout.systemBarsPadding` and `import androidx.compose.foundation.background`.

---

### 2.5 State Flow Diagram

```
TorchService
  │
  ├──(toggles torch)──► sendBroadcast(ACTION_TORCH_STATE, torch_on=true/false)
  │                           │
  │                           ▼
  │                     MainActivity.torchStateReceiver
  │                           │
  │                           ▼
  │                     torchOn = intent.getBooleanExtra(...)
  │                           │  (mutableStateOf triggers recomposition)
  │                           ▼
  │                     TorchScreen recomposes with new torchOn value
  │                           │
  │                           ├── animateColorAsState transitions status text color
  │                           ├── animateColorAsState transitions button fill/border
  │                           └── infiniteTransition pulses glow (only used when torchOn=true)
  │
  ◄──(user taps button)── onToggle() → toggleTorch() → startForegroundService(ACTION_TOGGLE_TORCH)
```

---

## 3. Caveats

1. **`setShadowLayer` on Canvas**: Some devices/GPU configurations may not render `setShadowLayer` correctly in Compose's Canvas. The fallback multi-circle approach (section 2.3.5) should be included as a safety net or used as the primary approach.

2. **Power icon drawing**: The arc-based power icon in Canvas uses `drawArc` with `Stroke`. The exact sizing (`radius * 0.35f`, `radius * 0.7f`) may need visual tuning. Consider extracting the power icon into a separate private composable for maintainability.

3. **`RECEIVER_NOT_EXPORTED` flag**: The `ContextCompat.registerReceiver` with `RECEIVER_NOT_EXPORTED` requires `core-ktx` 1.9.0+. Current version is 1.10.1 (line 3 of libs.versions.toml), which is sufficient.

4. **Edge-to-edge + system bars**: With `enableEdgeToEdge()`, content draws behind system bars. The TorchScreen Column needs `systemBarsPadding()` modifier to avoid content overlap. This requires `import androidx.compose.foundation.layout.systemBarsPadding`.

5. **Compose state on config change**: `mutableStateOf` at Activity level survives recomposition but NOT configuration changes (rotation). For a flashlight app, this is acceptable — screen lock/rotation doesn't need to preserve UI state since the real state lives in TorchService.

6. **Type.kt**: Left unchanged per plan. The default `Typography` is referenced by `Theme.kt`.

---

## 4. Conclusion

The Worker should implement these 4 files in this order:

1. **Color.kt** — Simple replacement, no dependencies
2. **Theme.kt** — Depends on Color.kt values
3. **TorchScreen.kt** — Depends on theme colors, no dependency on MainActivity or TorchService
4. **MainActivity.kt** — Depends on TorchScreen and MyTorchTheme; also references TorchService (which another worker handles)

**Critical implementation notes**:
- TorchScreen.kt must live in `com.example.mytorch.ui` package (not `.ui.theme`)
- MainActivity.kt references `TorchService::class.java` which will be created by a different worker — this will cause compilation errors until TorchService exists. Worker should add a `// TODO: TorchService` stub or coordinate build order.
- The `ACTION_TORCH_STATE`, `EXTRA_TORCH_ON`, and `ACTION_TOGGLE_TORCH` constants defined in `MainActivity.companion` must match exactly with the values used in `TorchService`. Consider: define these in a shared `Constants.kt` object to avoid duplication. If that's out of scope for this worker, document the exact string values for the service worker.

**Shared constants recommendation** (file: `app/src/main/java/com/example/mytorch/Constants.kt`):
```kotlin
package com.example.mytorch

object TorchConstants {
    const val PREFS_NAME = "mytorch_prefs"
    const val KEY_SERVICE_ENABLED = "service_enabled"
    const val ACTION_TORCH_STATE = "com.example.mytorch.TORCH_STATE"
    const val EXTRA_TORCH_ON = "torch_on"
    const val ACTION_TOGGLE_TORCH = "com.example.mytorch.TOGGLE_TORCH"
}
```

---

## 5. Verification Method

### Build Verification
```bash
cd c:\Users\moksh\AndroidStudioProjects\MyTorch
.\gradlew assembleDebug
```
Expected: Clean build with 0 errors. Note: will fail until TorchService.kt exists (referenced by MainActivity).

### File Verification Checklist
- [ ] `Color.kt` has exactly 6 color vals: TorchAmber, TorchAmberDark, SurfaceDark, SurfaceDarkElevated, OnSurfaceDim, OnSurfaceBright
- [ ] `Theme.kt` has NO references to: `lightColorScheme`, `dynamicDarkColorScheme`, `dynamicLightColorScheme`, `isSystemInDarkTheme`, `Build.VERSION`
- [ ] `Theme.kt` `MyTorchTheme` function has only `content` parameter (no `darkTheme`, no `dynamicColor`)
- [ ] `TorchScreen.kt` package is `com.example.mytorch.ui` (NOT `.ui.theme`)
- [ ] `TorchScreen.kt` has `@Composable fun TorchScreen(torchOn, serviceEnabled, hasFlash, onToggle, onServiceToggle)` signature
- [ ] `TorchScreen.kt` contains `rememberInfiniteTransition` and `animateFloat` for pulse
- [ ] `TorchScreen.kt` contains `animateColorAsState` for status text and button colors
- [ ] `TorchScreen.kt` contains `Canvas` with `drawCircle` and either `setShadowLayer` or multi-circle glow
- [ ] `MainActivity.kt` has `enableEdgeToEdge()` call preserved
- [ ] `MainActivity.kt` registers/unregisters `torchStateReceiver`
- [ ] `MainActivity.kt` has `registerForActivityResult` for POST_NOTIFICATIONS
- [ ] `MainActivity.kt` calls `checkFlashHardware()` and passes result to TorchScreen
- [ ] `MainActivity.kt` uses `SharedPreferences` for service-enabled flag

### Visual Verification (on device)
1. App launches with dark background (near-black 0xFF0D0D0D)
2. "TORCH OFF" text in dim gray
3. Power button shows dim circle with gray border
4. Tapping button → (once service exists) amber glow with pulsing animation
5. Status text transitions to amber "TORCH ON"
6. Service toggle switch themed in amber/dark
