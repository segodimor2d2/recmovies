package com.rec.recmov.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rec.recmov.viewmodel.CameraViewModel

@Composable
fun CameraPreview(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var videoCapture by remember {
        mutableStateOf<VideoCapture<Recorder>?>(null)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            androidx.camera.view.PreviewView(context)
        },
        update = { previewView ->

            if (
                ContextCompat.checkSelfPermission(
                    previewView.context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@AndroidView
            }

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(
                    previewView.context
                )

            cameraProviderFuture.addListener({

                val cameraProvider =
                    cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider =
                            previewView.surfaceProvider
                    }

                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.FHD
                        )
                    )
                    .build()

                val newVideoCapture =
                    VideoCapture.withOutput(recorder)

                videoCapture = newVideoCapture

                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newVideoCapture
                )

            }, ContextCompat.getMainExecutor(previewView.context))
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            viewModel.recording?.stop()
            viewModel.setRecording(null)
        }
    }
}
