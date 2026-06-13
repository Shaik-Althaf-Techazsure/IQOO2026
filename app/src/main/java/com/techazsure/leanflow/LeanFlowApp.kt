package com.techazsure.leanflow

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class LeanFlowApp : Application(), CameraXConfig.Provider {

    override fun onCreate() {
        super.onCreate()
        println("[INIT] LeanFlow Application context mounted successfully.")
    }

    // Fixed: Removed 'this' context argument from the defaultConfig parameter line
    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig()).build()
    }
}