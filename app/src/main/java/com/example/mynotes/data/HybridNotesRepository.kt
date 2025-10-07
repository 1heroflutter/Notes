package com.example.mynotes.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class HybridNotesRepository(
    private val offlineRepo: OfflineNotesRepository,
    private val cloudRepo: CloudNotesRepository
) : NotesRepository {
    override fun getAllNotesStream(): Flow<List<Note>> = flow {
        Log.d("HybridNotesRepo", "Fetching local notes...")
        val userUID = cloudRepo.getCurrentUserId()
        if (userUID.isNullOrEmpty()) {
            Log.w("HybridNotesRepo", "User not logged in! Using local data only.")
            emitAll(offlineRepo.getAllNotesStream())
            return@flow
        }


        cloudRepo.getAllNotesStream().collect { cloudNotes ->
            Log.d("HybridNotesRepo", "Fetched ${cloudNotes.size} notes from cloud")
            val userNotes = cloudNotes.filter { it.userUID == userUID }
            Log.d("HybridNotesRepo", "Filtered ${userNotes.size} notes for user $userUID")

            userNotes.forEach { note ->
                Log.d("HybridNotesRepo", "Updating local DB with note ID: ${note.id}")
                offlineRepo.insertOrUpdate(note)
            }
            emitAll(offlineRepo.getAllNotesStream())
        }
    }


    override fun getNotesStream(id: Int): Flow<Note?> = flow {
        emitAll(offlineRepo.getNotesStream(id)) // Lấy dữ liệu từ Room trước

        val cloudNote = cloudRepo.getNotesStream(id).firstOrNull()
        cloudNote?.let { offlineRepo.insertNote(it) } // Đồng bộ nếu có dữ liệu mới
    }

    override suspend fun insertNote(note: Note) {
        offlineRepo.insertNote(note)
    }

    override suspend fun deleteNote(note: Note) {
        offlineRepo.deleteNote(note)
        cloudRepo.deleteNote(note)
    }

    override suspend fun updateNote(note: Note) {
        offlineRepo.updateNote(note)
        cloudRepo.updateNote(note)
    }

    override suspend fun deleteNoteByListId(ids: List<Int>) {
        offlineRepo.deleteNoteByListId(ids)
        cloudRepo.deleteNoteByListId(ids)
    }

    override fun getAllFavouriteNotes(): Flow<List<Note>> = offlineRepo.getAllFavouriteNotes()

    override fun getAllSecureNotes(): Flow<List<Note>> = offlineRepo.getAllSecureNotes()

    override fun getNotesByTitle(title: String): Flow<List<Note>> =
        offlineRepo.getNotesByTitle(title)

    override suspend fun dataSync() {
        val userUid = cloudRepo.getCurrentUserId()
        if (userUid.isNullOrEmpty()) {
            Log.e("HybridNotesRepo", "Cannot sync: User not logged in!")
            return
        }

        withContext(Dispatchers.IO) {
            val notes = offlineRepo.getAllNotesStream().first() // DỪNG sau khi nhận lần đầu
            notes.forEach { note ->
                if (note.userUID.isEmpty()) {
                    val updatedNote = note.copy(userUID = userUid)
                    cloudRepo.updateNote(updatedNote)
                    Log.d("HybridNotesRepo", "Synced note ${note.id} to cloud with user $userUid")
                } else if (note.userUID == userUid) {
                    cloudRepo.updateNote(note)
                    Log.d("HybridNotesRepo", "Updated existing note ${note.id} for user $userUid")
                } else {
                    Log.w("HybridNotesRepo", "Skipped note ${note.id}: belongs to another user.")
                }
            }
        }
    }


}
