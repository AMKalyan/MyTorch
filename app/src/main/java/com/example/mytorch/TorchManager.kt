package com.example.mytorch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TorchManager {
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    
    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private var hasFlash = false
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        try {
            val cameraIds = cameraManager?.cameraIdList ?: emptyArray()
            for (id in cameraIds) {
                val characteristics = cameraManager?.getCameraCharacteristics(id)
                val flashAvailable = characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val lensFacing = characteristics?.get(CameraCharacteristics.LENS_FACING)
                if (flashAvailable == true && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    hasFlash = true
                    break
                }
            }
            if (cameraId == null && cameraIds.isNotEmpty()) {
                cameraId = cameraIds[0] // fallback
                hasFlash = cameraManager?.getCameraCharacteristics(cameraId!!)?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) {
                    _isTorchOn.value = enabled
                }
            }
        }, null)
        
        isInitialized = true
    }

    fun hasFlash(): Boolean = hasFlash

    fun toggle() {
        if (_isTorchOn.value) {
            turnOff()
        } else {
            turnOn()
        }
    }

    fun turnOn() {
        if (!hasFlash || cameraId == null) return
        try {
            cameraManager?.setTorchMode(cameraId!!, true)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun turnOff() {
        if (!hasFlash || cameraId == null) return
        try {
            cameraManager?.setTorchMode(cameraId!!, false)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
}
