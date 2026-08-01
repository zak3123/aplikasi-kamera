@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fatih.adaptivecompositioncamera.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fatih.adaptivecompositioncamera.domain.model.AppSettings

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onCameraInformation: () -> Unit,
    onDiagnostics: () -> Unit,
    onMirrorPreview: (Boolean) -> Unit,
    onSaveMirrored: (Boolean) -> Unit,
    onScreenFlash: (Boolean) -> Unit,
    onAudio: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onVolumeShutter: (Boolean) -> Unit,
    onMatchPreviewCrop: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to camera") } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item { SectionTitle("Selfie") }
            item { SettingSwitch("Mirror front-camera preview", settings.mirrorFrontPreview, onMirrorPreview) }
            item { SettingSwitch("Save mirrored selfie", settings.saveMirroredSelfie, onSaveMirrored) }
            item { SettingSwitch("Screen flash", settings.screenFlash, onScreenFlash) }
            item { SectionTitle("Capture") }
            item {
                SettingSwitch(
                    "Match saved photo to preview crop",
                    settings.matchPreviewCrop,
                    onMatchPreviewCrop,
                    "When disabled, the native sensor output is saved without the preview crop.",
                )
            }
            item { SettingSwitch("Record video audio", settings.audioEnabled, onAudio) }
            item { SettingSwitch("Camera haptics", settings.haptics, onHaptics) }
            item { SettingSwitch("Volume-button shutter", settings.volumeShutter, onVolumeShutter) }
            item {
                Text(
                    "Composition guides stay in the live preview and are not written into normal photos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
                )
            }
            item { SectionTitle("Support") }
            item { NavigationRow(Icons.Rounded.CameraAlt, "Camera information", "Android-exposed lenses, formats and resolutions", onCameraInformation) }
            item { NavigationRow(Icons.Rounded.BugReport, "Diagnostics", "Current capability report and camera warnings", onDiagnostics) }
            item {
                Text(
                    "Media is stored locally in DCIM/AdaptiveCompositionCamera. This app does not request internet or broad storage access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingSwitch(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(horizontal = 20.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, label: String, supporting: String, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
        HorizontalDivider(Modifier.padding(start = 56.dp))
    }
}
