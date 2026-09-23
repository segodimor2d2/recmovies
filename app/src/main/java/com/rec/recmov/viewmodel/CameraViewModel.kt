package com.rec.recmov.viewmodel

import android.media.AudioDeviceInfo
import androidx.camera.video.Recording
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {

    var recording: Recording? by mutableStateOf(null)
        private set

    var videoCapture: VideoCapture<Recorder>? = null
        private set

    val isRecording: Boolean
        get() = recording != null

    fun startRecording(recording: Recording) {
        this.recording = recording
    }

    fun stopCurrentRecording() {
        recording?.stop()
        recording = null
    }

    fun setVideoCapture(
        videoCapture: VideoCapture<Recorder>
    ) {
        this.videoCapture = videoCapture
    }

    var selectedAudioDevice: AudioDeviceInfo? by mutableStateOf(null)
        private set

    fun selectAudioDevice(device: AudioDeviceInfo) {
        selectedAudioDevice = device
    }

}
