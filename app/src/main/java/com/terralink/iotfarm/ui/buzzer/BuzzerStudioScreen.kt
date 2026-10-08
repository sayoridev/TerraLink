package com.terralink.iotfarm.ui.buzzer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.terralink.iotfarm.ui.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuzzerStudioScreen(
    viewModel: DashboardViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isBuzzerPlaying.collectAsState()
    val currentIndex by viewModel.currentNoteIndex.collectAsState()
    val totalNotes by viewModel.totalNotes.collectAsState()
    val melodyName by viewModel.selectedMelodyName.collectAsState()

    val mp3PickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, uri) ?: "Custom_Audio.mp3"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
            viewModel.loadAndConvertCustomMp3(fileName, bytes)
        }
    }

    // Preset melody sample based on Arduino tone example provided by user
    val classicMelodyNotes = intArrayOf(
        330, 330, 330, 262, 330, 392, 196,
        262, 196, 165, 220, 247, 233, 220, 196, 330, 392, 440, 349, 392, 330, 262, 294, 247
    )
    val classicMelodyDurations = intArrayOf(
        8, 4, 4, 8, 4, 2, 2,
        3, 3, 3, 4, 4, 8, 4, 8, 8, 8, 4, 8, 4, 3, 8, 8, 3
    )

    val scaleNotes = intArrayOf(262, 294, 330, 349, 392, 440, 494, 523)
    val scaleDurations = intArrayOf(4, 4, 4, 4, 4, 4, 4, 4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎵 Buzzer Studio (ESP32 PWM)") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "Current Melody:", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = melodyName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Status: ${if (isPlaying) "▶ Playing" else "⏸ Paused / Stopped"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPlaying) Color(0xFF2E7D32) else Color.Gray
                    )
                }
            }

            // MP3 Picker Button
            Button(
                onClick = { mp3PickerLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("📁 Select local .mp3 file to convert")
            }

            // Progress & Seeking
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Melody Progress", style = MaterialTheme.typography.titleSmall)
                    
                    val progress = if (totalNotes > 0) currentIndex.toFloat() / totalNotes.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Note: $currentIndex / $totalNotes", style = MaterialTheme.typography.bodySmall)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    }

                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = { viewModel.seekBuzzer(it.toInt()) },
                        valueRange = 0f..(if (totalNotes > 0) totalNotes.toFloat() else 1f),
                        steps = if (totalNotes > 1) totalNotes - 1 else 0
                    )
                }
            }

            // Playback Controls
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { viewModel.playPresetMelody("Classic (Super Mario Theme)", classicMelodyNotes, classicMelodyDurations) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Classic")
                    }

                    FilledTonalIconButton(
                        onClick = { viewModel.pauseBuzzer() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Pause / Restart")
                    }

                    IconButton(
                        onClick = { viewModel.stopBuzzer() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Stop", tint = Color.Red)
                    }
                }
            }

            // Preset Melodies Selection
            Text(text = "Melodies & Scales Library", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = { viewModel.playPresetMelody("Classic (Super Mario Theme)", classicMelodyNotes, classicMelodyDurations) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🎵 Play Classic Melody (24 Notes)")
            }

            Button(
                onClick = { viewModel.playPresetMelody("Chromatic Scale (C-D-E)", scaleNotes, scaleDurations) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("🎶 Play Chromatic Scale (8 Notes)")
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}
