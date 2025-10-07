package com.example.mynotes.ui.home

import android.util.Log
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotes.data.Note
import com.example.mynotes.data.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(private val notesRepository: NotesRepository) : ViewModel() {
    private val _currentNotesStream = MutableStateFlow(0)
    val currentNotesStream: StateFlow<Int> get() = _currentNotesStream

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> get() = _homeUiState

    private val _listSelectedNotes = mutableStateListOf<Int>()
    val listSelectedNotes: List<Int> get() = _listSelectedNotes
    var isFavTop by mutableStateOf(false)
    var isSelectedAll by mutableStateOf(false)
    init {
        viewModelScope.launch {
            _currentNotesStream.collectLatest { streamType ->
                val newFlow = when (streamType) {
                    0 -> notesRepository.getAllNotesStream()
                    1 -> notesRepository.getAllFavouriteNotes()
                    -1 -> notesRepository.getAllSecureNotes()
                    else -> emptyFlow()
                }.map { HomeUiState(mutableStateOf(it)) }

                newFlow.collectLatest { newState ->
                    _homeUiState.value = newState
                }
            }
        }
    }

    fun updateStream(type: Int) {
        _currentNotesStream.value = type
    }

    fun toggleSelection(noteId: Int) {
        if (_listSelectedNotes.contains(noteId)) {
            _listSelectedNotes.remove(noteId)
        } else {
            _listSelectedNotes.add(noteId)
        }
    }
    fun selectedAllNotes() {
        if (isSelectedAll) {
            _homeUiState.value.noteList.value.forEach { note ->
                if (!_listSelectedNotes.contains(note.id)) {
                    _listSelectedNotes.add(note.id)
                }
            }
        } else {
            _listSelectedNotes.clear()
        }
    }


    fun toggleFavTop() {
        isFavTop = !isFavTop
        updateSortedNotes()
    }

    fun updateSortedNotes() {
        val sortedNotes = if (isFavTop) {
            homeUiState.value.noteList.value.sortedByDescending { it.status }
        } else {
            homeUiState.value.noteList.value
        }
        homeUiState.value.noteList.value = sortedNotes
    }

    suspend fun delSelectedNotes() {
        withContext(Dispatchers.IO) {
            notesRepository.deleteNoteByListId(_listSelectedNotes)
            _listSelectedNotes.clear()
        }
    }
    suspend fun dataSync(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                Log.d("Repository", "Bắt đầu đồng bộ...")
                notesRepository.dataSync()
            }
            Log.d("Repository", "Đồng bộ xong không lỗi")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Repository", "Lỗi khi đồng bộ", e)
            Result.failure(e)
        }
    }

}

data class HomeUiState(
    var noteList: MutableState<List<Note>> = mutableStateOf(listOf()),
    val quantity: Int = noteList.value.count(),
    var colWidth: MutableState<Dp> = mutableStateOf(135.dp),
    var sortList: MutableState<String> = mutableStateOf("Ngày sửa đổi"),
)
