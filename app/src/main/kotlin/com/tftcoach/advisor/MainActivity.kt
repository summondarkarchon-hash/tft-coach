// MainActivity.kt
package com.tftcoach.advisor

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tftcoach.advisor.ui.MainScreen
import com.tftcoach.advisor.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null)
            vm.startCapture(result.resultCode, result.data!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ui by vm.ui.collectAsState()
            MainScreen(
                uiState = ui,
                onRequestCapture = ::requestCapture,
                onStop = vm::stopCapture,
                onOpenOverlaySettings = vm::openOverlayPermissionSettings,
                onAlphaChange = vm::setAlpha,
                onToggleFilter = vm::toggleFilter
            )
        }
    }

    override fun onResume() {
        super.onResume()
        vm.checkOverlayPermission()
    }

    private fun requestCapture() {
        if (!Settings.canDrawOverlays(this)) {
            vm.openOverlayPermissionSettings(); return
        }
        val pm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(pm.createScreenCaptureIntent())
    }
}
