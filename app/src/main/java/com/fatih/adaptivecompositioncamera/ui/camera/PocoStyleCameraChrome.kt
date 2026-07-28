package com.fatih.adaptivecompositioncamera.ui.camera

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.FlashMode
import com.fatih.adaptivecompositioncamera.domain.model.LensRole
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
import kotlin.math.abs

@Composable
fun PocoStyleTopControls(
    hasFlash: Boolean,
    flashMode: FlashMode,
    onFlash: () -> Unit,
    timerSeconds: Int,
    onTimer: () -> Unit,
    quickSettingsExpanded: Boolean,
    onQuickSettings: () -> Unit,
    captureFormatControlsVisible: Boolean,
    aspectRatioLabel: String?,
    resolutionLabel: String?,
    onAspectRatio: () -> Unit,
    onResolution: () -> Unit,
    stabilizationLabel: String?,
    onStabilization: () -> Unit,
    compositionVisible: Boolean,
    compositionActive: Boolean,
    onComposition: () -> Unit,
    onSettings: () -> Unit,
    controlRotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(CameraUiTokens.topGap),
    ) {
        if (hasFlash) {
            PocoTopControl(
                icon = when (flashMode) {
                    FlashMode.Off -> Icons.Rounded.FlashOff
                    FlashMode.Auto -> Icons.Rounded.FlashAuto
                    FlashMode.On, FlashMode.Torch -> Icons.Rounded.FlashOn
                },
                description = "Flash ${flashMode.name}",
                label = flashMode.takeUnless { it == FlashMode.Off }?.name,
                onClick = onFlash,
                rotationDegrees = controlRotationDegrees,
            )
        }
        PocoTopControl(
            icon = if (quickSettingsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            description = "Quick camera controls",
            label = null,
            onClick = onQuickSettings,
            active = quickSettingsExpanded,
            rotationDegrees = controlRotationDegrees,
        )
        PocoTopControl(Icons.Rounded.Timer, "Self timer", if (timerSeconds == 0) null else "${timerSeconds}s", onTimer, rotationDegrees = controlRotationDegrees)
        if (captureFormatControlsVisible) {
            PocoTopControl(Icons.Rounded.AspectRatio, "Aspect ratio", aspectRatioLabel, onAspectRatio, rotationDegrees = controlRotationDegrees)
            PocoTopControl(Icons.Rounded.PhotoSizeSelectLarge, "Capture resolution", resolutionLabel, onResolution, rotationDegrees = controlRotationDegrees)
        }
        if (stabilizationLabel != null) {
            PocoStabilizationControl(
                value = stabilizationLabel,
                description = "Stabilization selector. Current result $stabilizationLabel",
                onClick = onStabilization,
                active = stabilizationLabel !in setOf("OFF", "N/A", "WAIT"),
                rotationDegrees = controlRotationDegrees,
            )
        }
        if (compositionVisible) {
            PocoTopControl(
                icon = Icons.Rounded.GridOn,
                description = "Composition guides",
                label = null,
                onClick = onComposition,
                active = compositionActive,
                rotationDegrees = controlRotationDegrees,
            )
        }
        PocoTopControl(Icons.Rounded.Settings, "Settings", null, onSettings, rotationDegrees = controlRotationDegrees)
    }
}

@Composable
fun PocoStyleQuickSettings(
    hasAspectRatio: Boolean,
    hasResolution: Boolean,
    hasComposition: Boolean,
    hasStabilization: Boolean,
    aspectRatioLabel: String?,
    resolutionLabel: String?,
    stabilizationLabel: String?,
    onAspectRatio: () -> Unit,
    onResolution: () -> Unit,
    onComposition: () -> Unit,
    onStabilization: () -> Unit,
    onSettings: () -> Unit,
    controlRotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 420.dp),
        color = Color.Black.copy(alpha = 0.62f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (hasAspectRatio) PocoQuickItem(Icons.Rounded.AspectRatio, aspectRatioLabel ?: "Aspect", "Aspect", onAspectRatio, controlRotationDegrees)
                if (hasResolution) PocoQuickItem(Icons.Rounded.PhotoSizeSelectLarge, resolutionLabel ?: "MP", "Resolution", onResolution, controlRotationDegrees)
                if (hasComposition) PocoQuickItem(Icons.Rounded.GridOn, "Grid", "Composition", onComposition, controlRotationDegrees)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (hasStabilization) {
                    PocoQuickItem(
                        Icons.Rounded.CameraAlt,
                        "Stab ${stabilizationLabel ?: "OFF"}",
                        "Stabilization selector",
                        onStabilization,
                        controlRotationDegrees,
                    )
                }
                PocoQuickItem(Icons.Rounded.Settings, "Settings", "Settings", onSettings, controlRotationDegrees)
            }
        }
    }
}

@Composable
fun PocoStyleShutterControls(
    landscape: Boolean,
    activeMode: CameraMode,
    availableModes: List<CameraMode>,
    availableCameras: List<CameraCapability>,
    activeCameraId: String?,
    maxResolution: CameraResolution?,
    zoom: Float,
    minZoom: Float,
    maxZoom: Float,
    showZoomSlider: Boolean,
    onToggleZoomSlider: () -> Unit,
    onZoom: (Float) -> Unit,
    latestThumbnail: Bitmap?,
    hasLatestMedia: Boolean,
    onLatestMedia: () -> Unit,
    isRecording: Boolean,
    canSwitch: Boolean,
    onSwitch: () -> Unit,
    onCamera: (CameraCapability) -> Unit,
    onMode: (CameraMode) -> Unit,
    onMore: () -> Unit,
    onShutter: () -> Unit,
    controlRotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    if (landscape) {
        Row(
            modifier
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.50f))))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.width(CameraUiTokens.landscapeModeRailWidth).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PocoLandscapeModeSelector(activeMode, availableModes, maxResolution, onMode, onMore, controlRotationDegrees)
            }
            Column(
                Modifier.width(CameraUiTokens.landscapeCaptureRailWidth).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                if (availableCameras.size > 1) {
                    PocoCompactLensSelector(availableCameras, activeCameraId, onCamera, controlRotationDegrees)
                } else if (maxZoom > minZoom + 0.05f) {
                    PocoZoomButton(zoom, selected = true, controlRotationDegrees) { onToggleZoomSlider() }
                }
                AnimatedVisibility(showZoomSlider && maxZoom > minZoom + 0.05f) {
                    Slider(value = zoom, onValueChange = onZoom, valueRange = minZoom..maxZoom, modifier = Modifier.width(108.dp).height(30.dp))
                }
                PocoLatestMediaButton(latestThumbnail, hasLatestMedia, onLatestMedia)
                PocoShutterButton(videoMode = activeMode == CameraMode.Video, recording = isRecording, onClick = onShutter)
                PocoCameraSwitchButton(canSwitch, onSwitch)
            }
        }
    } else {
        Column(
            modifier
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f), Color.Black.copy(alpha = 0.74f))))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (availableCameras.size > 1) {
                PocoLensSelector(availableCameras, activeCameraId, onCamera, controlRotationDegrees)
            } else if (maxZoom > minZoom + 0.05f) {
                PocoQuickZoomRow(zoom, minZoom, maxZoom, onZoom, onToggleZoomSlider, controlRotationDegrees)
            }
            AnimatedVisibility(showZoomSlider && maxZoom > minZoom + 0.05f) {
                Slider(value = zoom, onValueChange = onZoom, valueRange = minZoom..maxZoom, modifier = Modifier.fillMaxWidth().height(30.dp))
            }
            Spacer(Modifier.height(4.dp))
            PocoStyleModeSelector(activeMode, availableModes, maxResolution, onMode, onMore)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PocoLatestMediaButton(latestThumbnail, hasLatestMedia, onLatestMedia)
                PocoShutterButton(videoMode = activeMode == CameraMode.Video, recording = isRecording, onClick = onShutter)
                PocoCameraSwitchButton(canSwitch, onSwitch)
            }
        }
    }
}

private fun VideoStabilizationMode.pocoShortLabel(): String = when (this) {
    VideoStabilizationMode.Off -> "OFF"
    VideoStabilizationMode.Standard -> "EIS"
    VideoStabilizationMode.Preview -> "PRE"
    VideoStabilizationMode.Optical -> "OIS"
    VideoStabilizationMode.Auto -> "AUTO"
    VideoStabilizationMode.Unsupported -> "N/A"
}

private val PocoAccent = Color(0xFFD6E66D)

@Composable
private fun PocoStabilizationControl(
    value: String,
    description: String,
    onClick: () -> Unit,
    active: Boolean,
    rotationDegrees: Float = 0f,
) {
    Column(Modifier.width(CameraUiTokens.secondaryTouchTarget), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Black.copy(alpha = if (active) 0.48f else 0.34f),
            modifier = Modifier
                .size(CameraUiTokens.minimumTouchTarget)
                .then(if (active) Modifier.border(1.5.dp, PocoAccent, CircleShape) else Modifier)
                .semantics { contentDescription = description },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    value,
                    color = if (active) PocoAccent else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.rotate(rotationDegrees),
                )
            }
        }
        Text(
            "STAB",
            color = if (active) PocoAccent else Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .rotate(rotationDegrees)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.56f))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun PocoTopControl(
    icon: ImageVector,
    description: String,
    label: String?,
    onClick: () -> Unit,
    active: Boolean = false,
    rotationDegrees: Float = 0f,
) {
    Column(Modifier.width(CameraUiTokens.minimumTouchTarget), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(CameraUiTokens.minimumTouchTarget).clip(CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(CameraUiTokens.topVisualSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = if (active) 0.46f else if (label != null) 0.34f else 0.24f))
                    .then(if (active) Modifier.border(1.5.dp, PocoAccent, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = description, tint = if (active) PocoAccent else Color.White, modifier = Modifier.size(CameraUiTokens.topIconSize).rotate(rotationDegrees))
            }
        }
        if (label != null) {
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                softWrap = false,
                modifier = Modifier
                    .rotate(rotationDegrees)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Black.copy(alpha = 0.60f))
                    .padding(
                        horizontal = CameraUiTokens.topLabelHorizontalPadding,
                        vertical = CameraUiTokens.topLabelVerticalPadding,
                    ),
            )
        }
    }
}

@Composable
private fun PocoQuickItem(icon: ImageVector, label: String, description: String, onClick: () -> Unit, rotationDegrees: Float) {
    Column(
        modifier = Modifier.heightIn(min = 66.dp).width(82.dp).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(24.dp).rotate(rotationDegrees))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp).rotate(rotationDegrees))
    }
}

@Composable
private fun PocoStyleModeSelector(activeMode: CameraMode, availableModes: List<CameraMode>, maxResolution: CameraResolution?, onMode: (CameraMode) -> Unit, onMore: () -> Unit) {
    val mainModes = listOf(CameraMode.Photo, CameraMode.Video).filter { it in availableModes }
    val visibleModes = if (activeMode in mainModes) mainModes else mainModes + activeMode
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visibleModes.forEach { mode ->
            PocoModeLabel(mode.label(maxResolution), mode == activeMode) { onMode(mode) }
        }
        PocoModeLabel("More", false, onMore)
    }
}

@Composable
private fun PocoLandscapeModeSelector(activeMode: CameraMode, availableModes: List<CameraMode>, maxResolution: CameraResolution?, onMode: (CameraMode) -> Unit, onMore: () -> Unit, rotationDegrees: Float) {
    val modes = listOf(CameraMode.Photo, CameraMode.Video).filter { it in availableModes }
    val visibleModes = if (activeMode in modes) modes else modes + activeMode
    visibleModes.forEach { mode ->
        val selected = mode == activeMode
        Text(
            text = mode.label(maxResolution),
            color = if (selected) Color(0xFFAEEA00) else Color.White.copy(alpha = 0.78f),
            style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.rotate(rotationDegrees).heightIn(min = CameraUiTokens.minimumTouchTarget).clickable { onMode(mode) }.padding(horizontal = 4.dp, vertical = 12.dp),
        )
    }
    Text("More", color = Color.White.copy(alpha = 0.78f), maxLines = 1, softWrap = false, modifier = Modifier.rotate(rotationDegrees).heightIn(min = CameraUiTokens.minimumTouchTarget).clickable(onClick = onMore).padding(horizontal = 4.dp, vertical = 12.dp))
}

@Composable
private fun PocoModeLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.heightIn(min = CameraUiTokens.minimumTouchTarget).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.68f),
            fontSize = if (selected) 20.sp else 18.sp,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(3.dp))
        Box(Modifier.width(26.dp).height(3.dp).clip(CircleShape).background(if (selected) PocoAccent else Color.Transparent))
    }
}

@Composable
private fun PocoLensSelector(cameras: List<CameraCapability>, activeCameraId: String?, onCamera: (CameraCapability) -> Unit, rotationDegrees: Float) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        cameras.forEach { camera ->
            val selected = camera.cameraId == activeCameraId
            val size = if (selected) CameraUiTokens.lensActiveVisibleSize else CameraUiTokens.lensVisibleSize
            Box(Modifier.size(CameraUiTokens.lensTouchTarget), contentAlignment = Alignment.Center) {
                Surface(onClick = { onCamera(camera) }, shape = CircleShape, color = if (selected) Color.White else Color.Black.copy(alpha = 0.42f), modifier = Modifier.size(size)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            camera.pocoLensLabel(cameras),
                            color = if (selected) Color.Black else Color.White,
                            fontSize = if (selected) 19.sp else 17.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.rotate(rotationDegrees),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PocoCompactLensSelector(cameras: List<CameraCapability>, activeCameraId: String?, onCamera: (CameraCapability) -> Unit, rotationDegrees: Float) {
    val activeIndex = cameras.indexOfFirst { it.cameraId == activeCameraId }.coerceAtLeast(0)
    val active = cameras.getOrNull(activeIndex) ?: return
    Surface(onClick = { onCamera(cameras[(activeIndex + 1) % cameras.size]) }, shape = CircleShape, color = Color.White, modifier = Modifier.size(CameraUiTokens.lensVisibleSize)) {
        Box(contentAlignment = Alignment.Center) {
            Text(active.pocoLensLabel(cameras), color = Color.Black, maxLines = 1, softWrap = false, modifier = Modifier.rotate(rotationDegrees))
        }
    }
}

@Composable
private fun PocoQuickZoomRow(zoom: Float, minZoom: Float, maxZoom: Float, onZoom: (Float) -> Unit, onToggleSlider: () -> Unit, rotationDegrees: Float) {
    val values = buildList {
        if (minZoom < 0.95f) add(minZoom)
        add(1f.coerceIn(minZoom, maxZoom))
        if (maxZoom >= 2f) add(2f)
        if (maxZoom >= 5f) add(5f)
        if (none { abs(it - zoom) < 0.08f }) add(zoom)
    }.distinctBy { (it * 10).toInt() }
    Row(horizontalArrangement = Arrangement.spacedBy(CameraUiTokens.lensGap), verticalAlignment = Alignment.CenterVertically) {
        values.forEach { value ->
            val selected = abs(zoom - value) < 0.08f
            PocoZoomButton(value, selected, rotationDegrees) { if (selected) onToggleSlider() else onZoom(value) }
        }
    }
}

@Composable
private fun PocoZoomButton(value: Float, selected: Boolean, rotationDegrees: Float, onClick: () -> Unit) {
    val size = if (selected) CameraUiTokens.lensActiveVisibleSize else CameraUiTokens.lensVisibleSize
    Box(Modifier.size(CameraUiTokens.lensTouchTarget), contentAlignment = Alignment.Center) {
        Surface(onClick = onClick, shape = CircleShape, color = if (selected) Color.White else Color.Black.copy(alpha = 0.42f), modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Text(pocoFormatZoom(value), color = if (selected) Color.Black else Color.White, fontSize = if (selected) 19.sp else 17.sp, modifier = Modifier.rotate(rotationDegrees))
        }
        }
    }
}

@Composable
private fun PocoCameraSwitchButton(enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(CameraUiTokens.secondaryTouchTarget), contentAlignment = Alignment.Center) {
        Surface(onClick = onClick, enabled = enabled, shape = CircleShape, color = Color.White.copy(alpha = if (enabled) 0.14f else 0.06f), modifier = Modifier.size(CameraUiTokens.secondaryControlSize)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Cameraswitch, "Switch camera", tint = Color.White, modifier = Modifier.size(29.dp)) }
        }
    }
}

@Composable
private fun PocoLatestMediaButton(bitmap: Bitmap?, hasMedia: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(CameraUiTokens.secondaryTouchTarget), contentAlignment = Alignment.Center) {
        Surface(onClick = onClick, enabled = hasMedia, shape = CircleShape, color = Color.White.copy(alpha = 0.14f), modifier = Modifier.size(CameraUiTokens.secondaryControlSize).border(1.dp, Color.White.copy(alpha = 0.62f), CircleShape)) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), "Latest captured media", Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Collections, "Open gallery", tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}

@Composable
private fun PocoShutterButton(videoMode: Boolean, recording: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(CameraUiTokens.shutterTouchTarget), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(CameraUiTokens.shutterOuterSize).clickable(onClick = onClick).semantics { contentDescription = if (recording) "Stop recording" else if (videoMode) "Start video recording" else "Take photo" }) {
        drawCircle(Color.White, radius = size.minDimension / 2f, style = Stroke(width = CameraUiTokens.shutterStroke.toPx()))
        when {
            recording -> drawRoundRect(Color(0xFFFF3B30), topLeft = Offset(size.width * 0.34f, size.height * 0.34f), size = Size(size.width * 0.32f, size.height * 0.32f), cornerRadius = CornerRadius(5.dp.toPx()))
            videoMode -> drawCircle(Color(0xFFFF3B30), radius = size.minDimension * 0.36f)
            else -> drawCircle(Color.White, radius = size.minDimension * 0.38f)
        }
        }
    }
}

private fun CameraCapability.pocoLensLabel(cameras: List<CameraCapability>): String {
    if (lensRole == LensRole.Ultrawide) return "0.6"
    if (lensRole == LensRole.Telephoto) return "2"
    if (lensRole == LensRole.Main || lensRole == LensRole.Wide) return "1x"
    val sameFacing = cameras.filter { it.lensFacing == lensFacing }
    val index = sameFacing.indexOfFirst { it.cameraId == cameraId }.coerceAtLeast(0) + 1
    return if (sameFacing.size == 1) "1x" else index.toString()
}

private fun pocoFormatZoom(value: Float): String = if (abs(value - value.toInt()) < 0.04f) "${value.toInt()}x" else "%.1fx".format(value)
