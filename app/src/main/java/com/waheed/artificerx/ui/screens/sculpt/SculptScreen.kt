import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.domain.model.PrimitiveType
import com.waheed.artificerx.domain.model.SculptBrushType
import com.waheed.artificerx.domain.model.SculptMesh
import com.waheed.artificerx.ui.theme.GoldPrimary

private data class SculptProjectedVertex(
    val point: Offset,
    val depth: Float,
)

/**
 * Full 3D sculpting studio surface — the 3D counterpart to
 * StudioScreen. Structure: top bar (back, agent-activity indicator),
 * full-screen mesh viewport with deterministic CPU geometry preview and
 * orbit interaction,
 * bottom brush toolbar (radius/strength sliders + 6 brush types),
 * floating primitive-add button and mesh list.
 */
@Composable
fun SculptScreen(
    onBack: () -> Unit,
    viewModel: SculptViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPrimitivePicker by remember { mutableStateOf(false) }
    var showMeshList by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Sculpt Studio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val meshId = uiState.activeMeshId ?: return@IconButton
                    val mesh = uiState.meshes[meshId] ?: return@IconButton
                    val centerVertex =
                        mesh.vertices.minByOrNull { it.length() }
                            ?: mesh.vertices.firstOrNull()
                            ?: return@IconButton
                    viewModel.applyManualStroke(centerVertex)
                },
                enabled = uiState.activeMeshId != null,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Apply center sculpt stroke",
                    tint = if (uiState.activeMeshId != null) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showMeshList = !showMeshList }) {
                Icon(Icons.Filled.Settings, contentDescription = "Meshes", tint = GoldPrimary)
            }
            IconButton(onClick = { showPrimitivePicker = !showPrimitivePicker }) {
                Icon(Icons.Filled.Add, contentDescription = "Add primitive", tint = GoldPrimary)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            SculptViewportPreview(
                activeMesh = uiState.activeMeshId?.let { uiState.meshes[it] },
                meshCount = uiState.meshes.size,
            )

            if (showPrimitivePicker) {
                PrimitivePickerOverlay(
                    onSelect = { type ->
                        viewModel.addPrimitive(type)
                        showPrimitivePicker = false
                    },
                    onDismiss = { showPrimitivePicker = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                )
            }

            if (showMeshList) {
                MeshListOverlay(
                    meshIds = uiState.meshes.keys.toList(),
                    meshNames = uiState.meshes.mapValues { it.value.name },
                    activeMeshId = uiState.activeMeshId,
                    onSelect = viewModel::setActiveMesh,
                    onDelete = viewModel::deleteMesh,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                )
            }
        }

        BrushToolbar(
            activeBrush = uiState.activeBrush,
            radius = uiState.brushRadius,
            strength = uiState.brushStrength,
            onBrushSelected = viewModel::selectBrush,
            onRadiusChanged = viewModel::setBrushRadius,
            onStrengthChanged = viewModel::setBrushStrength,
        )
    }
}

@Composable
private fun SculptViewportPreview(
    activeMesh: SculptMesh?,
    meshCount: Int,
) {
    var yawDegrees by remember { mutableStateOf(-28f) }
    var pitchDegrees by remember { mutableStateOf(18f) }
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val meshColor = remember(activeMesh?.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(activeMesh?.colorHex ?: "#CCCCCC")) }
            .getOrDefault(Color.LightGray)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(surface, MaterialTheme.shapes.large)
                .pointerInput(activeMesh?.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yawDegrees = (yawDegrees + dragAmount.x * 0.45f) % 360f
                        pitchDegrees = (pitchDegrees - dragAmount.y * 0.35f).coerceIn(-78f, 78f)
                    }
                },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(surface)
            val mesh = activeMesh
            if (mesh != null && mesh.vertices.isNotEmpty()) {
                val yaw = Math.toRadians(yawDegrees.toDouble()).toFloat()
                val pitch = Math.toRadians(pitchDegrees.toDouble()).toFloat()
                val cosY = kotlin.math.cos(yaw)
                val sinY = kotlin.math.sin(yaw)
                val cosP = kotlin.math.cos(pitch)
                val sinP = kotlin.math.sin(pitch)

                val span = maxOf(
                    mesh.vertices.maxOf { it.x } - mesh.vertices.minOf { it.x },
                    mesh.vertices.maxOf { it.y } - mesh.vertices.minOf { it.y },
                    mesh.vertices.maxOf { it.z } - mesh.vertices.minOf { it.z },
                    0.001f,
                )
                val scale = minOf(size.width, size.height) * 0.68f / span
                val cx = size.width / 2f
                val cy = size.height / 2f

                val projected = mesh.vertices.map { vertex ->
                    val x1 = vertex.x * cosY - vertex.z * sinY
                    val z1 = vertex.x * sinY + vertex.z * cosY
                    val y1 = vertex.y * cosP - z1 * sinP
                    val z2 = vertex.y * sinP + z1 * cosP
                    val perspective = 1f / (1f + (z2 * 0.12f).coerceIn(-0.65f, 0.65f))
                    SculptProjectedVertex(
                        point = Offset(cx + x1 * scale * perspective, cy - y1 * scale * perspective),
                        depth = z2,
                    )
                }

                val triangles = mesh.triangleIndices
                    .chunked(3)
                    .mapNotNull { tri ->
                        if (tri.size != 3 || tri.any { it !in projected.indices }) null
                        else Triple(projected[tri[0]], projected[tri[1]], projected[tri[2]])
                    }
                    .sortedByDescending { (a, b, c) -> (a.depth + b.depth + c.depth) / 3f }

                // Grounded axis helper makes orientation obvious after orbiting.
                drawLine(onSurface.copy(alpha = 0.25f), Offset(24f, size.height - 26f), Offset(72f, size.height - 26f), strokeWidth = 2f)
                drawLine(onSurface.copy(alpha = 0.25f), Offset(24f, size.height - 26f), Offset(24f, size.height - 74f), strokeWidth = 2f)

                triangles.forEach { (a, b, c) ->
                    val depth = (((a.depth + b.depth + c.depth) / 3f) + span) / (2f * span)
                    val faceAlpha = (0.10f + depth.coerceIn(0f, 1f) * 0.20f)
                    val path = Path().apply {
                        moveTo(a.point.x, a.point.y)
                        lineTo(b.point.x, b.point.y)
                        lineTo(c.point.x, c.point.y)
                        close()
                    }
                    drawPath(path, color = meshColor.copy(alpha = faceAlpha))
                    drawLine(meshColor.copy(alpha = 0.82f), a.point, b.point, strokeWidth = 1.4f)
                    drawLine(meshColor.copy(alpha = 0.82f), b.point, c.point, strokeWidth = 1.4f)
                    drawLine(meshColor.copy(alpha = 0.82f), c.point, a.point, strokeWidth = 1.4f)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = activeMesh?.name ?: "Sculpt viewport",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (activeMesh != null) "Drag to orbit • ${activeMesh.vertexCount} vertices • ${activeMesh.triangleCount} triangles" else "$meshCount mesh(es) in scene",
                style = MaterialTheme.typography.labelSmall,
                color = onSurface,
            )
        }

        if (activeMesh == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Circle, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                Text("Add a primitive to begin sculpting", style = MaterialTheme.typography.bodyMedium, color = onSurface)
            }
        }
    }
}
