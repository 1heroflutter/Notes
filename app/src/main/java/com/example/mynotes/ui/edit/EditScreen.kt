package com.example.mynotes.ui.edit

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState.Saver.save
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mynotes.AppViewModelProvider
import com.example.mynotes.EditTopAppBar
import com.example.mynotes.data.NoteDetails
import com.example.mynotes.data.NoteUiState
import com.example.mynotes.data.Reminder
import com.example.mynotes.ui.FontSettingsViewModel
import com.example.mynotes.ui.navigation.NavigationDestination
import kotlinx.coroutines.launch
import org.openxmlformats.schemas.drawingml.x2006.main.STTextFontSize
import java.util.Calendar

object NoteEditDestination : NavigationDestination {
    override val route = "note_edit"
    override val titleRes: Int
        get() = TODO("Not yet implemented")
    const val noteIdArg = "noteId"

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    navigateBack: () -> Unit,
    viewModel: EditViewModel = viewModel(factory = AppViewModelProvider.Factory),
    modifier: Modifier = Modifier,
    navigateToCover: (Int) -> Unit,
) {
    val fontSettingsViewModel: FontSettingsViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val editUiState = viewModel.noteUiState
    var showReminderDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(
        topBar = {
            EditTopAppBar(
                navigateBack, viewModel, viewModel::updateUiState,
                onDelete = {
                    coroutineScope.launch {
                        viewModel.delNote()
                        navigateBack()
                    }
                },
                onFavourite = {
                    coroutineScope.launch {
                        viewModel.favourites()
                    }
                },
                onHidden = {
                    viewModel.hidden { newStatus ->
                        Toast.makeText(
                            context,
                            if (newStatus == -1) "Ghi chú đã được ẩn" else "Ghi chú đã hiển thị",
                            Toast.LENGTH_SHORT
                        ).show()

                        coroutineScope.launch {
                            viewModel.saveNote()
                        }
                    }
                },

                showReminderDialog,
                onShowReminderDialogChange = { showReminderDialog = it },
                navigateToCover = { id -> navigateToCover(id) },
                onSaveAsPdfFile = {
                    val file = viewModel.exportNoteAsPdf()
                    Toast.makeText(
                        context,
                        "Đã lưu PDF tại: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onSaveAsWordFile = {
                    viewModel.exportNoteAsDocxAsync() { file ->
                        Toast.makeText(
                            context,
                            "Đã lưu Word tại : ${file.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                fontViewModel = fontSettingsViewModel
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                TextInp(
                    noteUiState = editUiState,
                    onValueChange = viewModel::updateUiState,
                    onSave = {
                        coroutineScope.launch {
                            viewModel.saveNote()
                        }
                    },
                    fontSize = fontSettingsViewModel.fontSize
                )
                if (showReminderDialog) {
                    ReminderDialogContent(
                        onDialogDismiss = { showReminderDialog = false },
                        context = viewModel.noteUiState.noteDetail.context,
                        onScheduleReminder = viewModel::scheduleReminder,
                    )
                }
            }

        }
    }
}
@Composable
fun TextInp(
    modifier: Modifier = Modifier,
    noteUiState: NoteUiState,
    onValueChange: (NoteDetails) -> Unit,
    onSave: () -> Unit,
    fontSize: TextUnit
) {
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        Spacer(
            modifier = Modifier.weight(0.5f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )

        // SCROLLABLE CONTENT AREA
        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            OutlinedTextField(
                value = noteUiState.noteDetail.context,
                onValueChange = {
                    onValueChange(noteUiState.noteDetail.copy(context = it))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.background,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedBorderColor = MaterialTheme.colorScheme.background
                ),
                textStyle = TextStyle(
                    fontSize = fontSize,
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 600.dp),
                maxLines = 28
            )
        }

        // SAVE BUTTON
        Box(
            modifier = Modifier.weight(0.5f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            FloatingActionButton(
                onClick = onSave,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialogContent(
    onDialogDismiss: () -> Unit,
    context: String,
    onScheduleReminder: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by rememberSaveable { mutableStateOf(true) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    var selectedDateMillis by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var selectedHour by rememberSaveable {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }
    var selectedMinute by rememberSaveable {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.MINUTE))
    }

    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = true
    )

    val appContext = LocalContext.current

    Box(
        modifier
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(25.dp)
    ) {
        if (showDatePicker) {
            DatePickerModal(
                onDateSelected = { dateMillis ->
                    if (dateMillis != null) {
                        selectedDateMillis = dateMillis
                        showDatePicker = false
                        showTimePicker = true
                    }
                },
                onDismiss = { showDatePicker = false }
            )
        } else if (showTimePicker) {
            TimePickerModal(
                timePickerState = timePickerState,
                onDismiss = { onDialogDismiss() },
                onConfirm = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute

                    selectedDateMillis?.let { millis ->
                        val reminder = Reminder(millis, selectedHour, selectedMinute, context)
                        onScheduleReminder(reminder)
                        Toast.makeText(
                            appContext,
                            "Đặt nhắc nhở thành công",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    onDialogDismiss()
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    onDismiss()
                }
            ) {
                Text("Chọn")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModal(
    timePickerState: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TimePicker(
            state = timePickerState,
        )
        Row {
            Button(onClick = onDismiss, Modifier.weight(1f)) {
                Text("Hủy")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onConfirm, Modifier.weight(1f)) {
                Text("Đặt nhắc nhở")
            }
        }
    }
}


