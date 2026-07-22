package com.fatih.adaptivecompositioncamera.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fatih.adaptivecompositioncamera.domain.model.AppSettings
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CompositionGuide
import com.fatih.adaptivecompositioncamera.domain.model.PhotoAspectRatio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "adaptive_camera_settings")

class SettingsRepository(private val context: Context) {
    private val guideKey = stringPreferencesKey("composition_guide")
    private val modeKey = stringPreferencesKey("mode")
    private val selectedCameraKey = stringPreferencesKey("selected_camera_id")
    private val selectedResolutionsKey = stringSetPreferencesKey("selected_resolution_ids")
    private val photoAspectRatioKey = stringPreferencesKey("photo_aspect_ratio")
    private val matchPreviewCropKey = booleanPreferencesKey("match_preview_crop")
    private val mirrorPreviewKey = booleanPreferencesKey("mirror_front_preview")
    private val mirrorSaveKey = booleanPreferencesKey("save_mirrored_selfie")
    private val screenFlashKey = booleanPreferencesKey("screen_flash")
    private val guideExportKey = booleanPreferencesKey("include_guide_in_export")
    private val audioKey = booleanPreferencesKey("audio_enabled")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val volumeShutterKey = booleanPreferencesKey("volume_shutter_enabled")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            selectedCameraId = prefs[selectedCameraKey],
            selectedResolutionIds = prefs[selectedResolutionsKey].orEmpty().mapNotNull { stored ->
                val separator = stored.indexOf('=')
                if (separator <= 0 || separator == stored.lastIndex) null
                else stored.substring(0, separator) to stored.substring(separator + 1)
            }.toMap(),
            mode = prefs[modeKey]?.let(::cameraModeFromStoredValue) ?: CameraMode.Photo,
            guide = prefs[guideKey]?.let(::guideFromStoredValue) ?: CompositionGuide.RuleOfThirds,
            photoAspectRatio = prefs[photoAspectRatioKey]?.let { stored ->
                runCatching { PhotoAspectRatio.valueOf(stored) }.getOrDefault(PhotoAspectRatio.FullSensor)
            } ?: PhotoAspectRatio.FullSensor,
            matchPreviewCrop = prefs[matchPreviewCropKey] ?: true,
            mirrorFrontPreview = prefs[mirrorPreviewKey] ?: true,
            saveMirroredSelfie = prefs[mirrorSaveKey] ?: false,
            screenFlash = prefs[screenFlashKey] ?: true,
            includeGuideInExport = prefs[guideExportKey] ?: false,
            audioEnabled = prefs[audioKey] ?: true,
            haptics = prefs[hapticsKey] ?: true,
            volumeShutter = prefs[volumeShutterKey] ?: true,
        )
    }

    suspend fun setGuide(guide: CompositionGuide) {
        context.dataStore.edit { it[guideKey] = guide.name }
    }

    suspend fun setMode(mode: CameraMode) {
        context.dataStore.edit { it[modeKey] = mode.name }
    }

    suspend fun setSelectedCamera(cameraId: String) {
        context.dataStore.edit { it[selectedCameraKey] = cameraId }
    }

    suspend fun setSelectedResolution(cameraId: String, resolutionId: String) {
        context.dataStore.edit { prefs ->
            val values = prefs[selectedResolutionsKey].orEmpty()
                .filterNot { it.substringBefore('=') == cameraId }
                .toMutableSet()
            values += "$cameraId=$resolutionId"
            prefs[selectedResolutionsKey] = values
        }
    }

    suspend fun setPhotoAspectRatio(aspectRatio: PhotoAspectRatio) {
        context.dataStore.edit { it[photoAspectRatioKey] = aspectRatio.name }
    }

    suspend fun setMatchPreviewCrop(enabled: Boolean) {
        context.dataStore.edit { it[matchPreviewCropKey] = enabled }
    }

    suspend fun setMirrorPreview(enabled: Boolean) {
        context.dataStore.edit { it[mirrorPreviewKey] = enabled }
    }

    suspend fun setSaveMirroredSelfie(enabled: Boolean) {
        context.dataStore.edit { it[mirrorSaveKey] = enabled }
    }

    suspend fun setScreenFlash(enabled: Boolean) {
        context.dataStore.edit { it[screenFlashKey] = enabled }
    }

    suspend fun setGuideExport(enabled: Boolean) {
        context.dataStore.edit { it[guideExportKey] = enabled }
    }

    suspend fun setAudio(enabled: Boolean) {
        context.dataStore.edit { it[audioKey] = enabled }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[hapticsKey] = enabled }
    }

    suspend fun setVolumeShutter(enabled: Boolean) {
        context.dataStore.edit { it[volumeShutterKey] = enabled }
    }
}

private fun cameraModeFromStoredValue(value: String): CameraMode = when (value) {
    "AutoPhoto" -> CameraMode.Photo
    "ProPhoto" -> CameraMode.Pro
    "HighResolution" -> CameraMode.MaximumResolution
    "Selfie" -> CameraMode.Photo
    "NormalVideo" -> CameraMode.Video
    else -> runCatching { CameraMode.valueOf(value) }.getOrDefault(CameraMode.Photo)
}

private fun guideFromStoredValue(value: String): CompositionGuide = when (value) {
    "Off" -> CompositionGuide.None
    "Thirds" -> CompositionGuide.RuleOfThirds
    "CenterCross" -> CompositionGuide.Centered
    "Diagonal" -> CompositionGuide.TextureRepetition
    "Horizon" -> CompositionGuide.HorizonLevel
    "PortraitSafeArea" -> CompositionGuide.FrameInFrame
    "SelfieEyeLine" -> CompositionGuide.EyeLine
    else -> runCatching { CompositionGuide.valueOf(value) }.getOrDefault(CompositionGuide.RuleOfThirds)
}
