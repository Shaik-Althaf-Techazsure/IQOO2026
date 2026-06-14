package com.techazsure.leanflow

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFlowEngine(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Binds the hardware camera lens straight to your UI layer and Lifecycle scope safely.
     */
    fun startCameraPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        // 🔥 Use the view's specific context to trace overlay window permissions safely
        val runtimeContext = previewView.context
        val cameraProviderFuture = ProcessCameraProvider.getInstance(runtimeContext)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Force COMPATIBLE mode so the overlay window renders perfectly on the phone screen
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                // Define the visual preview frame container mapping
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Define the high-res snapshot capture configuration channel
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Default to using the flagship rear-facing lens
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Clear any existing active camera bindings before mounting
                cameraProvider.unbindAll()

                // Mount the lens loops straight into the lifecycle owner
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                println("[SUCCESS] CameraFlow Engine: Live view matrix bound to current window layer.")
            } catch (exc: Exception) {
                println("[ERROR] Failed to bind camera sensor frames: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(runtimeContext))
    }

    /**
     * Captures a high-resolution snapshot completely offline and stores it in the cache directory.
     */
    fun takeMentorSnapshot(onPhotoSaved: (File) -> Unit, onError: (Exception) -> Unit) {
        val captureChannel = imageCapture ?: return

        // Setup a local storage file inside the app cache memory space
        val photoFile = File(context.cacheDir, "mentor_input_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        captureChannel.takePicture(
            outputOptions,
            cameraExecutor, // 🔥 Executed safely on your dedicated background thread instead of the main main UI runner
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    println("[SUCCESS] Photo stored in cache path: ${photoFile.absolutePath}")
                    onPhotoSaved(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    println("[ERROR] Snapshot capture failed: ${exception.message}")
                    onError(exception)
                }
            }
        )
    }

    fun shutdownExecutor() {
        cameraExecutor.shutdown()
    }
}