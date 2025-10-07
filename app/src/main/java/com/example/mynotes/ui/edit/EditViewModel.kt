package com.example.mynotes.ui.edit

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotes.data.NoteDetails
import com.example.mynotes.data.NoteUiState
import com.example.mynotes.data.NotesRepository
import com.example.mynotes.data.NotifyRepository
import com.example.mynotes.data.Reminder
import com.example.mynotes.data.toItem
import com.example.mynotes.data.toNoteUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.graphics.Paint
import android.icu.text.SimpleDateFormat
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.util.Date
import java.util.Locale

class EditViewModel(
    private val notesRepository: NotesRepository,
    savedStateHandle: SavedStateHandle,
    private val notifyRepository: NotifyRepository
) : ViewModel() {
    var noteUiState by mutableStateOf(NoteUiState())
        private set
    private val noteId: Int = checkNotNull(savedStateHandle[NoteEditDestination.noteIdArg])
    var isFavourite by mutableIntStateOf(0)
    var isHidden by mutableIntStateOf(0)

    fun updateUiState(noteDetails: NoteDetails) {
        noteUiState =
            NoteUiState(noteDetail = noteDetails, isEntryValid = validateInput(noteDetails))
    }

    init {
        viewModelScope.launch {
            noteUiState = notesRepository.getNotesStream(noteId)
                .filterNotNull()
                .first()
                .toNoteUiState(true)
        }
    }

    private fun validateInput(uiState: NoteDetails = noteUiState.noteDetail): Boolean {
        return with(uiState) {
            context.isNotBlank()
        }
    }

    suspend fun saveNote() {
        withContext(Dispatchers.IO) {
            notesRepository.updateNote(noteUiState.noteDetail.toItem())
        }
    }

    suspend fun delNote() {
        withContext(Dispatchers.IO) {
            notesRepository.deleteNote(noteUiState.noteDetail.toItem())
        }
    }

    fun favourites() {
        isFavourite = if (noteUiState.noteDetail.status == 0) 1 else 0
        val updatedNote = noteUiState.noteDetail.copy(status = isFavourite)

        updateUiState(updatedNote)

        viewModelScope.launch {
            notesRepository.updateNote(updatedNote.toItem())
        }
    }

    fun hidden(onDone: (Int) -> Unit) {
        isHidden = if (noteUiState.noteDetail.status != -1) -1 else 0
        val updatedNote = noteUiState.noteDetail.copy(status = isHidden)

        updateUiState(updatedNote)

        viewModelScope.launch {
            try {
                notesRepository.updateNote(updatedNote.toItem())
                onDone(isHidden)
            } catch (e: Exception) {
                onDone(noteUiState.noteDetail.status)
            }
        }
    }




    fun scheduleReminder(reminder: Reminder) {
        notifyRepository.scheduleReminder(
            reminder.date,
            reminder.hour,
            reminder.minute,
            reminder.context
        )
    }
    //Lưu file pdf + word
    fun exportNoteAsPdf(): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint().apply {
            textSize = 16f
        }

        val text = noteUiState.noteDetail.context
        val textWidth = pageInfo.pageWidth - 80
        val x = 40f
        val y = 40f

        val textLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, TextPaint(paint), textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        textLayout.draw(canvas)
        canvas.restore()

        pdfDocument.finishPage(page)

        // Lưu vào thư mục công khai để dễ tìm
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
        val baseName = noteUiState.noteDetail.title.ifBlank { "note" }
        val fileName = "${baseName}_$timestamp.pdf"
        val file = File(downloadsDir, fileName)

        Log.d("ExportPDF", "Saving file to: ${file.absolutePath}")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }

        pdfDocument.close()
        return file
    }

    private suspend fun exportNoteAsDocx(): File = withContext(Dispatchers.IO) {
        val document = XWPFDocument()
        val paragraph = document.createParagraph()
        val run = paragraph.createRun()
        run.setFontSize(18)
        run.setText(noteUiState.noteDetail.context)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val baseName = noteUiState.noteDetail.title.ifBlank { "note" }
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${baseName}_$timestamp.docx"
        val file = File(downloadsDir, fileName)

        FileOutputStream(file).use { out ->
            document.write(out)
        }
        document.close()
         file
    }
    fun exportNoteAsDocxAsync( onComplete:(File)->Unit){
        viewModelScope.launch(Dispatchers.IO) {
            val file = exportNoteAsDocx()
            withContext(Dispatchers.Main){
                onComplete(file)
            }
        }

    }
// share file
    fun shareNote(context: Context) {
        val intent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, noteUiState.noteDetail.context)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }, null)

        context.startActivity(intent)
    }
    suspend fun createPdfFile(cacheDir: File): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint().apply {
            textSize = 16f
        }

        val text = noteUiState.noteDetail.context
        val textWidth = pageInfo.pageWidth - 80
        val x = 40f
        val y = 40f

        val textLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, TextPaint(paint), textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        textLayout.draw(canvas)
        canvas.restore()

        pdfDocument.finishPage(page)

        val baseName = noteUiState.noteDetail.title.ifBlank { "note" }
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${baseName}_$timestamp.pdf"
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }

        pdfDocument.close()
        file
    }

    suspend fun createWordFile(cacheDir: File): File = withContext(Dispatchers.IO) {
        val document = XWPFDocument()
        val paragraph = document.createParagraph()
        val run = paragraph.createRun()
        run.setFontSize(18)
        run.setText(noteUiState.noteDetail.context)

        val baseName = noteUiState.noteDetail.title.ifBlank { "note" }
        val timestamp = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${baseName}_$timestamp.docx"
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { out ->
            document.write(out)
        }
        document.close()
        file
    }
    fun shareFile(context: Context, file: File){
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/file"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Share file"))
        } catch (e:Exception){
            Log.e("ShareFile", "Error sharing File: ${e.message}")
        }

    }
}
