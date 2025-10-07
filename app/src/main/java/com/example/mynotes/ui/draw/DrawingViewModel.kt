package com.example.mynotes.ui.draw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color.WHITE
import android.graphics.Paint
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.mynotes.data.PathData
import com.example.mynotes.data.drawing.DrawingEntity
import com.example.mynotes.data.drawing.DrawingsRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrawingViewModel(private val drawingsRepository: DrawingsRepository) : ViewModel() {
    // Current drawing state
    private val _drawing = MutableStateFlow(Drawing())
    val drawing: StateFlow<Drawing> = _drawing
    // Current stroke being drawn
    private val _currentStroke = MutableStateFlow<Stroke?>(null)

    private val _backgroundBitmap = MutableStateFlow<Bitmap?>(null)
    val backgroundBitmap: StateFlow<Bitmap?> = _backgroundBitmap

    // Current drawing settings
    private val _strokeColor = MutableStateFlow(Color.Black)
    val strokeColor: StateFlow<Color> = _strokeColor

    private val _strokeWidth = MutableStateFlow(5f)
    val strokeWidth: StateFlow<Float> = _strokeWidth

    val gson = Gson()

    fun setStrokes(strokes: List<Stroke>) {
        _drawing.value = _drawing.value.copy(strokes = strokes)
    }
    // Rest of your existing drawing methods...

    fun startStroke(x: Float, y: Float) {
        val color = if (_isErasing.value) {
            _drawing.value.backgroundColor
        } else {
            _strokeColor.value
        }

        _currentStroke.value = Stroke(
            points = listOf(DrawPoint(x, y)),
            color = color,
            strokeWidth = _strokeWidth.value
        )
    }

    fun addPointToStroke(x: Float, y: Float, pressure: Float = 1f) {
        _currentStroke.value?.let { currentStroke ->
            val updatedPoints = currentStroke.points + DrawPoint(x, y, pressure)
            _currentStroke.value = currentStroke.copy(points = updatedPoints)
        }
    }

    fun endStroke() {
        _currentStroke.value?.let { stroke ->
            if (stroke.points.size > 1) {
                _drawing.value = _drawing.value.copy(
                    strokes = _drawing.value.strokes + stroke
                )
            }
            _currentStroke.value = null
        }
    }
    // Change drawing settings
    fun setStrokeColor(color: Color) {
        _strokeColor.value = color
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
    }

    // Clear the drawing
    fun clearDrawing() {
        _drawing.value = Drawing()
    }
    fun saveCanvasAsBitmap(
        context: Context,
        canvasWidth: Int,
        canvasHeight: Int,
        originalDrawingId: Long? = null,
        onSaved: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resultBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(resultBitmap)

                backgroundBitmap.value?.let {
                    canvas.drawBitmap(it, 0f, 0f, null)
                } ?: canvas.drawColor(WHITE)

                drawing.value.strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val paint = Paint().apply {
                            color = stroke.color.toArgb()
                            strokeWidth = stroke.strokeWidth
                            style = Paint.Style.STROKE
                            isAntiAlias = true
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }

                        val path = android.graphics.Path().apply {
                            moveTo(stroke.points.first().x, stroke.points.first().y)
                            for (i in 1 until stroke.points.size) {
                                lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                        }

                        canvas.drawPath(path, paint)
                    }
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "drawing_$timestamp.png"

                val file = withContext(Dispatchers.IO) {
                    val storageDir = File(context.filesDir, "drawings")
                    if (!storageDir.exists()) storageDir.mkdirs()
                    File(storageDir, fileName)
                }

                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { out ->
                        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                val entity = DrawingEntity(
                    title = "Drawing $timestamp",
                    filePath = file.absolutePath,
                    createdAt = System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    drawingsRepository.insertDrawing(entity)
                }

                // ✅ Xóa bản cũ nếu có
                if (originalDrawingId != null) {
                    withContext(Dispatchers.IO) {
                        val old = drawingsRepository.getDrawingById(originalDrawingId)
                        old?.let {
                            File(it.filePath).delete()
                            drawingsRepository.deleteDrawing(it.id.toLong())
                        }
                    }
                }

                onSaved(file.absolutePath)

            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun loadDrawing(drawingId: Long) {
        viewModelScope.launch {
            val drawing = drawingsRepository.getDrawingById(drawingId)
            if (drawing != null) {
                val file = File(drawing.filePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(drawing.filePath)
                    _backgroundBitmap.value = bitmap
                }
            }
        }
    }
    // Status for saving operation
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus

    sealed class SaveStatus {
        object Idle : SaveStatus()
        object Saving : SaveStatus()
        object Success : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }

    // Drawing paths
    private val _paths = MutableStateFlow<List<PathData>>(emptyList())
    val paths: StateFlow<List<PathData>> = _paths

    // Stroke color and width settings
    private var _isErasing = MutableStateFlow(false)
    val isErasing: StateFlow<Boolean> = _isErasing


    fun toggleEraserMode() {
        _isErasing.value = !_isErasing.value
    }

    fun setBackgroundBitmap(bitmap: Bitmap?) {
        _backgroundBitmap.value = bitmap
    }

    @RequiresApi(35)
    fun undoLastStroke() {
        val strokes = _drawing.value.strokes.toMutableList()
        if (strokes.isNotEmpty()) {
            strokes.removeLast()
            _drawing.value = _drawing.value.copy(strokes = strokes)
        }
    }
}
fun List<Stroke>.toPathDataList(): List<PathData> {
    return this.map { stroke ->
        val path = Path().apply {
            if (stroke.points.isNotEmpty()) {
                moveTo(stroke.points.first().x, stroke.points.first().y)
                stroke.points.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
            }
        }
        PathData(path, stroke.color, stroke.strokeWidth)
    }
}
