package com.example.mynotes

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mynotes.ui.Auth.AuthViewModel
import com.example.mynotes.ui.addNote.AddViewModel
import com.example.mynotes.ui.changePassword.ChangePasswordViewModel
import com.example.mynotes.ui.draw.DrawingViewModel
import com.example.mynotes.ui.edit.EditViewModel
import com.example.mynotes.ui.fileReader.FileReaderViewModel
import com.example.mynotes.ui.home.HomeViewModel
import com.example.mynotes.ui.search.SearchViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(noteApplication().container.notesRepository)
        }
        initializer {
            AddViewModel(
                noteApplication().container.notesRepository
            )
        }
        initializer {
            EditViewModel(
                noteApplication().container.notesRepository,
                this.createSavedStateHandle(),
                noteApplication().container.notifyRepository,

                )
        }
        initializer {
            SearchViewModel(
                noteApplication().container.notesRepository
            )
        }
        initializer {
            AuthViewModel()
        }
        initializer {
            DrawingViewModel(noteApplication().container.drawingsRepository)
        }
        initializer {
            FileReaderViewModel(
                noteApplication().container.drawingsRepository
            )
        }
        initializer {
            ChangePasswordViewModel()
        }
    }
}

fun CreationExtras.noteApplication(): NoteApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NoteApplication)