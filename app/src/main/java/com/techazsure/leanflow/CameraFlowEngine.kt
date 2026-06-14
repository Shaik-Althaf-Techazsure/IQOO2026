package com.techazsure.leanflow

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFlowEngine(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var currentRecording: Recording? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // 🔥 PERMANENT STORAGE HUB
    private val mediaDir = File(context.filesDir, "LeanFlowMedia").apply { 
        if (!exists()) mkdirs() 
    }

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

                // Setup Video Recorder configuration
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                // Default to using the flagship rear-facing lens
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Clear any existing active camera bindings before mounting
                cameraProvider.unbindAll()

                // Mount the lens loops straight into the lifecycle owner
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture, videoCapture)
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

        // Setup a local storage file inside the app permanent storage
        val photoFile = File(mediaDir, "scan_${System.currentTimeMillis()}.jpg")
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

    /**
     * Starts recording a video clip to the cache directory.
     */
    fun startVideoRecording(onVideoSaved: (File) -> Unit, onError: (Exception) -> Unit) {
        val recordingChannel = videoCapture ?: run {
            onError(Exception("Video capture not initialized"))
            return
        }

        val videoFile = File(mediaDir, "recording_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        currentRecording = recordingChannel.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (!event.hasError()) {
                        println("[SUCCESS] Video stored in cache path: ${videoFile.absolutePath}")
                        onVideoSaved(videoFile)
                    } else {
                        println("[ERROR] Video recording failed: ${event.error}")
                        onError(Exception("Video recording failed with code: ${event.error}"))
                    }
                }
            }
        println("[RECORDING] Video stream capture started.")
    }

    /**
     * Stops the active video recording.
     */
    fun stopVideoRecording() {
        currentRecording?.stop()
        currentRecording = null
        println("[RECORDING] Stop command issued.")
    }

    fun shutdownExecutor() {
        cameraExecutor.shutdown()
    }
}
