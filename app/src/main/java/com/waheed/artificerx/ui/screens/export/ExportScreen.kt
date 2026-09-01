package com.waheed.artificerx.ui.screens.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GlassCardShape
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.QualityFail
import com.waheed.artificerx.ui.theme.QualityPass
import com.waheed.artificerx.ui.theme.glassSurface

@Composable
fun ExportScreen(
    projectId: String,
    studioViewModel: StudioViewModel,
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val compositedBitmap by studioViewModel.compositedBitmap.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) {
        viewModel.loadProjectName(projectId)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ArtificerXGradients.backgroundWash),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.large)
                            .glassSurface(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (compositedBitmap != null) {
                        Image(
                            bitmap = compositedBitmap!!.asImageBitmap(),
                            contentDescription = "Artwork preview",
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text("No artwork to export yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (state.exportState) {
                    ExportState.IDLE -> {
                        Button(
                            onClick = { compositedBitmap?.let { viewModel.exportBitmap(it) } },
                            enabled = compositedBitmap != null,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = GlassCardShape,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = GoldPrimary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text("Save to Gallery (PNG)", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    ExportState.EXPORTING -> {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                    ExportState.SUCCESS -> {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = QualityPass,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Text(state.resultMessage.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) {
                            Text("Export Again")
                        }
                    }
                    ExportState.FAILED -> {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = QualityFail, modifier = Modifier.padding(bottom = 8.dp))
                        Text(state.resultMessage.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}
