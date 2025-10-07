package com.example.mynotes.ui.fileReader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotes.data.drawing.DrawingEntity
import com.example.mynotes.data.drawing.DrawingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileReaderViewModel(private val drawingsRepository: DrawingsRepository) : ViewModel() {

    // Status for loading operation
    private val _loadStatus = MutableStateFlow<LoadStatus>(LoadStatus.Idle)
    val loadStatus: StateFlow<LoadStatus> = _loadStatus

    // Selected drawing
    private val _selectedDrawing = MutableStateFlow<DrawingEntity?>(null)
    val selectedDrawing: StateFlow<DrawingEntity?> = _selectedDrawing

    // Loaded bitmap
    private val _loadedBitmap = MutableStateFlow<Bitmap?>(null)
    val loadedBitmap: StateFlow<Bitmap?> = _loadedBitmap

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _isInSelectionMode = MutableStateFlow(false)
    val isInSelectionMode: StateFlow<Boolean> = _isInSelectionMode

    sealed class LoadStatus {
        data object Idle : LoadStatus()
        data object Loading : LoadStatus()
        data object Success : LoadStatus()
        data class Error(val message: String) : LoadStatus()
    }

    // Get all saved drawings
    fun getAllDrawings(): Flow<List<DrawingEntity>> {
        return drawingsRepository.getAllDrawings()
    }

    // Load drawing from file
    fun loadDrawingFromFile(filePath: String) {
        viewModelScope.launch {
            _loadStatus.value = LoadStatus.Loading

            try {
                withContext(Dispatchers.IO) {
                    // Check if file exists
                    val file = File(filePath)
                    if (!file.exists()) {
                        _loadStatus.value = LoadStatus.Error("File not found")
                        return@withContext
                    }

                    // Load bitmap from file
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    if (bitmap == null) {
                        _loadStatus.value = LoadStatus.Error("Failed to decode image")
                        return@withContext
                    }

                    // Store the loaded bitmap
                    _loadedBitmap.value = bitmap

                    _loadStatus.value = LoadStatus.Success
                }
            } catch (e: Exception) {
                Log.e("FileReaderViewModel", "Failed to load drawing", e)
                _loadStatus.value = LoadStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Load drawing from database by ID
    fun loadDrawingById(drawingId: Long) {
        viewModelScope.launch {
            _loadStatus.value = LoadStatus.Loading

            try {
                val drawingEntity = withContext(Dispatchers.IO) {
                    drawingsRepository.getDrawingById(drawingId)
                }

                if (drawingEntity != null) {
                    // Store the drawing entity
                    _selectedDrawing.value = drawingEntity

                    // Load the bitmap from the file path
                    loadDrawingFromFile(drawingEntity.filePath)
                } else {
                    _loadStatus.value = LoadStatus.Error("Drawing not found")
                }
            } catch (e: Exception) {
                Log.e("FileReaderViewModel", "Failed to load drawing from database", e)
                _loadStatus.value = LoadStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Delete drawing
    fun deleteDrawing(drawingId: Long) {
        viewModelScope.launch {
            try {
                // Get the drawing to access its file path
                val drawing = drawingsRepository.getDrawingById(drawingId)

                // Delete from database
                drawingsRepository.deleteDrawing(drawingId)

                // Delete the file
                drawing?.let {
                    val file = File(it.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }

                // Clear the selected drawing if it's the one we just deleted
                if (_selectedDrawing.value?.id?.toLong() == drawingId) {
                    _selectedDrawing.value = null
                    _loadedBitmap.value = null
                }
            } catch (e: Exception) {
                Log.e("FileReaderViewModel", "Failed to delete drawing", e)
            }
        }
    }

    // Reset load status
    fun resetLoadStatus() {
        _loadStatus.value = LoadStatus.Idle
    }

    // Clear selected drawing and loaded bitmap
    fun clearSelection() {
        _selectedDrawing.value = null
        _loadedBitmap.value = null
    }
    fun startSelection(id: Long) {
        _isInSelectionMode.value = true
        _selectedIds.value = setOf(id)
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun clearSelectionMode() {
        _isInSelectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun deleteSelectedDrawings() {
        viewModelScope.launch {
            try {
                _selectedIds.value.forEach { id ->
                    val drawing = drawingsRepository.getDrawingById(id)
                    drawingsRepository.deleteDrawing(id)
                    drawing?.let {
                        val file = File(it.filePath)
                        if (file.exists()) file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("FileReaderViewModel", "Failed to delete selected drawings", e)
            } finally {
                clearSelectionMode()
            }
        }
    }
}