package com.fatih.adaptivecompositioncamera.ui.media

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fatih.adaptivecompositioncamera.domain.model.MediaItem
import com.fatih.adaptivecompositioncamera.media.AndroidMediaRepository
import com.fatih.adaptivecompositioncamera.utility.CameraMath
import kotlinx.coroutines.launch

@Composable
fun MediaViewerScreen(
    item: MediaItem,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val containerSize = LocalWindowInfo.current.containerSize
    val repository = remember { AndroidMediaRepository() }
    val scope = rememberCoroutineScope()
    var bitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var videoPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoPlaying by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }

    LaunchedEffect(item.uri, item.isVideo) {
        if (!item.isVideo) {
            bitmap = repository.loadViewerBitmap(
                context,
                item.uri,
                containerSize.width.coerceAtLeast(1) * 2,
                containerSize.height.coerceAtLeast(1) * 2,
            )
        }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (item.isVideo) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    VideoView(viewContext).apply {
                        val controls = MediaController(viewContext)
                        controls.setAnchorView(this)
                        setMediaController(controls)
                        setVideoURI(item.uri)
                        setOnPreparedListener { player ->
                            videoPlayer = player
                            player.isLooping = false
                            player.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
                            start()
                            videoPlaying = true
                        }
                        setOnCompletionListener { videoPlaying = false }
                        videoView = this
                    }
                },
            )
        } else {
            val image = bitmap
            if (image == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                ZoomablePhoto(image)
            }
        }

        Surface(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding(),
            color = Color.Black.copy(alpha = 0.58f),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
                Text(item.displayName, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
                IconButton(onClick = { showInfo = true }) { Icon(Icons.Rounded.Info, "Media information", tint = Color.White) }
            }
        }

        Surface(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding(),
            color = Color.Black.copy(alpha = 0.62f),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.isVideo) {
                    ViewerAction(
                        if (videoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (videoPlaying) "Pause" else "Play",
                    ) {
                        videoView?.let { view ->
                            if (view.isPlaying) view.pause() else view.start()
                            videoPlaying = view.isPlaying
                        }
                    }
                    ViewerAction(if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, if (muted) "Unmute" else "Mute") {
                        muted = !muted
                        videoPlayer?.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
                    }
                }
                ViewerAction(Icons.Rounded.Share, "Share") { shareMedia(context, item) }
                ViewerAction(Icons.AutoMirrored.Rounded.OpenInNew, "Open externally") { openExternally(context, item) }
                ViewerAction(Icons.Rounded.Delete, "Delete") { showDelete = true }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this ${if (item.isVideo) "video" else "photo"}?") },
            text = { Text("This removes the media from this device and gallery apps.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        if (repository.delete(context, item)) onDeleted() else onMessage("Unable to delete this media.")
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Media information") },
            text = {
                Column(Modifier.widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.displayName)
                    Text("Type: ${item.mimeType}")
                    if (item.width > 0 && item.height > 0) {
                        Text("Dimensions: ${item.width} x ${item.height}")
                        Text("Actual output: ${CameraMath.megapixels(item.width, item.height)} MP")
                        Text("Aspect ratio: ${CameraMath.aspectRatioLabel(item.width, item.height)}")
                    }
                    if (item.rotationDegrees != 0) Text("EXIF rotation: ${item.rotationDegrees}°")
                    item.cameraId?.let { Text("Camera ID: $it") }
                    item.requestedResolution?.let { Text("Requested native resolution: $it") }
                    item.boundResolution?.let { Text("Bound capture resolution: $it") }
                    if (item.durationMillis > 0) Text("Duration: ${formatDuration(item.durationMillis)}")
                    if (item.sizeBytes > 0) Text("Size: ${formatBytes(item.sizeBytes)}")
                    Text("Location: DCIM/AdaptiveCompositionCamera")
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun ZoomablePhoto(bitmap: Bitmap) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Captured photo",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(bitmap) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(1f, 6f)
                    scale = next
                    offset = if (next == 1f) Offset.Zero else offset + pan
                }
            }
            .pointerInput(bitmap) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1f) 1f else 2.5f
                    if (scale == 1f) offset = Offset.Zero
                })
            },
    )
}

@Composable
private fun ViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = description, tint = Color.White) }
}

private fun shareMedia(context: android.content.Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share media"))
}

private fun openExternally(context: android.content.Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(item.uri, item.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1_000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
