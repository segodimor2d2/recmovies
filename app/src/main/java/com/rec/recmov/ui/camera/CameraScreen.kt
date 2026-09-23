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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import java.io.RandomAccessFile


private fun RandomAccessFile.writeIntLE(
    value: Int
) {
    writeByte(value and 0xFF)
    writeByte((value shr 8) and 0xFF)
    writeByte((value shr 16) and 0xFF)
    writeByte((value shr 24) and 0xFF)
}

private fun RandomAccessFile.writeShortLE(
    value: Int
) {
    writeByte(value and 0xFF)
    writeByte((value shr 8) and 0xFF)
}

private fun writeWavHeader(
    file: RandomAccessFile,
    dataSize: Long,
    sampleRate: Int,
    channels: Int,
    bitsPerSample: Int
) {
    val byteRate =
        sampleRate * channels * bitsPerSample / 8

    val blockAlign =
        channels * bitsPerSample / 8

    file.seek(0)

    file.writeBytes("RIFF")

    file.writeIntLE(
        (36 + dataSize).toInt()
    )

    file.writeBytes("WAVE")

    file.writeBytes("fmt ")

    file.writeIntLE(16)
    file.writeShortLE(1)
    file.writeShortLE(channels)

    file.writeIntLE(sampleRate)
    file.writeIntLE(byteRate)

    file.writeShortLE(blockAlign)
    file.writeShortLE(bitsPerSample)

    file.writeBytes("data")

    file.writeIntLE(
        dataSize.toInt()
    )
}

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current

    val audioDeviceManager = AudioDeviceManager(context)

    val audioDevices =
        audioDeviceManager.getInputDevices()

    val bluetoothAudioRecord = remember {
        mutableStateOf<android.media.AudioRecord?>(null)
    }

    val bluetoothWavFile = remember {
        mutableStateOf<java.io.File?>(null)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        CameraPreview(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            audioDevices.forEach { device ->
                Text(
                    text = "${device.productName} (${device.type})",
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f)
                        )
                        .clickable {

                            viewModel.selectAudioDevice(device)

                            if (
                                device.type ==
                                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                            ) {
                                val success =
                                    audioDeviceManager
                                        .setBluetoothCommunicationDevice()

                                println(
                                    "AudioDevice: Bluetooth routing = $success"
                                )
                            }
                        }
                        .padding(10.dp),
                    color = Color.White
                )
            }
        }


        viewModel.selectedAudioDevice?.let { device ->

            Text(
                text = "Selecionado: ${device.productName}\n" +
                        "id=${device.id} type=${device.type}",
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


        Button(
            onClick = {

                if (viewModel.isRecording) {

                    viewModel.stopCurrentRecording()

                    bluetoothAudioRecord.value?.let { audioRecord ->

                        if (
                            audioRecord.recordingState ==
                                android.media.AudioRecord.RECORDSTATE_RECORDING
                        ) {
                            audioRecord.stop()
                        }

                        audioRecord.release()

                        println(
                            "AudioRecord: parado e liberado"
                        )
                    }

                    bluetoothAudioRecord.value = null

                } else {

                    val videoCapture =
                        viewModel.videoCapture
                            ?: return@Button

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
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            )
                            .setContentValues(contentValues)
                            .build()


                    audioDeviceManager.setBluetoothInputRoute()

                    val bluetoothRouted =
                        audioDeviceManager.setBluetoothCommunicationDevice()

                    println(
                        "AudioDevice: Bluetooth routing antes da gravação = " +
                            bluetoothRouted
                    )

                    bluetoothAudioRecord.value =
                        if (
                            viewModel.selectedAudioDevice?.type ==
                                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        ) {
                            audioDeviceManager.createBluetoothAudioRecord()
                        } else {
                            null
                        }

                    bluetoothAudioRecord.value?.let { audioRecord ->

                        val wavFile =
                            java.io.File(
                                context.cacheDir,
                                "recmov_bluetooth_audio.wav"
                            )

                        bluetoothWavFile.value = wavFile

                        println(
                            "AudioRecord: WAV temporário = " +
                                wavFile.absolutePath
                        )

                        Thread {

                            val sampleRate = 8000
                            val channels = 1
                            val bitsPerSample = 16

                            var dataSize = 0L

                            RandomAccessFile(
                                wavFile,
                                "rw"
                            ).use { wav ->

                                // Reserva os primeiros 44 bytes
                                // para o cabeçalho WAV.
                                wav.setLength(44)

                                audioRecord.startRecording()

                                println(
                                    "AudioRecord: gravação iniciada = " +
                                        (
                                            audioRecord.recordingState ==
                                                android.media.AudioRecord.RECORDSTATE_RECORDING
                                        )
                                )

                                val buffer =
                                    ShortArray(1024)

                                while (
                                    audioRecord.recordingState ==
                                        android.media.AudioRecord.RECORDSTATE_RECORDING
                                ) {

                                    val read =
                                        audioRecord.read(
                                            buffer,
                                            0,
                                            buffer.size
                                        )

                                    if (read > 0) {

                                        for (i in 0 until read) {

                                            val sample =
                                                buffer[i].toInt()

                                            wav.writeByte(
                                                sample and 0xFF
                                            )

                                            wav.writeByte(
                                                (sample shr 8) and 0xFF
                                            )
                                        }

                                        dataSize +=
                                            read.toLong() * 2
                                    }
                                }

                                writeWavHeader(
                                    file = wav,
                                    dataSize = dataSize,
                                    sampleRate = sampleRate,
                                    channels = channels,
                                    bitsPerSample = bitsPerSample
                                )
                            }

                            println(
                                "AudioRecord: WAV finalizado"
                            )

                            println(
                                "AudioRecord: WAV bytes de áudio = " +
                                    dataSize
                            )

                        }.start()
                    }

                    val recording =

                        videoCapture.output
                            .prepareRecording(
                                context,
                                mediaStoreOutput
                            )
                            .withAudioEnabled()
                            .start(
                                ContextCompat.getMainExecutor(context)
                            ) { event ->

                                when (event) {

                                    is VideoRecordEvent.Start -> {
                                        // gravação iniciada
                                    }

                                    is VideoRecordEvent.Finalize -> {

                                        if (
                                            !event.hasError()
                                        ) {
                                            // vídeo salvo
                                        }
                                    }
                                }
                            }

                    viewModel.startRecording(recording)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                if (viewModel.isRecording) {
                    "stop"
                } else {
                    "rec"
                }
            )
        }
    }
}
