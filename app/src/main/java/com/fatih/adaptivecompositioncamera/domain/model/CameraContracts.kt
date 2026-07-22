package com.fatih.adaptivecompositioncamera.domain.model

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface CameraCapabilityRepository {
    val capabilityReport: Flow<CapabilityReport?>
    suspend fun refresh(): CapabilityReport
}

interface CameraController {
    suspend fun capturePhoto(): Result<Uri>
    suspend fun startVideo(): Result<Unit>
    suspend fun stopVideo(): Result<Uri?>
}

interface MediaRepository {
    fun createImageName(extension: String = "jpg"): String
    fun createVideoName(extension: String = "mp4"): String
}

interface StabilizationResolver {
    fun isSupported(capability: CameraCapability, mode: CameraMode): Boolean
}

interface ExtensionResolver {
    fun availableExtensions(capability: CameraCapability): ExtensionSupport
}

interface HighSpeedCapabilityProvider {
    fun options(capability: CameraCapability): List<HighSpeedVideoOption>
}

interface StorageMonitor {
    fun hasReasonableFreeSpace(): Boolean
}

interface ThermalMonitor {
    fun isCaptureSafe(): Boolean
}
