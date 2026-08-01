package com.fatih.adaptivecompositioncamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fatih.adaptivecompositioncamera.ui.AdaptiveCameraApp
import com.fatih.adaptivecompositioncamera.ui.theme.AdaptiveCompositionCameraTheme

class MainActivity : ComponentActivity() {
    private var volumeShutterEvent by mutableIntStateOf(0)
    private var cameraScreenActive = true

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (cameraScreenActive && isVolumeKey) {
            volumeShutterEvent += 1
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdaptiveCompositionCameraTheme {
                var cameraGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
                    )
                }
                var audioGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                    )
                }
                var legacyStorageGranted by remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED,
                    )
                }
                val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    cameraGranted = it
                }
                val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    audioGranted = it
                }
                val legacyStoragePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    legacyStorageGranted = it
                }
                LaunchedEffect(Unit) {
                    if (!cameraGranted) cameraPermission.launch(Manifest.permission.CAMERA)
                }
                AdaptiveCameraApp(
                    cameraPermissionGranted = cameraGranted,
                    audioPermissionGranted = audioGranted,
                    legacyStoragePermissionGranted = legacyStorageGranted,
                    requestCameraPermission = { cameraPermission.launch(Manifest.permission.CAMERA) },
                    requestAudioPermission = { audioPermission.launch(Manifest.permission.RECORD_AUDIO) },
                    requestLegacyStoragePermission = { legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) },
                    volumeShutterEvent = volumeShutterEvent,
                    onCameraScreenState = { active, volumeShutterEnabled ->
                        cameraScreenActive = active && volumeShutterEnabled
                        updateSystemBars(active)
                    },
                )
            }
        }
    }

    private fun updateSystemBars(cameraActive: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (cameraActive) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
