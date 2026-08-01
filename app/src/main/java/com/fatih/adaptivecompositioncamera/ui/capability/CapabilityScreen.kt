@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fatih.adaptivecompositioncamera.ui.capability

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraDiagnostics
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CapabilityReport

@Composable
fun CapabilityScreen(
    report: CapabilityReport?,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    diagnosticsMode: Boolean = false,
    activeMode: CameraMode = CameraMode.Photo,
    diagnostics: CameraDiagnostics = CameraDiagnostics(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val diagnosticText = remember(report, diagnostics, activeMode) {
        report?.let { buildDiagnosticText(it, diagnostics, activeMode) }.orEmpty()
    }
    val exportDiagnostics = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(diagnosticText)
                }
            }
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(if (diagnosticsMode) "Diagnostics" else "Camera information") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to settings") } },
                actions = {
                    IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Refresh camera information") }
                    IconButton(enabled = diagnosticText.isNotBlank(), onClick = {
                        clipboard.setText(AnnotatedString(diagnosticText))
                    }) {
                        Icon(Icons.Rounded.ContentCopy, "Copy diagnostics")
                    }
                    IconButton(
                        enabled = diagnosticText.isNotBlank(),
                        onClick = { exportDiagnostics.launch("AdaptiveCameraDiagnostics.txt") },
                    ) { Icon(Icons.Rounded.Share, "Export diagnostics as TXT") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                if (report != null) {
                    Info("Device", "${report.manufacturer} ${report.model}")
                    Info("Android", report.androidVersion)
                }
                Text(
                    if (diagnosticsMode) "Active mode: ${activeMode.name}. Values below are reported by Android and may differ from the manufacturer camera app."
                    else "Only capabilities exposed by Android are shown. Manufacturer-only camera paths are not assumed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (diagnosticsMode) item { RuntimeDiagnosticsSection(diagnostics) }
            if (report == null) item { Text("Scanning cameras...") }
            else items(report.cameras, key = { it.cameraId }) { camera -> CameraCapabilitySection(camera, diagnosticsMode) }
        }
    }
}

@Composable
private fun RuntimeDiagnosticsSection(diagnostics: CameraDiagnostics) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Active camera session", style = MaterialTheme.typography.titleLarge)
        Info("Current camera", diagnostics.currentCameraName ?: diagnostics.currentCameraId ?: "Unavailable")
        Info("Camera ID", diagnostics.currentCameraId ?: "Unavailable")
        Info("Session state", diagnostics.sessionState)
        Info("Current mode", diagnostics.mode.name)
        Info("Active resolution", diagnostics.activeResolution ?: "Unknown")
        Info("Requested native resolution", diagnostics.requestedResolution ?: "Unknown")
        Info("Bound ImageCapture resolution", diagnostics.boundCaptureResolution ?: "Unknown")
        Info("Actual saved output", diagnostics.actualSavedResolution ?: "No capture in this session")
        Info("Selected aspect ratio", diagnostics.selectedAspectRatio ?: "Unknown")
        Info("Sensor pixel mode", diagnostics.sensorPixelMode)
        Info("Configuration match", diagnostics.configurationMismatch ?: "No mismatch detected")
        Info("Preview resolution", diagnostics.previewResolution ?: "Reported at runtime")
        Info("Current FPS", diagnostics.currentFps ?: "Camera-managed")
        Info("Requested FPS", diagnostics.requestedFps ?: "Camera-managed")
        Info("Requested video quality", diagnostics.requestedVideoQuality ?: "Auto")
        Info("Supported video qualities", diagnostics.supportedVideoQualities ?: "Not queried")
        Info("Stabilization", diagnostics.stabilization)
        Info("Extension", diagnostics.extension)
        Info("Last camera error", diagnostics.lastCameraError ?: "None")
        Info("Last capture error", diagnostics.lastCaptureError ?: "None")
        Info("Last recording error", diagnostics.lastRecordingError ?: "None")
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CameraCapabilitySection(camera: CameraCapability, diagnosticsMode: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(camera.friendlyName, style = MaterialTheme.typography.titleLarge)
        Info("Camera ID", camera.cameraId)
        Info("Openable / logical / physical-only", "${camera.isOpenable} / ${camera.isLogical} / ${camera.isPhysicalOnly}")
        Info("Parent logical cameras", camera.parentLogicalCameraIds.joinToString().ifBlank { "None" })
        Info("Facing / role", "${camera.lensFacing.name} / ${camera.lensRole.name}")
        Info("Hardware support", camera.hardwareLevel.name)
        Info("Logical / physical", if (camera.physicalCameraIds.isEmpty()) "No physical IDs exposed" else camera.physicalCameraIds.joinToString())
        Info("Maximum JPEG", camera.jpegResolutions.firstOrNull()?.displayText() ?: "Unavailable")
        Info("Android high-resolution JPEG", camera.highResolutionJpegs.firstOrNull()?.displayText() ?: "Not exposed")
        Info("Maximum-resolution sensor mode", camera.maximumResolutionJpegs.firstOrNull()?.displayText() ?: "Not exposed")
        Info("All JPEG outputs", camera.jpegResolutions.joinToString { it.displayText() }.ifBlank { "Unavailable" })
        Info("All high-resolution JPEG", camera.highResolutionJpegs.joinToString { it.displayText() }.ifBlank { "Not exposed" })
        Info("All maximum-resolution JPEG", camera.maximumResolutionJpegs.joinToString { it.displayText() }.ifBlank { "Not exposed" })
        Info("Ultra-high-resolution sensor", camera.supportsUltraHighResolutionSensor.toString())
        Info("Sensor pixel modes", camera.sensorPixelModes.joinToString().ifBlank { "Not reported" })
        Info("HEIC / HEIF", camera.heicResolutions.firstOrNull()?.displayText() ?: "Not exposed")
        Info("Video outputs", camera.videoResolutions.take(8).joinToString { "${it.width}x${it.height}" }.ifBlank { "Unavailable" })
        Info("FPS ranges", camera.fpsRanges.joinToString().ifBlank { "Unknown" })
        Info("High-speed FPS", camera.highSpeedVideo.joinToString { "${it.width}x${it.height}@${it.maxFps}" }.ifBlank { "Not exposed" })
        Info("Aperture", camera.apertures.joinToString().ifBlank { "Unknown" })
        Info("Focal length", camera.focalLengths.joinToString().ifBlank { "Unknown" })
        Info("Zoom ratio", camera.zoomRatioRange ?: "CameraX runtime range")
        Info("Flash / torch", "${camera.hasFlash} / ${camera.hasTorch}")
        Info("OIS / EIS", "${camera.stabilization.optical} / ${camera.stabilization.electronicVideo}")
        Info("RAW / manual sensor", "${camera.supportsRaw} / ${camera.supportsManualSensor}")
        Info("Extensions", extensionText(camera))
        if (diagnosticsMode) {
            Info("Sensor orientation", camera.sensorOrientation?.toString() ?: "Unknown")
            Info("Active array", camera.activeArray ?: "Unknown")
            Info("Pre-correction active array", camera.preCorrectionActiveArray ?: "Unknown")
            Info("Pixel array", camera.pixelArray ?: "Unknown")
            Info("Maximum pixel array", camera.maximumPixelArray ?: "Not exposed")
            Info("Physical sensor size", camera.physicalSize ?: "Unknown")
            Info("ISO", camera.isoRange ?: "Unavailable")
            Info("Exposure time", camera.exposureTimeRange ?: "Unavailable")
            Info("Exposure compensation", camera.exposureCompensationRange ?: "Unavailable")
            Info("Exposure compensation step", camera.exposureCompensationStep ?: "Unavailable")
            Info("Maximum digital zoom", camera.maximumDigitalZoom?.toString() ?: "Unavailable")
            Info("AF modes", camera.autofocusModes.joinToString().ifBlank { "Not reported" })
            Info("AE modes", camera.autoExposureModes.joinToString().ifBlank { "Not reported" })
            Info("AWB modes", camera.autoWhiteBalanceModes.joinToString().ifBlank { "Not reported" })
            Info("EIS modes", camera.videoStabilizationModes.joinToString().ifBlank { "Not reported" })
            Info("OIS modes", camera.opticalStabilizationModes.joinToString().ifBlank { "Not reported" })
            Info("Capture request keys", camera.availableCaptureRequestKeys.joinToString().ifBlank { "Not reported" })
            Info("YUV outputs", camera.yuvResolutions.take(8).joinToString { "${it.width}x${it.height}" }.ifBlank { "Unavailable" })
        }
        camera.unavailableReasons.forEach { reason ->
            Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.58f))
    }
}

private fun com.fatih.adaptivecompositioncamera.domain.model.CameraResolution.displayText(): String =
    "$megapixelLabel - $width x $height - $aspectRatioLabel - $format"

private fun extensionText(camera: CameraCapability): String = buildList {
    if (camera.extensions.hdr) add("HDR")
    if (camera.extensions.night) add("Night")
    if (camera.extensions.bokeh) add("Bokeh")
    if (camera.extensions.faceRetouch) add("Face retouch")
}.joinToString().ifBlank { "Extensions are not enabled in this build" }

private fun buildDiagnosticText(
    report: CapabilityReport,
    diagnostics: CameraDiagnostics,
    activeMode: CameraMode,
): String = buildString {
    appendLine("Adaptive Composition Camera diagnostics")
    appendLine("Generated: ${java.util.Date(report.generatedAtEpochMillis)}")
    appendLine("Device: ${report.manufacturer} ${report.model}")
    appendLine("Android: ${report.androidVersion}")
    appendLine("Package: ${report.appPackage}")
    appendLine("Active mode: ${activeMode.name}")
    appendLine("Session: ${diagnostics.sessionState}")
    appendLine("Current camera: ${diagnostics.currentCameraName ?: diagnostics.currentCameraId ?: "Unavailable"}")
    appendLine("Requested photo: ${diagnostics.requestedResolution ?: "Unknown"}")
    appendLine("Bound photo: ${diagnostics.boundCaptureResolution ?: "Unknown"}")
    appendLine("Saved photo: ${diagnostics.actualSavedResolution ?: "No capture in this session"}")
    appendLine("Video: ${diagnostics.requestedVideoQuality ?: "Auto"} @ ${diagnostics.requestedFps ?: "Camera-managed"}")
    appendLine("Supported video: ${diagnostics.supportedVideoQualities ?: "Not queried"}")
    appendLine("Stabilization: ${diagnostics.stabilization}")
    appendLine("Last camera error: ${diagnostics.lastCameraError ?: "None"}")
    appendLine("Last capture error: ${diagnostics.lastCaptureError ?: "None"}")
    appendLine("Last recording error: ${diagnostics.lastRecordingError ?: "None"}")
    report.cameras.forEach { camera ->
        appendLine()
        appendLine("[${camera.friendlyName}]")
        appendLine("cameraId=${camera.cameraId}")
        appendLine("openable=${camera.isOpenable}; logical=${camera.isLogical}; physicalOnly=${camera.isPhysicalOnly}")
        appendLine("physicalIds=${camera.physicalCameraIds.joinToString()}")
        appendLine("parentLogicalIds=${camera.parentLogicalCameraIds.joinToString()}")
        appendLine("facing=${camera.lensFacing}; role=${camera.lensRole}; hardware=${camera.hardwareLevel}")
        appendLine("focalLengths=${camera.focalLengths.joinToString()}; apertures=${camera.apertures.joinToString()}")
        appendLine("activeArray=${camera.activeArray}; pixelArray=${camera.pixelArray}; maximumPixelArray=${camera.maximumPixelArray}")
        appendLine("jpeg=${camera.jpegResolutions.joinToString { it.displayText() }}")
        appendLine("highResolutionJpeg=${camera.highResolutionJpegs.joinToString { it.displayText() }}")
        appendLine("maximumResolutionJpeg=${camera.maximumResolutionJpegs.joinToString { it.displayText() }}")
        appendLine("videoOutputs=${camera.videoResolutions.joinToString { "${it.width}x${it.height}" }}")
        appendLine("fpsRanges=${camera.fpsRanges.joinToString()}")
        appendLine("eisModes=${camera.videoStabilizationModes.joinToString()}; oisModes=${camera.opticalStabilizationModes.joinToString()}")
        appendLine("manualSensor=${camera.supportsManualSensor}; raw=${camera.supportsRaw}; maximumZoom=${camera.maximumDigitalZoom}")
        appendLine("AF=${camera.autofocusModes.joinToString()}; AE=${camera.autoExposureModes.joinToString()}; AWB=${camera.autoWhiteBalanceModes.joinToString()}")
        appendLine("captureRequestKeys=${camera.availableCaptureRequestKeys.joinToString()}")
        camera.unavailableReasons.forEach { appendLine("reason=$it") }
    }
}
