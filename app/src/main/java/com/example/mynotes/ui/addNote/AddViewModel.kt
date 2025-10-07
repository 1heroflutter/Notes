package com.example.mynotes.ui.addNote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.mynotes.data.NoteDetails
import com.example.mynotes.data.NoteUiState
import com.example.mynotes.data.NotesRepository
import com.example.mynotes.data.toItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AddViewModel(private val notesRepository: NotesRepository) : ViewModel() {
    var noteUiState by mutableStateOf(NoteUiState())
        private set
    private val firestore = FirebaseFirestore.getInstance()
    private val notesCollection = firestore.collection("notes")

    fun updateUiState(noteDetails: NoteDetails) {
        noteUiState = NoteUiState(noteDetail = noteDetails, isEntryValid = validateInput())
    }

    private fun validateInput(uiState: NoteDetails = noteUiState.noteDetail): Boolean {
        return with(uiState) {
            context.isNotBlank()
        }
    }

    suspend fun saveNote() {
        withContext(Dispatchers.IO) {
            if (validateInput()) {
                if (noteUiState.noteDetail.title.isBlank()) {
                    val words = noteUiState.noteDetail.context.trim().split("\\s+".toRegex())
                    noteUiState.noteDetail.title = words.take(3).joinToString(" ")
                }
                notesRepository.insertNote(noteUiState.noteDetail.toItem())
                notesCollection.document(noteUiState.noteDetail.id.toString())
                    .set(noteUiState.noteDetail.toItem())
                    .await()
            }
        }
    }
}


