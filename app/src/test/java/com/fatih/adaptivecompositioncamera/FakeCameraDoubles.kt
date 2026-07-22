package com.fatih.adaptivecompositioncamera

import android.net.Uri
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapabilityRepository
import com.fatih.adaptivecompositioncamera.domain.model.CameraController
import com.fatih.adaptivecompositioncamera.domain.model.CapabilityReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCameraCapabilityRepository(initial: CapabilityReport? = null) : CameraCapabilityRepository {
    private val state = MutableStateFlow(initial)
    override val capabilityReport: Flow<CapabilityReport?> = state

    override suspend fun refresh(): CapabilityReport {
        return state.value ?: CapabilityReport(0L, "fake", emptyList()).also { state.value = it }
    }
}

class FakeCameraController : CameraController {
    var photoUri: Uri? = null
    var videoUri: Uri? = null
    var recording = false

    override suspend fun capturePhoto(): Result<Uri> {
        return photoUri?.let { Result.success(it) } ?: Result.failure(IllegalStateException("No fake photo URI configured."))
    }

    override suspend fun startVideo(): Result<Unit> {
        recording = true
        return Result.success(Unit)
    }

    override suspend fun stopVideo(): Result<Uri?> {
        recording = false
        return Result.success(videoUri)
    }
}
