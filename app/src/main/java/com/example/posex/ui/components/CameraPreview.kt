package com.example.posex.ui.components

import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.posex.pose.PoseAnalyzer
import com.google.mlkit.vision.pose.Pose
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    isActive: Boolean,
    onSnapshotCaptured: (ImageBitmap?) -> Unit,
    onPoseDetected: (Pose, Int, Int) -> Unit,
    onError: (Exception) -> Unit
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var lastActiveState by remember { mutableStateOf<Boolean?>(null) }

    DisposableEffect(cameraProviderFuture) {
        cameraProviderFuture.addListener(
            { cameraProvider = cameraProviderFuture.get() },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            cameraProvider?.unbindAll()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            previewView
        },
        update = { previewView ->
            val provider = cameraProvider ?: return@AndroidView
            if (lastActiveState == isActive) return@AndroidView

            if (isActive) {
                onSnapshotCaptured(null)
                bindCamera(
                    cameraProvider = provider,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    executor = executor,
                    onPoseDetected = onPoseDetected,
                    onError = onError
                )
            } else {
                onSnapshotCaptured(previewView.bitmap?.asImageBitmap())
                provider.unbindAll()
            }

            lastActiveState = isActive
        }
    )
}

private fun bindCamera(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    executor: Executor,
    onPoseDetected: (Pose, Int, Int) -> Unit,
    onError: (Exception) -> Unit
) {
    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

    val preview = Preview.Builder()
        .setTargetRotation(rotation)
        .build()
        .also { it.surfaceProvider = previewView.surfaceProvider }

    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetRotation(rotation)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(
                executor,
                PoseAnalyzer(
                    onPoseDetected = onPoseDetected,
                    onError = onError
                )
            )
        }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        .build()

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
    } catch (e: Exception) {
        onError(e)
    }
}
