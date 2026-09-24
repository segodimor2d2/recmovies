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

    fun getInputDevices(): List<AudioDeviceInfo> {
        return audioManager
            .getDevices(AudioManager.GET_DEVICES_INPUTS)
            .toList()
    }

    fun clearCommunicationDevice() {
        audioManager.clearCommunicationDevice()
        audioManager.mode = AudioManager.MODE_NORMAL
    }

}
