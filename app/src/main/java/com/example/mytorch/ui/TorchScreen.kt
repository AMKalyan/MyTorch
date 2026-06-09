package com.example.mytorch.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytorch.TorchManager
import com.example.mytorch.ui.theme.OnSurfaceBright
import com.example.mytorch.ui.theme.OnSurfaceDim
import com.example.mytorch.ui.theme.SurfaceDarkElevated
import com.example.mytorch.ui.theme.TorchAmber

@Composable
fun TorchScreen(
    isServiceEnabled: Boolean,
    onServiceToggled: (Boolean) -> Unit
) {
    val isTorchOn by TorchManager.isTorchOn.collectAsState()
    val hasFlash = remember { TorchManager.hasFlash() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Status Text
            val statusColor by animateColorAsState(
                targetValue = if (isTorchOn) TorchAmber else OnSurfaceDim,
                animationSpec = tween(300)
            )
            
            Text(
                text = if (isTorchOn) "TORCH ON" else "TORCH OFF",
                color = statusColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Power Button
            val buttonColor by animateColorAsState(
                targetValue = if (isTorchOn) TorchAmber else SurfaceDarkElevated,
                animationSpec = tween(300)
            )
            val iconColor by animateColorAsState(
                targetValue = if (isTorchOn) OnSurfaceBright else OnSurfaceDim,
                animationSpec = tween(300)
            )

            val infiniteTransition = rememberInfiniteTransition()
            val glowRadius by infiniteTransition.animateFloat(
                initialValue = 20f,
                targetValue = 40f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .shadow(
                        elevation = if (isTorchOn) glowRadius.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = TorchAmber,
                        ambientColor = TorchAmber
                    )
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(enabled = hasFlash) {
                        TorchManager.toggle()
                    }
            ) {
                Text(
                    text = "⏻",
                    color = iconColor,
                    fontSize = 64.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Shake phone twice to toggle",
                color = OnSurfaceDim,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            if (!hasFlash) {
                Text(
                    text = "Your device doesn't have a camera flash",
                    color = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Service Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Background Service",
                    color = OnSurfaceBright,
                    fontSize = 16.sp
                )
                Switch(
                    checked = isServiceEnabled,
                    onCheckedChange = onServiceToggled,
                    enabled = hasFlash,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnSurfaceBright,
                        checkedTrackColor = TorchAmber,
                        uncheckedThumbColor = OnSurfaceDim,
                        uncheckedTrackColor = SurfaceDarkElevated
                    )
                )
            }
        }
    }
}
