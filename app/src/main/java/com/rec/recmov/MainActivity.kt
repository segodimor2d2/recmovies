package com.rec.recmov

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.rec.recmov.ui.camera.CameraScreen
import com.rec.recmov.ui.theme.RecmovTheme
import com.rec.recmov.audio.AudioDeviceManager

class MainActivity : ComponentActivity() {

    private var showCamera = false

    private val permissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val cameraGranted =
                permissions[Manifest.permission.CAMERA] == true

            val audioGranted =
                permissions[Manifest.permission.RECORD_AUDIO] == true

            showCamera = cameraGranted && audioGranted

            setContentView()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AudioDeviceManager(this).logInputDevices()
        AudioDeviceManager(this).logCommunicationDevices()
        // AudioDeviceManager(this).testBluetoothInput()

        val cameraGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        val audioGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (cameraGranted && audioGranted) {

            showCamera = true
            setContentView()

        } else {

            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    private fun setContentView() {
        setContent {
            RecmovTheme {
                if (showCamera) {
                    CameraScreen()
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Permissões da câmera e do microfone necessárias"
                        )
                    }
                }
            }
        }
    }
}
