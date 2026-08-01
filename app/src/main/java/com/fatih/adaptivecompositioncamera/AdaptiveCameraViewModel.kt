package com.fatih.adaptivecompositioncamera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fatih.adaptivecompositioncamera.capability.AndroidCameraCapabilityRepository
import com.fatih.adaptivecompositioncamera.domain.model.AppSettings
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraDiagnostics
import com.fatih.adaptivecompositioncamera.domain.model.CapabilityReport
import com.fatih.adaptivecompositioncamera.domain.model.CompositionGuide
import com.fatih.adaptivecompositioncamera.domain.model.PhotoAspectRatio
import com.fatih.adaptivecompositioncamera.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdaptiveCameraViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val capabilityRepository = AndroidCameraCapabilityRepository(application)

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    val capabilityReport: StateFlow<CapabilityReport?> = capabilityRepository.capabilityReport.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _diagnostics = MutableStateFlow(CameraDiagnostics())
    val diagnostics: StateFlow<CameraDiagnostics> = _diagnostics.asStateFlow()

    init {
        refreshCapabilities()
    }

    fun refreshCapabilities() {
        viewModelScope.launch {
            runCatching { capabilityRepository.refresh() }
                .onFailure { _message.value = it.message ?: "Unable to inspect camera capabilities." }
        }
    }

    fun setGuide(guide: CompositionGuide) = viewModelScope.launch {
        settingsRepository.setGuide(guide)
    }

    fun setMode(mode: CameraMode) = viewModelScope.launch {
        settingsRepository.setMode(mode)
    }

    fun setSelectedCamera(cameraId: String) = viewModelScope.launch {
        settingsRepository.setSelectedCamera(cameraId)
    }

    fun setSelectedResolution(cameraId: String, resolutionId: String) = viewModelScope.launch {
        settingsRepository.setSelectedResolution(cameraId, resolutionId)
    }

    fun setPhotoAspectRatio(aspectRatio: PhotoAspectRatio) = viewModelScope.launch {
        settingsRepository.setPhotoAspectRatio(aspectRatio)
    }

    fun setMatchPreviewCrop(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMatchPreviewCrop(enabled)
    }

    fun setMirrorPreview(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMirrorPreview(enabled)
    }

    fun setSaveMirroredSelfie(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSaveMirroredSelfie(enabled)
    }

    fun setScreenFlash(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setScreenFlash(enabled)
    }

    fun setGuideExport(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setGuideExport(enabled)
    }

    fun setAudio(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAudio(enabled)
    }

    fun setHaptics(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setHaptics(enabled)
    }

    fun setVolumeShutter(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setVolumeShutter(enabled)
    }

    fun updateDiagnostics(diagnostics: CameraDiagnostics) {
        _diagnostics.value = diagnostics
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun clearMessage() {
        _message.value = null
    }
}
