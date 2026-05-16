package com.example.posex.pose

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

class PoseAnalyzer(
    private val onPoseDetected: (Pose, Int, Int) -> Unit,
    private val onError: (Exception) -> Unit,
    // How many milliseconds must pass between processed frames.
    // 100ms = max ~10 pose detections per second, which is plenty for
    // form feedback and safe on mid-range devices.
    // Raise this value if you still see thermal throttling on low-end hardware.
    private val processIntervalMs: Long = 100L
) : ImageAnalysis.Analyzer {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector: PoseDetector = PoseDetection.getClient(options)

    // Atomic flag: true while ML Kit is processing a frame.
    // AtomicBoolean is thread-safe without synchronized blocks —
    // CameraX delivers frames on a background executor thread.
    private val isProcessing = AtomicBoolean(false)

    // Timestamp of the last frame we actually sent to ML Kit.
    // Volatile so the write from the ML Kit callback thread is
    // visible to the CameraX analyzer thread on the next frame.
    @Volatile
    private var lastProcessedTimeMs: Long = 0L

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()

        // Gate 1 — Time throttle:
        // Drop this frame if not enough time has passed since the last one
        // we actually processed. This is the primary throughput limiter.
        if (now - lastProcessedTimeMs < processIntervalMs) {
            imageProxy.close()
            return
        }

        // Gate 2 — Processing lock:
        // Drop this frame if ML Kit is still working on the previous one.
        // compareAndSet(false, true) atomically checks the flag is false
        // and sets it to true. If it was already true, returns false → drop.
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        // Both gates passed — record the time and process this frame.
        lastProcessedTimeMs = now

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            isProcessing.set(false)
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { pose ->
                onPoseDetected(pose, imageProxy.width, imageProxy.height)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
            .addOnCompleteListener {
                // Always release the lock and close the proxy,
                // regardless of success or failure.
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    fun shutdown() {
        detector.close()
    }
}