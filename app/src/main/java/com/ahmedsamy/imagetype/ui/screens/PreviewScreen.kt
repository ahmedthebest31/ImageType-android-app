package com.ahmedsamy.imagetype.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.ahmedsamy.imagetype.util.stringRes
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahmedsamy.imagetype.EditorViewModel
import com.ahmedsamy.imagetype.R
import com.ahmedsamy.imagetype.util.BgType
import com.ahmedsamy.imagetype.util.checkerboardBackground

@Composable
fun TabLivePreviewWorkspace(viewModel: EditorViewModel) {
    val bitmapState by viewModel.generatedBitmap.collectAsState()
    val isRendering by viewModel.isRendering.collectAsState()
    val bgType by viewModel.bgType.collectAsState()

    val renderingLabelSem = stringRes(R.string.rendering_label)
    val zoomOutSem = stringRes(R.string.zoom_out)
    val zoomInSem = stringRes(R.string.zoom_in)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = bitmapState
        if (bitmap != null) {
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val zoomLevelSem = stringRes(R.string.zoom_level, (scale * 100).toInt())
            val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 6f)
                offset += offsetChange
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .then(
                        if (bgType == BgType.Transparent) Modifier.checkerboardBackground() else Modifier.background(
                            Color.DarkGray
                        )
                    )
                    .transformable(state = transformState)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringRes(R.string.preview_description),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { scale = (scale / 1.25f).coerceIn(1f, 6f) },
                    modifier = Modifier.semantics { contentDescription = zoomOutSem }
                ) {
                    Text("-", style = MaterialTheme.typography.headlineMedium)
                }
                Text(
                    text = stringRes(R.string.zoom_level, (scale * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { contentDescription = zoomLevelSem }
                )
                TextButton(
                    onClick = { scale = (scale * 1.25f).coerceIn(1f, 6f) },
                    modifier = Modifier.semantics { contentDescription = zoomInSem }
                ) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Panorama,
                    contentDescription = stringRes(R.string.no_preview_available),
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringRes(R.string.no_preview_available),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (isRendering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics { contentDescription = renderingLabelSem },
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = stringRes(R.string.rendering_label),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
