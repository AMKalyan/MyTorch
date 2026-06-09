package com.example.mytorch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TorchManager.init(context)
        TorchManager.toggle()
    }
}
