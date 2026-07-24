package com.fatih.adaptivecompositioncamera.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fatih.adaptivecompositioncamera.AdaptiveCameraViewModel
import com.fatih.adaptivecompositioncamera.domain.model.MediaItem
import com.fatih.adaptivecompositioncamera.ui.camera.CameraScreen
import com.fatih.adaptivecompositioncamera.ui.capability.CapabilityScreen
import com.fatih.adaptivecompositioncamera.ui.media.MediaViewerScreen
import com.fatih.adaptivecompositioncamera.ui.settings.SettingsScreen

private enum class AppScreen { Camera, Settings, CameraInformation, Diagnostics, MediaViewer }

@Composable
fun AdaptiveCameraApp(
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean,
    legacyStoragePermissionGranted: Boolean,
    requestCameraPermission: () -> Unit,
    requestAudioPermission: () -> Unit,
    requestLegacyStoragePermission: () -> Unit,
    volumeShutterEvent: Int,
    onCameraScreenState: (active: Boolean, volumeShutterEnabled: Boolean) -> Unit,
    viewModel: AdaptiveCameraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val report by viewModel.capabilityReport.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val message by viewModel.message.collectAsState()
    var screen by remember { mutableStateOf(AppScreen.Camera) }
    var viewerItem by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(screen, settings.volumeShutter) {
        onCameraScreenState(screen == AppScreen.Camera, settings.volumeShutter)
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    BackHandler(enabled = screen != AppScreen.Camera) {
        screen = if (screen in listOf(AppScreen.CameraInformation, AppScreen.Diagnostics)) AppScreen.Settings else AppScreen.Camera
    }

    when (screen) {
        AppScreen.Camera -> CameraScreen(
            modifier = Modifier.fillMaxSize(),
            settings = settings,
            report = report,
            cameraPermissionGranted = cameraPermissionGranted,
            audioPermissionGranted = audioPermissionGranted,
            legacyStoragePermissionGranted = legacyStoragePermissionGranted,
            requestCameraPermission = requestCameraPermission,
            requestAudioPermission = requestAudioPermission,
            requestLegacyStoragePermission = requestLegacyStoragePermission,
            volumeShutterEvent = volumeShutterEvent,
            onModeChange = viewModel::setMode,
            onGuideChange = viewModel::setGuide,
            onCameraChange = viewModel::setSelectedCamera,
            onResolutionChange = viewModel::setSelectedResolution,
            onAspectRatioChange = viewModel::setPhotoAspectRatio,
            onDiagnosticsChange = viewModel::updateDiagnostics,
            onOpenSettings = { screen = AppScreen.Settings },
            onOpenMedia = {
                viewerItem = it
                screen = AppScreen.MediaViewer
            },
            onMessage = viewModel::showMessage,
        )

        AppScreen.Settings -> SettingsScreen(
            modifier = Modifier.fillMaxSize(),
            settings = settings,
            onBack = { screen = AppScreen.Camera },
            onCameraInformation = { screen = AppScreen.CameraInformation },
            onDiagnostics = { screen = AppScreen.Diagnostics },
            onMirrorPreview = viewModel::setMirrorPreview,
            onSaveMirrored = viewModel::setSaveMirroredSelfie,
            onScreenFlash = viewModel::setScreenFlash,
            onAudio = viewModel::setAudio,
            onHaptics = viewModel::setHaptics,
            onVolumeShutter = viewModel::setVolumeShutter,
            onMatchPreviewCrop = viewModel::setMatchPreviewCrop,
        )

        AppScreen.CameraInformation -> CapabilityScreen(
            modifier = Modifier.fillMaxSize(),
            report = report,
            onRefresh = viewModel::refreshCapabilities,
            onBack = { screen = AppScreen.Settings },
        )

        AppScreen.Diagnostics -> CapabilityScreen(
            modifier = Modifier.fillMaxSize(),
            report = report,
            onRefresh = viewModel::refreshCapabilities,
            onBack = { screen = AppScreen.Settings },
            diagnosticsMode = true,
            activeMode = settings.mode,
            diagnostics = diagnostics,
        )

        AppScreen.MediaViewer -> viewerItem?.let { item ->
            MediaViewerScreen(
                modifier = Modifier.fillMaxSize(),
                item = item,
                onBack = { screen = AppScreen.Camera },
                onDeleted = {
                    viewerItem = null
                    screen = AppScreen.Camera
                    viewModel.showMessage("Media deleted")
                },
                onMessage = viewModel::showMessage,
            )
        } ?: run { screen = AppScreen.Camera }
    }
}
