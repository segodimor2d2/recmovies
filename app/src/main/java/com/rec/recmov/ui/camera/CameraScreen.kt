package com.rec.recmov.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rec.recmov.viewmodel.CameraViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        CameraPreview(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = {
                // gravação será implementada no próximo passo
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text("GRAVAR")
        }
    }
}
