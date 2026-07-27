@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fatih.adaptivecompositioncamera.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.fatih.adaptivecompositioncamera.composition.CompositionGuideOverlay
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.CompositionGuide
import com.fatih.adaptivecompositioncamera.domain.model.ForegroundZone
import com.fatih.adaptivecompositioncamera.domain.model.GuideLineStyle
import com.fatih.adaptivecompositioncamera.domain.model.GuideStyle
import com.fatih.adaptivecompositioncamera.domain.model.PhotoAspectRatio
import com.fatih.adaptivecompositioncamera.domain.model.PhotoQualityPreset
import com.fatih.adaptivecompositioncamera.domain.model.SpiralOrientation
import com.fatih.adaptivecompositioncamera.domain.model.VideoFpsRange
import com.fatih.adaptivecompositioncamera.domain.model.VideoQualitySetting
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
import com.fatih.adaptivecompositioncamera.utility.CameraMath

@Composable
fun AspectRatioSheet(
    sourceResolution: CameraResolution?,
    selected: PhotoAspectRatio,
    viewportWidth: Int,
    viewportHeight: Int,
    onSelect: (PhotoAspectRatio) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Aspect ratio", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Native capture size and output crop are separate. A crop is never presented as full-sensor resolution.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            PhotoAspectRatio.entries.forEach { option ->
                RatioRow(option, sourceResolution, viewportWidth, viewportHeight, selected == option) {
                    onSelect(option)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun RatioRow(
    option: PhotoAspectRatio,
    sourceResolution: CameraResolution?,
    viewportWidth: Int,
    viewportHeight: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dimensions = sourceResolution?.let {
        CameraMath.cropDimensions(it.width, it.height, option, viewportWidth, viewportHeight)
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(option.label(), style = MaterialTheme.typography.titleMedium)
            Text(
                dimensions?.let { (width, height) ->
                    val kind = if (option == PhotoAspectRatio.FullSensor) "native" else "crop"
                    "${CameraMath.megapixels(width, height)} MP - $width x $height - $kind JPEG"
                } ?: "Available after camera discovery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected")
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}

@Composable
fun ResolutionSheet(
    resolutions: List<CameraResolution>,
    selected: CameraResolution?,
    reportedMaximum: CameraResolution?,
    onSelect: (CameraResolution) -> Unit,
    onDismiss: () -> Unit,
) {
    val groups = listOf(
        "Maximum available" to resolutions.filter { it.maximumSensorMode || it.highResolution },
        "Recommended" to resolutions.filter { it.recommended && !it.highResolution && !it.maximumSensorMode },
        "Other" to resolutions.filterNot { it.highResolution || it.recommended || it.maximumSensorMode },
    ).filter { it.second.isNotEmpty() }
    val unavailableMaximum = reportedMaximum?.takeIf { maximum ->
        maximum.maximumSensorMode && resolutions.none {
            it.width == maximum.width && it.height == maximum.height
        }
    }
    val presets = emptyList<Pair<PhotoQualityPreset, CameraResolution>>()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Photo resolution", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Only real Android camera outputs are shown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            if (unavailableMaximum != null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null)
                        Column {
                            Text(
                                "${unavailableMaximum.megapixelLabel} sensor mode detected",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "${unavailableMaximum.width} x ${unavailableMaximum.height} is reported only in Android's maximum-sensor map. " +
                                    "It is not shown as selectable until a valid capture session can use it.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (presets.isNotEmpty()) {
            Text(
                "QUALITY PRESETS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                items(presets, key = { it.first.name }) { (preset, resolution) ->
                    FilterChip(
                        selected = selected?.id == resolution.id,
                        onClick = { onSelect(resolution) },
                        label = {
                            Column {
                                Text(preset.photoLabel())
                                Text(
                                    "${resolution.megapixelLabel} · ${resolution.width}×${resolution.height}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                    )
                }
            }
            Text(
                "CUSTOM RESOLUTION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            }
            LazyColumn {
                groups.forEach { (title, options) ->
                    item(key = "section:$title") {
                        Text(
                            title.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 3.dp),
                        )
                    }
                    items(options, key = { it.id }) { resolution ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                onSelect(resolution)
                                onDismiss()
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SelectionDot(selected?.id == resolution.id)
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(resolution.megapixelLabel, style = MaterialTheme.typography.titleMedium)
                                    when {
                                        resolution.maximumSensorMode || resolution.highResolution -> Badge("Maximum")
                                        resolution.recommended -> Badge("Recommended")
                                        resolution.maximum -> Badge("Largest")
                                    }
                                }
                                Text(
                                    "${resolution.width} x ${resolution.height} - ${resolution.aspectRatioLabel} - ${resolution.format}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (resolution.maximumSensorMode) {
                                    Text(
                                        "Slower capture, larger files",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                } else if (resolution.highResolution) {
                                    Text(
                                        "High-resolution capture",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(22.dp)) {
        drawCircle(
            color.copy(alpha = if (selected) 1f else 0.65f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
        )
        if (selected) drawCircle(color, radius = size.minDimension * 0.26f)
    }
}

@Composable
private fun Badge(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraSmall) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun VideoSettingsSheet(
    mode: CameraMode,
    supportedQualities: List<VideoQualitySetting>,
    selectedQuality: VideoQualitySetting,
    fpsRanges: List<VideoFpsRange>,
    selectedFpsRange: VideoFpsRange?,
    stabilizationModes: List<VideoStabilizationMode>,
    selectedStabilization: VideoStabilizationMode,
    stabilizationStatus: String,
    onQuality: (VideoQualitySetting) -> Unit,
    onFps: (VideoFpsRange) -> Unit,
    onStabilization: (VideoStabilizationMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            val videoMode = mode in setOf(
                CameraMode.Video,
                CameraMode.SlowMotion,
                CameraMode.HighFrameRate,
                CameraMode.TimeLapse,
            )
            Text(if (videoMode) "Video configuration" else "Stabilization", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (videoMode) {
                    "Only CameraX qualities and Camera2 FPS/stabilization modes exposed by this lens are listed."
                } else {
                    "Only stabilization modes exposed by Android for this lens and mode are listed."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            if (videoMode) {
                Text("QUALITY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (listOf(VideoQualitySetting.Auto) + supportedQualities).distinct().forEach { quality ->
                        FilterChip(
                            selected = selectedQuality == quality,
                            onClick = { onQuality(quality) },
                            label = { Text(quality.videoLabel()) },
                        )
                    }
                }
                if (supportedQualities.isEmpty()) {
                    Text("Waiting for CameraX quality discovery.", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "FRAME RATE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    fpsRanges.forEach { range ->
                        FilterChip(
                            selected = selectedFpsRange == range,
                            onClick = { onFps(range) },
                            label = { Text(range.label) },
                        )
                    }
                }
                if (fpsRanges.isEmpty()) {
                    Text("Frame rate is camera-managed; no standard reported range can be selected.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "STABILIZATION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 14.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stabilizationModes.forEach { mode ->
                    FilterChip(
                        selected = selectedStabilization == mode,
                        enabled = mode != VideoStabilizationMode.Unsupported,
                        onClick = { onStabilization(mode) },
                        label = { Text(mode.stabilizationLabel()) },
                    )
                }
            }
            Text(
                stabilizationStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (
                selectedStabilization in listOf(
                    VideoStabilizationMode.Standard,
                    VideoStabilizationMode.Preview,
                    VideoStabilizationMode.Auto,
                )
            ) {
                Text(
                    "Electronic stabilization may crop the field of view. Availability can change with quality and FPS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Button(onClick = onDismiss, modifier = Modifier.padding(top = 18.dp)) { Text("Done") }
        }
    }
}

private fun PhotoQualityPreset.photoLabel(): String = when (this) {
    PhotoQualityPreset.Maximum -> "Maximum"
    PhotoQualityPreset.High -> "High"
    PhotoQualityPreset.Medium -> "Medium"
    PhotoQualityPreset.StorageSaver -> "Storage saver"
    PhotoQualityPreset.Custom -> "Custom"
}

private fun VideoQualitySetting.videoLabel(): String = when (this) {
    VideoQualitySetting.Auto -> "Auto"
    VideoQualitySetting.UHD -> "4K"
    VideoQualitySetting.FHD -> "1080p"
    VideoQualitySetting.HD -> "720p"
    VideoQualitySetting.SD -> "480p"
}

private fun VideoStabilizationMode.stabilizationLabel(): String = when (this) {
    VideoStabilizationMode.Off -> "Off"
    VideoStabilizationMode.Standard -> "Standard EIS"
    VideoStabilizationMode.Preview -> "Preview"
    VideoStabilizationMode.Optical -> "Optical"
    VideoStabilizationMode.Auto -> "Auto"
    VideoStabilizationMode.Unsupported -> "Unsupported"
}

private fun formatBytes(bytes: Long): String =
    if (bytes >= 1_048_576L) "${"%.1f".format(bytes / 1_048_576.0)} MB" else "${bytes / 1024} KB"

@Composable
fun CompositionSheet(
    selected: CompositionGuide,
    style: GuideStyle,
    onSelect: (CompositionGuide) -> Unit,
    onStyle: (GuideStyle) -> Unit,
    onResetInteractive: () -> Unit,
    onVanishingPreset: (Offset) -> Unit,
    onFramePreset: (Rect) -> Unit,
    onEyeLinePreset: (Float) -> Unit,
    onLevelCalibrate: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Composition guide", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                "${professionalGuideCatalog.size} photographic guides · Swipe sideways, then tap to apply",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp).padding(top = 3.dp, bottom = 12.dp),
            )
            GuideCatalogGrid(
                selected = selected,
                style = style,
                onSelect = { guide ->
                    onSelect(guide)
                    onDismiss()
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            Text("Guide appearance", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                guideColors.forEach { argb ->
                    val color = Color(argb)
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(color)
                            .clickable { onStyle(style.copy(colorArgb = argb)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (style.colorArgb == argb) Icon(Icons.Rounded.Check, "Selected guide color", tint = if (argb == 0xFF000000) Color.White else Color.Black)
                    }
                }
            }
            Text("Opacity ${(style.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(value = style.opacity, onValueChange = { onStyle(style.copy(opacity = it)) }, valueRange = 0.2f..1f)
            Text("Thickness ${"%.1f".format(style.thicknessDp)} dp", style = MaterialTheme.typography.labelLarge)
            Slider(value = style.thicknessDp, onValueChange = { onStyle(style.copy(thicknessDp = it)) }, valueRange = 0.8f..4f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = style.lineStyle == GuideLineStyle.Solid,
                    onClick = { onStyle(style.copy(lineStyle = GuideLineStyle.Solid)) },
                    label = { Text("Solid") },
                )
                FilterChip(
                    selected = style.lineStyle == GuideLineStyle.Dashed,
                    onClick = { onStyle(style.copy(lineStyle = GuideLineStyle.Dashed)) },
                    label = { Text("Dashed") },
                )
                FilterChip(
                    selected = style.outline,
                    onClick = { onStyle(style.copy(outline = !style.outline)) },
                    label = { Text("Outline") },
                )
            }
            Text("Orientation", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(0, 90, 180, 270).forEach { degrees ->
                    FilterChip(
                        selected = style.overlayRotationDegrees == degrees,
                        onClick = { onStyle(style.copy(overlayRotationDegrees = degrees)) },
                        label = { Text("$degrees°") },
                    )
                }
                FilterChip(
                    selected = style.overlayMirrorHorizontal,
                    onClick = { onStyle(style.copy(overlayMirrorHorizontal = !style.overlayMirrorHorizontal)) },
                    label = { Text("Mirror") },
                )
                FilterChip(
                    selected = style.overlayLocked,
                    onClick = { onStyle(style.copy(overlayLocked = !style.overlayLocked)) },
                    label = { Text(if (style.overlayLocked) "Locked" else "Lock") },
                )
            }
            if (selected == CompositionGuide.GoldenSpiral) {
                Text("Spiral orientation", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpiralOrientation.entries.forEach { orientation ->
                        FilterChip(
                            selected = style.spiralOrientation == orientation,
                            onClick = { onStyle(style.copy(spiralOrientation = orientation)) },
                            label = { Text(orientation.shortLabel()) },
                        )
                    }
                }
                FilterChip(
                    selected = style.spiralClockwise,
                    onClick = { onStyle(style.copy(spiralClockwise = !style.spiralClockwise)) },
                    label = { Text(if (style.spiralClockwise) "Clockwise" else "Counterclockwise") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = style.spiralHorizontalFlip,
                        onClick = { onStyle(style.copy(spiralHorizontalFlip = !style.spiralHorizontalFlip)) },
                        label = { Text("Flip horizontal") },
                    )
                    FilterChip(
                        selected = style.spiralVerticalFlip,
                        onClick = { onStyle(style.copy(spiralVerticalFlip = !style.spiralVerticalFlip)) },
                        label = { Text("Flip vertical") },
                    )
                }
            }
            if (selected == CompositionGuide.VanishingPoint) {
                Text("Perspective lines: ${style.vanishingLineCount}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Slider(
                    value = style.vanishingLineCount.toFloat(),
                    onValueChange = { onStyle(style.copy(vanishingLineCount = it.toInt())) },
                    valueRange = 3f..13f,
                    steps = 9,
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Left" to Offset(0.24f, 0.48f),
                        "Center" to Offset(0.5f, 0.48f),
                        "Right" to Offset(0.76f, 0.48f),
                        "Upper" to Offset(0.5f, 0.28f),
                        "Lower" to Offset(0.5f, 0.68f),
                    ).forEach { (label, point) ->
                        FilterChip(selected = false, onClick = { onVanishingPreset(point) }, label = { Text(label) })
                    }
                }
            }
            if (selected == CompositionGuide.FrameInFrame) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = style.frameDimOutside,
                        onClick = { onStyle(style.copy(frameDimOutside = !style.frameDimOutside)) },
                        label = { Text("Dim outside") },
                    )
                    FilterChip(
                        selected = style.frameCornerDp > 0f,
                        onClick = { onStyle(style.copy(frameCornerDp = if (style.frameCornerDp > 0f) 0f else 12f)) },
                        label = { Text("Rounded") },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = false, onClick = { onFramePreset(Rect(0.10f, 0.14f, 0.90f, 0.86f)) }, label = { Text("Large") })
                    FilterChip(selected = false, onClick = { onFramePreset(Rect(0.18f, 0.20f, 0.82f, 0.80f)) }, label = { Text("Medium") })
                    FilterChip(selected = false, onClick = { onFramePreset(Rect(0.28f, 0.30f, 0.72f, 0.70f)) }, label = { Text("Small") })
                }
            }
            if (selected == CompositionGuide.TextureRepetition) {
                Text("Grid density", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(4, 6, 8, 10).forEach { cells ->
                        FilterChip(
                            selected = style.textureGridSize == cells,
                            onClick = { onStyle(style.copy(textureGridSize = cells)) },
                            label = { Text("${cells}×${cells}") },
                        )
                    }
                }
                FilterChip(
                    selected = style.textureDiagonals,
                    onClick = { onStyle(style.copy(textureDiagonals = !style.textureDiagonals)) },
                    label = { Text("Diagonal guides") },
                )
            }
            if (selected == CompositionGuide.Foreground) {
                Text("Foreground zone", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForegroundZone.entries.forEach { zone ->
                        FilterChip(
                            selected = style.foregroundZone == zone,
                            onClick = { onStyle(style.copy(foregroundZone = zone)) },
                            label = { Text(zone.name) },
                        )
                    }
                }
                Text("Zone size ${(style.foregroundFraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = style.foregroundFraction,
                    onValueChange = { onStyle(style.copy(foregroundFraction = it)) },
                    valueRange = 0.18f..0.48f,
                )
            }
            if (selected == CompositionGuide.EyeLine) {
                FilterChip(
                    selected = style.faceSafeArea,
                    onClick = { onStyle(style.copy(faceSafeArea = !style.faceSafeArea)) },
                    label = { Text("Face-safe area") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = false, onClick = { onEyeLinePreset(1f / 3f) }, label = { Text("Upper third") })
                    FilterChip(selected = false, onClick = { onEyeLinePreset(0.5f) }, label = { Text("Center") })
                }
            }
            if (selected == CompositionGuide.Centered) {
                FilterChip(
                    selected = style.centeredTarget,
                    onClick = { onStyle(style.copy(centeredTarget = !style.centeredTarget)) },
                    label = { Text("Circular target") },
                )
            }
            if (selected == CompositionGuide.HorizonLevel) {
                Text(
                    "Calibration treats the current device position as level. Select None to disable sensor use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                OutlinedButton(onClick = onLevelCalibrate) { Text("Calibrate level") }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    onStyle(GuideStyle())
                    onResetInteractive()
                }) { Text("Reset") }
                Button(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun GuideCatalogGrid(
    selected: CompositionGuide,
    style: GuideStyle,
    onSelect: (CompositionGuide) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(professionalGuideCatalog, key = { it.name }) { guide ->
            GuideTile(
                guide = guide,
                selected = guide == selected,
                style = style,
                onClick = { onSelect(guide) },
                modifier = Modifier.width(174.dp),
            )
        }
    }
}

@Composable
private fun GuideTile(
    guide: CompositionGuide,
    selected: Boolean,
    style: GuideStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 156.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.45f).background(Color(0xFF24282D))) {
                CompositionGuideOverlay(guide, mirrored = false, style = style.copy(thicknessDp = 1f, opacity = 0.8f))
                if (selected) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(24.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected guide",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }
            Text(guide.title(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 7.dp))
            Text(guide.description(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

internal val professionalGuideCatalog = listOf(
    CompositionGuide.None,
    CompositionGuide.RuleOfThirds,
    CompositionGuide.LeadingLines,
    CompositionGuide.VanishingPoint,
    CompositionGuide.GoldenRatio,
    CompositionGuide.GoldenSpiral,
    CompositionGuide.FrameInFrame,
    CompositionGuide.Centered,
    CompositionGuide.Symmetry,
    CompositionGuide.Diagonal,
    CompositionGuide.GoldenTriangle,
    CompositionGuide.TextureRepetition,
    CompositionGuide.Foreground,
    CompositionGuide.EyeLine,
    CompositionGuide.HorizonLevel,
)

@Composable
fun MoreModesSheet(
    modes: List<CameraMode>,
    activeMode: CameraMode,
    maxResolution: CameraResolution?,
    onSelect: (CameraMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("More modes", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Only modes with a working capture path for this lens are shown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
            )
            val additional = modes.filterNot { it in listOf(CameraMode.Portrait, CameraMode.Photo, CameraMode.Video) }
            if (additional.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null)
                        Column {
                            Text("No extra modes for this lens", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Photo and Video remain available. Advanced modes appear only when Android exposes a compatible capture configuration.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                additional.chunked(2).forEach { rowModes ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowModes.forEach { mode ->
                            val selected = mode == activeMode
                            Surface(
                                onClick = {
                                    onSelect(mode)
                                    onDismiss()
                                },
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.weight(1f).aspectRatio(1.45f),
                            ) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Icon(mode.icon(), contentDescription = null, modifier = Modifier.size(28.dp))
                                        if (selected) Icon(Icons.Rounded.Check, "Selected mode")
                                    }
                                    Column {
                                        Text(mode.label(maxResolution), style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            mode.description(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                }
                            }
                        }
                        if (rowModes.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private val guideColors = listOf(
    0xFFFFFFFF,
    0xFF000000,
    0xFFFF5252,
    0xFFFFD740,
    0xFF69F0AE,
    0xFF18FFFF,
)

fun CompositionGuide.title(): String = when (this) {
    CompositionGuide.None -> "None"
    CompositionGuide.RuleOfThirds -> "Rule of Thirds"
    CompositionGuide.LeadingLines -> "Leading Lines"
    CompositionGuide.VanishingPoint -> "Vanishing Point"
    CompositionGuide.GoldenRatio -> "Golden Ratio"
    CompositionGuide.GoldenSpiral -> "Golden Spiral"
    CompositionGuide.FrameInFrame -> "Frame in a Frame"
    CompositionGuide.Centered -> "Centered"
    CompositionGuide.Symmetry -> "Symmetry"
    CompositionGuide.Diagonal -> "Diagonal"
    CompositionGuide.GoldenTriangle -> "Golden Triangle"
    CompositionGuide.TextureRepetition -> "Texture & Repetition"
    CompositionGuide.Foreground -> "Foreground"
    CompositionGuide.EyeLine -> "Eye Line"
    CompositionGuide.HorizonLevel -> "Horizon & Level"
}

private fun CompositionGuide.description(): String = when (this) {
    CompositionGuide.None -> "Unobstructed preview"
    CompositionGuide.RuleOfThirds -> "Balance subjects on thirds"
    CompositionGuide.LeadingLines -> "Guide attention into the frame"
    CompositionGuide.VanishingPoint -> "Movable manual perspective"
    CompositionGuide.GoldenRatio -> "Phi-based alignment grid"
    CompositionGuide.GoldenSpiral -> "Golden rectangle arc sequence"
    CompositionGuide.FrameInFrame -> "Movable inner framing area"
    CompositionGuide.Centered -> "Symmetry and center target"
    CompositionGuide.Symmetry -> "Mirror balance across the center"
    CompositionGuide.Diagonal -> "Dynamic diagonal alignment"
    CompositionGuide.GoldenTriangle -> "Diagonal golden-section balance"
    CompositionGuide.TextureRepetition -> "Manual repeating-pattern grid"
    CompositionGuide.Foreground -> "Separate foreground placement"
    CompositionGuide.EyeLine -> "Portrait and selfie eye line"
    CompositionGuide.HorizonLevel -> "Live roll and pitch sensor"
}

fun CameraMode.label(maxResolution: CameraResolution? = null): String = when (this) {
    CameraMode.Portrait -> "Portrait"
    CameraMode.Photo -> "Photo"
    CameraMode.Video -> "Video"
    CameraMode.Pro -> "Pro"
    CameraMode.Documents -> "Documents"
    CameraMode.Night -> "Night"
    CameraMode.MaximumResolution -> maxResolution?.megapixelLabel ?: "Max Resolution"
    CameraMode.SlowMotion -> "Slow Motion"
    CameraMode.HighFrameRate -> "High Frame Rate"
    CameraMode.TimeLapse -> "Time-lapse"
    CameraMode.Burst -> "Burst"
    CameraMode.PanoramaExperimental -> "Panorama"
}

private fun CameraMode.description(): String = when (this) {
    CameraMode.Pro -> "Manual controls supported by this camera"
    CameraMode.Documents -> "Guided document capture saved to gallery"
    CameraMode.MaximumResolution -> "Slower capture and larger files"
    CameraMode.SlowMotion -> "Uses an exposed high-speed configuration"
    CameraMode.HighFrameRate -> "Records using supported high-speed FPS"
    CameraMode.TimeLapse -> "Interval capture mode"
    CameraMode.Burst -> "Continuous capture support is exposed"
    CameraMode.PanoramaExperimental -> "Experimental guided panorama"
    CameraMode.Night -> "Camera extension exposed by Android"
    CameraMode.Portrait -> "Bokeh camera extension"
    CameraMode.Photo -> "Standard still capture"
    CameraMode.Video -> "Standard video recording"
}

private fun CameraMode.icon() = when (this) {
    CameraMode.Pro -> Icons.Rounded.Tune
    CameraMode.Documents -> Icons.Rounded.DocumentScanner
    CameraMode.MaximumResolution -> Icons.Rounded.HighQuality
    else -> Icons.Rounded.CameraAlt
}

private fun SpiralOrientation.shortLabel(): String = when (this) {
    SpiralOrientation.TopLeft -> "TL"
    SpiralOrientation.TopRight -> "TR"
    SpiralOrientation.BottomLeft -> "BL"
    SpiralOrientation.BottomRight -> "BR"
}

fun PhotoAspectRatio.label(): String = when (this) {
    PhotoAspectRatio.FullSensor -> "Full sensor"
    PhotoAspectRatio.Ratio4x3 -> "4:3"
    PhotoAspectRatio.Ratio3x2 -> "3:2"
    PhotoAspectRatio.Ratio16x9 -> "16:9"
    PhotoAspectRatio.Ratio1x1 -> "1:1"
    PhotoAspectRatio.FullScreen -> "Full screen"
}
