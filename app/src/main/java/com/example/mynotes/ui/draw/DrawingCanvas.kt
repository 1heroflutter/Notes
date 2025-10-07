package com.example.mynotes.ui.draw

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    viewModel: DrawingViewModel,
    onCapture: (ImageBitmap) -> Unit
) {
    val drawing by viewModel.drawing.collectAsState()
    val paths by viewModel.paths.collectAsState()
    val backgroundBitmap by viewModel.backgroundBitmap.collectAsState()

    var currentStrokePath by remember { mutableStateOf(Path()) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val captureModifier = Modifier.drawWithContent {
        val imageBitmap = ImageBitmap(size.width.toInt(), size.height.toInt(), ImageBitmapConfig.Argb8888)
        val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)

        with(canvas) {
            drawRect(Color.White, size = size)
            withTransform({
                scale(scale)
                translate(offset.x / scale, offset.y / scale)
            }) {
                backgroundBitmap?.let { bitmap ->
                    drawImage(bitmap.asImageBitmap())
                }
                val combinedPaths = drawing.strokes.toPathDataList() + paths
                combinedPaths.forEach { pathData ->
                    drawPath(
                        path = pathData.path,
                        color = pathData.color,
                        style = Stroke(
                            width = pathData.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
        onCapture(imageBitmap)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(captureModifier)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offset += pan
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { rawOffset ->
                        val local = (rawOffset - offset) / scale
                        currentStrokePath = Path().apply { moveTo(local.x, local.y) }
                        viewModel.startStroke(local.x, local.y)
                    },
                    onDrag = { change, _ ->
                        val local = (change.position - offset) / scale
                        currentStrokePath.lineTo(local.x, local.y)
                        viewModel.addPointToStroke(local.x, local.y)
                    },
                    onDragEnd = {
                        viewModel.endStroke()
                        currentStrokePath = Path()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.White, size = size)
            scale(scale, pivot = Offset.Zero) {
                translate(offset.x / scale, offset.y / scale) {
                    backgroundBitmap?.let { bitmap ->
                        drawImage(bitmap.asImageBitmap())
                    }
                    val combinedPaths = drawing.strokes.toPathDataList() + paths
                    combinedPaths.forEach { pathData ->
                        drawPath(
                            path = pathData.path,
                            color = pathData.color,
                            style = Stroke(
                                width = pathData.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

