package com.rec.recmov.ui.camera

import android.content.ContentValues
import android.provider.MediaStore
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rec.recmov.viewmodel.CameraViewModel
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Column
import com.rec.recmov.audio.AudioDeviceManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color


import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {

    var showAudioDevices by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    val audioDeviceManager = AudioDeviceManager(context)

    val audioDevices =
        audioDeviceManager.getInputDevices()

    fun audioDeviceName(device: android.media.AudioDeviceInfo): String {
        return when (device.type) {
            android.media.AudioDeviceInfo.TYPE_BUILTIN_MIC ->
                "Microfone interno"

            android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ->
                "Fone com fio"

            android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ->
                "Fone com fio"

            android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ->
                "Bluetooth"

            else ->
                device.productName.toString()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        CameraPreview(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // ------------------------------------------------------------
        // BOTÃO DE SELEÇÃO DE ÁUDIO
        // ------------------------------------------------------------

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    top = 50.dp,
                    end = 12.dp
                )
                .background(
                    Color.Black.copy(alpha = 0.5f)
                )
                .clickable {
                    showAudioDevices = !showAudioDevices
                }
                .padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                )
        ) {
            Text(
                text = if (showAudioDevices) { ">" } else { "<" },
                color = Color.White
            )
        }

        // ------------------------------------------------------------
        // LISTA DE DISPOSITIVOS DE ÁUDIO
        // ------------------------------------------------------------

        if (showAudioDevices) {

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        top = 100.dp,
                        end = 42.dp
                    )
            ) {

                audioDevices.forEach { device ->

                    Text(
                        text = audioDeviceName(device),
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f)
                            )
                            .clickable {

                                viewModel.selectAudioDevice(device)

                                if (
                                    device.type ==
                                    android.media.AudioDeviceInfo
                                        .TYPE_BLUETOOTH_SCO
                                ) {

                                    val success =
                                        audioDeviceManager
                                            .setBluetoothCommunicationDevice()

                                    println(
                                        "AudioDevice: Bluetooth routing = $success"
                                    )

                                } else {

                                    audioDeviceManager
                                        .clearCommunicationDevice()
                                }

                                // Fecha a lista depois da seleção
                                showAudioDevices = false
                            }
                            .padding(10.dp),
                        color = Color.White
                    )
                }
            }

            // --------------------------------------------------------
            // DISPOSITIVO SELECIONADO
            // --------------------------------------------------------

            viewModel.selectedAudioDevice?.let { device ->

                Text(
                    text = "Selecionado: ${audioDeviceName(device)}",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f)
                        )
                        .padding(10.dp),
                    color = Color.White
                )
            }
        }

        // ------------------------------------------------------------
        // BOTÃO REC
        // ------------------------------------------------------------

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f)
                )
                .clickable {

                    if (viewModel.isRecording) {

                        viewModel.stopCurrentRecording()

                    } else {

                        val videoCapture =
                            viewModel.videoCapture
                                ?: return@clickable

                        val name =
                            "recmov_${System.currentTimeMillis()}.mp4"

                        val contentValues =
                            ContentValues().apply {

                                put(
                                    MediaStore.Video.Media.DISPLAY_NAME,
                                    name
                                )

                                put(
                                    MediaStore.Video.Media.MIME_TYPE,
                                    "video/mp4"
                                )
                            }

                        val mediaStoreOutput =
                            MediaStoreOutputOptions
                                .Builder(
                                    context.contentResolver,
                                    MediaStore.Video.Media
                                        .EXTERNAL_CONTENT_URI
                                )
                                .setContentValues(
                                    contentValues
                                )
                                .build()

                        val recording =
                            videoCapture.output
                                .prepareRecording(
                                    context,
                                    mediaStoreOutput
                                )
                                .withAudioEnabled()
                                .start(
                                    ContextCompat.getMainExecutor(
                                        context
                                    )
                                ) { event ->

                                    when (event) {

                                        is VideoRecordEvent.Start -> {
                                            // gravação iniciada
                                        }

                                        is VideoRecordEvent.Finalize -> {

                                            if (!event.hasError()) {
                                                // vídeo salvo
                                            }
                                        }
                                    }
                                }

                        viewModel.startRecording(recording)
                    }
                }
                .padding(
                    horizontal = 20.dp,
                    vertical = 10.dp
                )
        ) {

            Text(
                text = if (viewModel.isRecording) {
                    "stop"
                } else {
                    "rec"
                },
                color = Color.White
            )
        }
    }
}
