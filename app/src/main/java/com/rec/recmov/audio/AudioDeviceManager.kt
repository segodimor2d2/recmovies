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

    fun setBluetoothInputRoute(): Boolean {

        val device =
            audioManager
                .getAvailableCommunicationDevices()
                .firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }

        if (device == null) {
            println(
                "AudioDevice: Bluetooth SCO não encontrado"
            )
            return false
        }

        println(
            "AudioDevice: selecionando Bluetooth = " +
                device.productName
        )

        val result =
            audioManager.setCommunicationDevice(device)

        println(
            "AudioDevice: setCommunicationDevice=" +
                result
        )

        return result
    }

    fun createBluetoothAudioRecord(): AudioRecord? {

        val device =
            audioManager
                .getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }

        if (device == null) {
            println(
                "AudioRecord: Bluetooth T10-L não encontrado"
            )
            return null
        }

        println(
            "AudioRecord: dispositivo = " +
                "id=${device.id}, " +
                "name=${device.productName}"
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
            "AudioRecord: minBuffer=$minBuffer"
        )

        if (minBuffer <= 0) {
            println(
                "AudioRecord: minBuffer inválido"
            )
            return null
        }

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
                .setBufferSizeInBytes(
                    minBuffer * 2
                )
                .build()

        val preferred =
            audioRecord.setPreferredDevice(device)

        println(
            "AudioRecord: setPreferredDevice=" +
                preferred
        )

        println(
            "AudioRecord: preferredDevice=" +
                audioRecord.preferredDevice?.productName
        )

        if (
            audioRecord.state !=
            AudioRecord.STATE_INITIALIZED
        ) {
            println(
                "AudioRecord: NÃO inicializado"
            )

            audioRecord.release()
            return null
        }

        println(
            "AudioRecord: inicializado"
        )

        return audioRecord
    }

    fun testBluetoothInput() {

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

        if (communicationDevice == null) {
            println(
                "AudioTest: T10-L não encontrado entre os " +
                    "dispositivos de comunicação"
            )
            return
        }

        val communicationResult =
            audioManager.setCommunicationDevice(
                communicationDevice
            )

        println(
            "AudioTest: setCommunicationDevice=" +
                communicationResult
        )

        println(
            "AudioTest: communicationDevice=" +
                audioManager.communicationDevice?.productName
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

        if (minBuffer <= 0) {
            println("AudioTest: minBuffer inválido")
            return
        }

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
                .setBufferSizeInBytes(
                    minBuffer * 2
                )
                .build()

        val preferred =
            audioRecord.setPreferredDevice(device)

        println(
            "AudioTest: setPreferredDevice=" +
                preferred
        )

        println(
            "AudioTest: preferredDevice=" +
                audioRecord.preferredDevice?.productName
        )

        if (
            audioRecord.state !=
            AudioRecord.STATE_INITIALIZED
        ) {
            println(
                "AudioTest: AudioRecord NÃO inicializado"
            )

            audioRecord.release()
            return
        }

        println(
            "AudioTest: AudioRecord inicializado"
        )

        audioRecord.startRecording()

        println(
            "AudioTest: recordingState=" +
                audioRecord.recordingState
        )

        val activeConfig =
            audioRecord.activeRecordingConfiguration

        println(
            "AudioTest: activeDevice=" +
                activeConfig?.audioDevice?.productName
        )

        println(
            "AudioTest: activeConfig=" +
                activeConfig
        )

        val buffer =
            ShortArray(minBuffer / 2)

        repeat(1) { index ->

            val read =
                audioRecord.read(
                    buffer,
                    0,
                    buffer.size
                )

            var peak = 0L
            var sumSquares = 0.0

            if (read > 0) {

                for (i in 0 until read) {

                    val sample =
                        buffer[i].toLong()

                    val absolute =
                        kotlin.math.abs(sample)

                    if (absolute > peak) {
                        peak = absolute
                    }

                    sumSquares +=
                        sample.toDouble() *
                        sample.toDouble()
                }

                val rms =
                    kotlin.math.sqrt(
                        sumSquares / read
                    )

                println(
                    "AudioTest: " +
                        "[$index] " +
                        "read=$read " +
                        "peak=$peak " +
                        "rms=$rms"
                )

            } else {

                println(
                    "AudioTest: " +
                        "[$index] " +
                        "read=$read"
                )
            }
        }

        audioRecord.stop()

        println(
            "AudioTest: captura finalizada"
        )

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

        audioManager.mode =
            AudioManager.MODE_IN_COMMUNICATION

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
