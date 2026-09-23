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

class MainActivity : ComponentActivity() {

    private var showCamera = false

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            showCamera = granted

            if (granted) {
                setContentView()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showCamera = true
        } else {
            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }

        setContentView()
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
                        Text("Permissão da câmera necessária")
                    }
                }
            }
        }
    }
}
