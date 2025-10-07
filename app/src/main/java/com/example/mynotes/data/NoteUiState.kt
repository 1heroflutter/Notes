package com.example.mynotes.data

import java.time.LocalDate


data class NoteUiState (
    val noteDetail: NoteDetails = NoteDetails(),
    val isEntryValid: Boolean = false
)
data class NoteDetails(
    val id: Int = 0,
    var title: String = "",
    var context: String = "",
    val day: String = "",
    val status: Int = 0,
    val cover: String = "cover_1",
    val coverOn: Boolean = false,
    val userUID: String = "",
)
fun NoteDetails.toItem(): Note = Note(
    id = id,
    title = title,
    context = context,
    day = LocalDate.now().toString(),
    status = status,
    cover = cover,
    coverOn = coverOn,
    userUID = userUID,
)

fun Note.toNoteUiState(b: Boolean): NoteUiState = NoteUiState(
    noteDetail = this.toNoteDetail()
)

fun Note.toNoteDetail(): NoteDetails = NoteDetails(
    id = id,
    title = title,
    context = context,
    day = day,
    status = status,
    cover = cover,
    coverOn = coverOn,
    userUID = userUID
)
