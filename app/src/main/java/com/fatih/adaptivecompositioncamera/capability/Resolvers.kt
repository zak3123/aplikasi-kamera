package com.fatih.adaptivecompositioncamera.capability

import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraConfiguration
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionResolver
import com.fatih.adaptivecompositioncamera.domain.model.ExtensionSupport
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedCapabilityProvider
import com.fatih.adaptivecompositioncamera.domain.model.HighSpeedVideoOption
import com.fatih.adaptivecompositioncamera.domain.model.LensFacing
import com.fatih.adaptivecompositioncamera.domain.model.StabilizationResolver

class CameraConfigurationResolver {
    fun availableModes(capability: CameraCapability): List<CameraMode> = buildList {
        add(CameraMode.Photo)
        if (capability.videoResolutions.isNotEmpty()) add(CameraMode.Video)
        val recommended = capability.jpegResolutions.firstOrNull { it.recommended }
        val maximum = capability.displayMaximumResolution
        if (maximum != null && recommended != null && maximum.megapixels > recommended.megapixels * 1.2) {
            add(CameraMode.MaximumResolution)
        }
    }

    fun safeResolution(capability: CameraCapability, requested: CameraResolution?): CameraResolution? {
        val choices = capability.selectablePhotoResolutions
        return choices.firstOrNull { it.id == requested?.id }
            ?: choices.firstOrNull { it.recommended }
            ?: choices.firstOrNull()
    }

    fun resolve(
        capability: CameraCapability,
        mode: CameraMode,
        resolution: CameraResolution?,
    ): CameraConfiguration {
        val safeMode = mode.takeIf { it in availableModes(capability) } ?: CameraMode.Photo
        return CameraConfiguration(
            lensFacing = capability.lensFacing,
            cameraId = capability.cameraId,
            mode = safeMode,
            resolution = safeResolution(capability, resolution),
        )
    }
}

class ModeConflictResolver {
    fun resolve(configuration: CameraConfiguration, capability: CameraCapability): CameraConfiguration {
        return when (configuration.mode) {
            CameraMode.SlowMotion, CameraMode.HighFrameRate -> configuration.copy(
                stabilizationEnabled = false,
                resolution = capability.jpegResolutions.firstOrNull { resolution ->
                    capability.highSpeedVideo.any { it.width == resolution.width && it.height == resolution.height }
                } ?: configuration.resolution,
            )
            CameraMode.MaximumResolution -> configuration.copy(stabilizationEnabled = false)
            else -> configuration
        }
    }
}

class LastKnownGoodCameraConfiguration {
    private val values = mutableMapOf<LensFacing, CameraConfiguration>()

    fun remember(configuration: CameraConfiguration) {
        values[configuration.lensFacing] = configuration
    }

    fun restore(facing: LensFacing): CameraConfiguration? = values[facing]
}

class DefaultStabilizationResolver : StabilizationResolver {
    override fun isSupported(capability: CameraCapability, mode: CameraMode): Boolean {
        return when (mode) {
            CameraMode.Video, CameraMode.TimeLapse -> capability.stabilization.electronicVideo || capability.stabilization.optical
            CameraMode.HighFrameRate, CameraMode.SlowMotion, CameraMode.MaximumResolution -> false
            else -> capability.stabilization.preview || capability.stabilization.optical
        }
    }
}

class DefaultExtensionResolver : ExtensionResolver {
    override fun availableExtensions(capability: CameraCapability): ExtensionSupport = capability.extensions
}

class DefaultHighSpeedCapabilityProvider : HighSpeedCapabilityProvider {
    override fun options(capability: CameraCapability): List<HighSpeedVideoOption> = capability.highSpeedVideo
}
