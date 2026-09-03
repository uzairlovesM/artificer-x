package com.waheed.artificerx.ui.screens.canvas

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.asImageBitmap
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.theme.GoldPrimary
import java.io.File

/**
 * v0.4.30: real timelapse playback (see TimelapseRecorder's doc for the
 * capture side). Frames are read lazily as android.graphics.Bitmap and
 * converted to ImageBitmap per-frame rather than pre-decoding the whole
 * sequence up front — a long session can have hundreds of frames, and
 * holding them all decoded in memory simultaneously would be wasteful
 * when only one is ever on screen at a time.
 */
@Composable
fun TimelapseScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit,
) {
    var frames by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var frameIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        frames = viewModel.listTimelapseFrames()
        isLoading = false
    }

    LaunchedEffect(isPlaying, frames) {
        if (!isPlaying || frames.isEmpty()) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(FRAME_DELAY_MS)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
            }
            Text("Timelapse", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = GoldPrimary)
                frames.isEmpty() ->
                    Text(
                        "No timelapse recorded yet for this project — frames are captured automatically " +
                            "as you draw. Come back after your next drawing session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                else -> {
                    val currentFrame = frames.getOrNull(frameIndex)
                    val bitmap =
                        remember(currentFrame) {
                            currentFrame?.let { BitmapFactory.decodeFile(it.absolutePath) }
                        }
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Timelapse frame")
                    }
                }
            }
        }

        if (frames.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = GoldPrimary,
                    )
                }
                Slider(
                    value = frameIndex.toFloat(),
                    onValueChange = {
                        isPlaying = false
                        frameIndex = it.toInt().coerceIn(0, frames.size - 1)
                    },
                    valueRange = 0f..(frames.size - 1).coerceAtLeast(1).toFloat(),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                )
            }
            Text(
                "Frame ${frameIndex + 1} / ${frames.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp).align(Alignment.CenterHorizontally),
            )
        }
        Spacer(modifier = Modifier.padding(bottom = 4.dp))
    }
}

private const val FRAME_DELAY_MS = 120L
