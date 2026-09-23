package com.rec.recmov.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

class AudioDeviceManager(
    context: Context
) {

    private val audioManager =
        context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

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
