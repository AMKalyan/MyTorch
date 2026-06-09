package com.example.mytorch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.mytorch.ui.TorchScreen
import com.example.mytorch.ui.theme.MyTorchTheme

class MainActivity : ComponentActivity() {
    private var isServiceEnabled by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TorchManager.init(this)

        val prefs = getSharedPreferences("TorchPrefs", Context.MODE_PRIVATE)
        isServiceEnabled = prefs.getBoolean("service_enabled", true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        if (isServiceEnabled && TorchManager.hasFlash()) {
            startTorchService()
        }

        setContent {
            MyTorchTheme {
                TorchScreen(
                    isServiceEnabled = isServiceEnabled,
                    onServiceToggled = { enabled ->
                        isServiceEnabled = enabled
                        prefs.edit().putBoolean("service_enabled", enabled).apply()
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

    private fun startTorchService() {
        val serviceIntent = Intent(this, TorchService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopTorchService() {
        val serviceIntent = Intent(this, TorchService::class.java)
        stopService(serviceIntent)
    }
}