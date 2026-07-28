package com.fatih.adaptivecompositioncamera.ui.camera

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.hardware.camera2.CameraMetadata
import com.fatih.adaptivecompositioncamera.domain.model.CameraCapability
import com.fatih.adaptivecompositioncamera.domain.model.CameraMode
import com.fatih.adaptivecompositioncamera.domain.model.CameraResolution
import com.fatih.adaptivecompositioncamera.domain.model.FlashMode
import com.fatih.adaptivecompositioncamera.domain.model.LensRole
import com.fatih.adaptivecompositioncamera.domain.model.RuntimeCameraInfo
import com.fatih.adaptivecompositioncamera.domain.model.VideoStabilizationMode
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

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
    PocoTopControl(
        icon = Icons.Rounded.CameraAlt,
        description = description,
        label = value.takeUnless { it == "OFF" || it == "N/A" },
        onClick = onClick,
        active = active,
        rotationDegrees = rotationDegrees,
    )
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
            color = if (selected) PocoAccent else Color.White.copy(alpha = 0.78f),
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
        if (maxZoom >= 5f && minZoom < 0.95f) add(5f)
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

@Composable
fun PocoExposureControl(
    focusPoint: Offset,
    exposureIndex: Int,
    minExposure: Int,
    maxExposure: Int,
    exposureStep: Float,
    onExposure: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (maxExposure <= minExposure) return
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val controlWidthPx = with(density) { CameraUiTokens.exposureControlWidth.toPx() }.roundToInt()
        val controlHeightPx = with(density) { CameraUiTokens.exposureSliderHeight.toPx() }.roundToInt()
        val ringRadiusPx = with(density) { (CameraUiTokens.focusRingDiameter / 2).toPx() }.roundToInt()
        val gapPx = with(density) { 18.dp.toPx() }.roundToInt()
        val viewportWidth = with(density) { maxWidth.toPx() }.roundToInt()
        val viewportHeight = with(density) { maxHeight.toPx() }.roundToInt()
        val geometry = exposureControlGeometry(
            focusPoint = focusPoint,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            controlWidthPx = controlWidthPx,
            controlHeightPx = controlHeightPx,
            ringRadiusPx = ringRadiusPx,
            gapPx = gapPx,
        )
        val range = (maxExposure - minExposure).coerceAtLeast(1)
        val normalized = ((exposureIndex.coerceIn(minExposure, maxExposure) - minExposure).toFloat() / range)
            .coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .offset { IntOffset(geometry.x, geometry.y) }
                .size(CameraUiTokens.exposureControlWidth, CameraUiTokens.exposureSliderHeight)
                .pointerInput(minExposure, maxExposure) {
                    detectDragGestures(
                        onDragStart = { position ->
                            val mapped = maxExposure - ((position.y / controlHeightPx) * range).roundToInt()
                            onExposure(mapped.coerceIn(minExposure, maxExposure))
                        },
                    ) { change, _ ->
                        change.consume()
                        val mapped = maxExposure - ((change.position.y / controlHeightPx) * range).roundToInt()
                        onExposure(mapped.coerceIn(minExposure, maxExposure))
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize().clickable(onClick = onReset)) {
                val centerX = size.width / 2f
                val top = 30.dp.toPx()
                val bottom = size.height - 34.dp.toPx()
                val thumbY = bottom - (bottom - top) * normalized
                drawLine(
                    Color.Black.copy(alpha = 0.52f),
                    Offset(centerX + 1.5.dp.toPx(), top),
                    Offset(centerX + 1.5.dp.toPx(), bottom),
                    5.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    Color.White.copy(alpha = 0.72f),
                    Offset(centerX, top),
                    Offset(centerX, bottom),
                    2.2.dp.toPx(),
                    StrokeCap.Round,
                )
                drawCircle(Color.Black.copy(alpha = 0.58f), CameraUiTokens.exposureThumbSize.toPx() / 2f + 3.dp.toPx(), Offset(centerX, thumbY))
                drawCircle(PocoAccent, CameraUiTokens.exposureThumbSize.toPx() / 2f, Offset(centerX, thumbY))
                val sunCenter = Offset(centerX, 16.dp.toPx())
                drawCircle(Color.White, 6.dp.toPx(), sunCenter, style = Stroke(width = 1.8.dp.toPx()))
                repeat(8) { index ->
                    val angle = Math.toRadians((index * 45).toDouble())
                    drawLine(
                        Color.White,
                        Offset(
                            sunCenter.x + kotlin.math.cos(angle).toFloat() * 10.dp.toPx(),
                            sunCenter.y + kotlin.math.sin(angle).toFloat() * 10.dp.toPx(),
                        ),
                        Offset(
                            sunCenter.x + kotlin.math.cos(angle).toFloat() * 14.dp.toPx(),
                            sunCenter.y + kotlin.math.sin(angle).toFloat() * 14.dp.toPx(),
                        ),
                        1.6.dp.toPx(),
                        StrokeCap.Round,
                    )
                }
            }
            Text(
                exposureEvLabel(exposureIndex, exposureStep),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.48f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
internal fun PocoStyleProControls(
    runtimeInfo: RuntimeCameraInfo,
    activeControl: ProControl,
    detailsVisible: Boolean,
    iso: Int,
    exposureNanos: Long,
    focusDistance: Float,
    exposureCompensation: Int,
    manualExposure: Boolean,
    manualFocus: Boolean,
    whiteBalanceMode: Int,
    onControl: (ProControl) -> Unit,
    onIso: (Int) -> Unit,
    onExposure: (Long) -> Unit,
    onFocus: (Float) -> Unit,
    onExposureCompensation: (Int) -> Unit,
    onWhiteBalance: (Int) -> Unit,
    onAuto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = Color.Black.copy(alpha = 0.42f), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("PRO", color = PocoAccent, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProControl.entries.forEach { control ->
                        val available = when (control) {
                            ProControl.Iso, ProControl.Shutter -> runtimeInfo.supportsManualSensor
                            ProControl.WhiteBalance -> runtimeInfo.availableWhiteBalanceModes.size > 1
                            ProControl.Focus -> runtimeInfo.minFocusDistance > 0f
                            ProControl.Exposure -> runtimeInfo.exposureMin != runtimeInfo.exposureMax
                        }
                        if (available) {
                            Column(
                                Modifier
                                    .widthIn(min = 54.dp)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeControl == control) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable { onControl(control) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(control.shortLabel(), color = Color.White.copy(alpha = 0.64f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                Text(
                                    control.valueLabel(iso, exposureNanos, focusDistance, exposureCompensation, manualExposure, manualFocus, whiteBalanceMode),
                                    color = if (activeControl == control) PocoAccent else Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
                Surface(
                    onClick = onAuto,
                    color = if (!manualExposure && !manualFocus && exposureCompensation == 0 && whiteBalanceMode == CameraMetadata.CONTROL_AWB_MODE_AUTO) {
                        PocoAccent
                    } else {
                        Color.White.copy(alpha = 0.16f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text("AUTO", color = if (!manualExposure && !manualFocus && exposureCompensation == 0 && whiteBalanceMode == CameraMetadata.CONTROL_AWB_MODE_AUTO) Color.Black else Color.White)
                    }
                }
            }
            AnimatedVisibility(detailsVisible, modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp)) {
            when (activeControl) {
                ProControl.Iso -> if (runtimeInfo.isoMax > runtimeInfo.isoMin) {
                    Slider(
                        value = iso.coerceIn(runtimeInfo.isoMin, runtimeInfo.isoMax).toFloat(),
                        onValueChange = { onIso(it.roundToInt()) },
                        valueRange = runtimeInfo.isoMin.toFloat()..runtimeInfo.isoMax.toFloat(),
                    )
                }
                ProControl.Shutter -> {
                    val minimum = runtimeInfo.exposureTimeMinNanos.coerceAtLeast(1L)
                    val maximum = minOf(runtimeInfo.exposureTimeMaxNanos, 250_000_000L).coerceAtLeast(minimum)
                    if (maximum > minimum) {
                        val start = ln(minimum.toDouble())
                        val span = ln(maximum.toDouble()) - start
                        val normalized = ((ln(exposureNanos.coerceIn(minimum, maximum).toDouble()) - start) / span)
                            .toFloat().coerceIn(0f, 1f)
                        Slider(
                            value = normalized,
                            onValueChange = { onExposure(exp(start + span * it).toLong()) },
                            valueRange = 0f..1f,
                        )
                    }
                }
                ProControl.WhiteBalance -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    runtimeInfo.availableWhiteBalanceModes.forEach { mode ->
                        Surface(
                            onClick = { onWhiteBalance(mode) },
                            color = if (whiteBalanceMode == mode) PocoAccent else Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                whiteBalanceLabel(mode),
                                color = if (whiteBalanceMode == mode) Color.Black else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                maxLines = 1,
                            )
                        }
                    }
                }
                ProControl.Focus -> if (runtimeInfo.minFocusDistance > 0f) {
                    Slider(value = focusDistance.coerceIn(0f, runtimeInfo.minFocusDistance), onValueChange = onFocus, valueRange = 0f..runtimeInfo.minFocusDistance)
                }
                ProControl.Exposure -> if (runtimeInfo.exposureMax > runtimeInfo.exposureMin) {
                    Slider(
                        value = exposureCompensation.coerceIn(runtimeInfo.exposureMin, runtimeInfo.exposureMax).toFloat(),
                        onValueChange = { onExposureCompensation(it.roundToInt()) },
                        valueRange = runtimeInfo.exposureMin.toFloat()..runtimeInfo.exposureMax.toFloat(),
                    )
                }
            }
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
