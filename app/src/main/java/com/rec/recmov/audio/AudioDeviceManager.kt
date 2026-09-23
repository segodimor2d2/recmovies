package com.rec.recmov.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioDeviceManager(
    context: Context
) {

    private val audioManager =
        context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

    fun testBluetoothInput() {

        val communicationDevices =
            audioManager.getAvailableCommunicationDevices()

        communicationDevices.forEach { communicationDevice ->
            println(
                "AudioTest: CommunicationDevice: " +
                    "id=${communicationDevice.id}, " +
                    "type=${communicationDevice.type}, " +
                    "name=${communicationDevice.productName}"
            )
        }

        val device =
            audioManager
                .getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }

        if (device == null) {
            println("AudioTest: T10-L não encontrado")
            return
        }

        println(
            "AudioTest: dispositivo encontrado: " +
                "id=${device.id}, " +
                "type=${device.type}, " +
                "name=${device.productName}"
        )

        val communicationDevice =
            audioManager
                .getAvailableCommunicationDevices()
                .firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }

        println(
            "AudioTest: communicationDevice=" +
                communicationDevice?.productName
        )

        val communicationResult =
            communicationDevice?.let {
                audioManager.setCommunicationDevice(it)
            } ?: false

        println(
            "AudioTest: setCommunicationDevice=" +
                communicationResult
        )

        println(
            "AudioTest: setCommunicationDevice=" +
                communicationResult
        )

        val sampleRate = 8000
        val channelMask = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT

        val minBuffer =
            AudioRecord.getMinBufferSize(
                sampleRate,
                channelMask,
                encoding
            )

        println(
            "AudioTest: minBuffer=$minBuffer"
        )

        val audioRecord =
            AudioRecord.Builder()
                .setAudioSource(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer * 2)
                .build()

        val preferred =
            audioRecord.setPreferredDevice(device)

        println(
            "AudioTest: setPreferredDevice=$preferred"
        )

        println(
            "AudioTest: preferredDevice=" +
                audioRecord.preferredDevice?.productName
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            println("AudioTest: AudioRecord NÃO inicializado")
            audioRecord.release()
            return
        }

        println("AudioTest: AudioRecord inicializado")

        val buffer = ShortArray(minBuffer / 2)

        audioRecord.startRecording()

        val activeConfig =
            audioRecord.activeRecordingConfiguration

        println(
            "AudioTest: activeDevice=" +
                activeConfig?.audioDevice?.productName
        )

        println(
            "AudioTest: recordingState=" +
                audioRecord.recordingState
        )

        repeat(100) { index ->

            val read =
                audioRecord.read(
                    buffer,
                    0,
                    buffer.size
                )

            var peak = 0

            for (i in 0 until maxOf(read, 0)) {
                val value =
                    kotlin.math.abs(buffer[i].toInt())

                if (value > peak) {
                    peak = value
                }
            }

            println(
                "AudioTest: read=$read peak=$peak"
            )
        }

        audioRecord.stop()

        println("AudioTest: captura finalizada")

        audioRecord.release()
    }

    fun logCommunicationDevices() {

        val devices =
            audioManager.getAvailableCommunicationDevices()

        devices.forEach { device ->

            println(
                "CommunicationDevice: " +
                    "id=${device.id}, " +
                    "type=${device.type}, " +
                    "name=${device.productName}"
            )
        }
    }

    fun setBluetoothCommunicationDevice(): Boolean {

        val device =
            audioManager
                .getAvailableCommunicationDevices()
                .firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }

        if (device == null) {
            return false
        }

        return audioManager.setCommunicationDevice(device)
    }

    fun setCommunicationDevice(
        device: AudioDeviceInfo
    ): Boolean {
        return audioManager.setCommunicationDevice(device)
    }

    fun getInputDevices(): List<AudioDeviceInfo> {
        return audioManager
            .getDevices(AudioManager.GET_DEVICES_INPUTS)
            .toList()
    }

    fun logInputDevices() {

        val devices = getInputDevices()

        devices.forEach { device ->

            println(
                "AudioInputDevice: " +
                    "id=${device.id}, " +
                    "type=${device.type}, " +
                    "name=${device.productName}"
            )
        }
    }
}
