package com.rec.recmov.viewmodel

import androidx.lifecycle.ViewModel
import androidx.camera.video.Recording

class CameraViewModel : ViewModel() {

    var recording: Recording? = null
        private set

    val isRecording: Boolean
        get() = recording != null

    fun setRecording(recording: Recording?) {
        this.recording = recording
    }
}
