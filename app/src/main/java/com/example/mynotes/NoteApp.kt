package com.example.mynotes

import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.mynotes.data.NoteDetails
import com.example.mynotes.data.NoteUiState
import com.example.mynotes.ui.FontSettingsViewModel
import com.example.mynotes.ui.edit.EditViewModel
import com.example.mynotes.ui.home.HomeUiState
import com.example.mynotes.ui.home.HomeViewModel
import com.example.mynotes.ui.navigation.NoteNavHost
import com.facebook.CallbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(35)
@Composable
fun NoteApp(
    activity: AppCompatActivity,
    callbackManager: CallbackManager,
    navController: NavHostController = rememberNavController(),

    ) {
    NoteNavHost(
        activity = activity,
        navController = navController,
        callbackManager = callbackManager
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBar(currentSteam: StateFlow<Int>, quantity: Int, scrollBehavior: TopAppBarScrollBehavior) {
    val current = currentSteam.collectAsState()
    CenterAlignedTopAppBar(
        {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (current.value == 0) "Tất cả ghi chú" else if (current.value == 1) "Yêu thích" else "Bảo mật",
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$quantity ghi chú",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primaryContainer
                )
            }
        },
        expandedHeight = 250.dp,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        )
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsBar(
    homeUiState: HomeUiState,
    viewModel: HomeViewModel,
    navigateToSearch: () -> Unit,
    auth: AuthManager,
    openDrawer: () -> Unit,
    navigateToFileReader:()->Unit
) {
    var isLogin by remember { mutableStateOf(auth.isUserLoggedIn()) }
    val onSelected by remember { mutableStateOf(viewModel.listSelectedNotes) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        auth.authState.collect { loggedIn ->
            isLogin = loggedIn
        }
    }

    var expandState by rememberSaveable { mutableStateOf(false) }
    var expandedViewStates by remember { mutableStateOf(false) }
    val listOfWidthVal = listOf(
        Pair("Dạng Lưới (nhỏ)", 100.dp),
        Pair("Dạng Lưới (vừa)", 150.dp),
        Pair("Danh Sách", 200.dp)
    )
    CenterAlignedTopAppBar(
        {
            if (onSelected.isNotEmpty()) {
                SelectedNoteBar(viewModel)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        Log.d("HomeScreen", "Opening drawer")
                        openDrawer()
                    }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "")
                    }

                    Spacer(Modifier.weight(1.8f))
                    IconButton(onClick = { navigateToSearch() }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "")
                    }

                    IconButton(onClick = { navigateToFileReader() }, Modifier.size(22.dp)) {
                        Icon(painter = painterResource(R.drawable.drawing), contentDescription = "")
                    }
                    Spacer(Modifier.weight(0.1f))

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val result = viewModel.dataSync()
                                withContext(Dispatchers.Main) {
                                    if (result.isSuccess) {
                                        Log.d("ToolsBar", "Data sync result: $result")
                                        Toast.makeText(
                                            context,
                                            "Sao lưu thành công!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Log.e("ToolsBar", "Error: ${result.exceptionOrNull()?.message}")
                                        Toast.makeText(
                                            context,
                                            "Lỗi khi sao lưu: ${result.exceptionOrNull()?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        Modifier.size(24.dp),
                        enabled = isLogin
                    ) {
                        Icon(painter = painterResource(R.drawable.cloud), contentDescription = "")
                    }


                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = { expandState = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "")
                        }
                        DropdownMenu(
                            expanded = expandState,
                            shape = RoundedCornerShape(16),
                            onDismissRequest = { expandState = false }) {
                            DropdownMenuItem(text = {
                                Text(if (viewModel.isFavTop) "Bỏ ghim mục yêu thích" else "Ghim mục yêu thích lên đầu")
                            }, onClick = {
                                viewModel.toggleFavTop()
                                expandState = false
                            })
                            DropdownMenuItem(
                                text = { Text("Dạng Xem") },
                                onClick = {
                                    expandedViewStates = true
                                    expandState = false
                                })
                        }
                        DropdownMenu(
                            expanded = expandedViewStates,
                            shape = RoundedCornerShape(16),
                            onDismissRequest = { expandedViewStates = false },
                        ) {
                            listOfWidthVal.forEach { (label, width) ->
                                DropdownMenuItem(
                                    onClick = {
                                        homeUiState.colWidth.value = width
                                        expandedViewStates = false
                                    },
                                    text = {
                                        Text(
                                            text = label,
                                            color =
                                            if (
                                                homeUiState.colWidth.value == width
                                            ) {
                                                Color.Red
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                            },
                                        )
                                    }
                                )

                            }
                        }
                    }

                }
            }

        },
        expandedHeight = 38.dp,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
@Composable
fun SelectedNoteBar(viewModel: HomeViewModel) {
    val scope = rememberCoroutineScope()
    val allNotes = viewModel.homeUiState.collectAsState().value.noteList.value
    val selectedNotes = viewModel.listSelectedNotes
    val isAllSelected = selectedNotes.size == allNotes.size && allNotes.isNotEmpty()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RadioButton(
                selected = isAllSelected,
                onClick = {
                    viewModel.isSelectedAll = !isAllSelected
                    viewModel.selectedAllNotes()
                },
                colors = RadioButtonDefaults.colors(colorResource(id = R.color.radioBtn)),
                modifier = Modifier
                    .size(20.dp)
                    .padding(0.dp)
            )
            Text("Tất cả", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
        }

        Text(
            "Đã chọn ${selectedNotes.count()}",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 20.sp,
            modifier = Modifier.padding(start = 12.dp)
        )

        Spacer(Modifier.weight(1f))

        IconButton(onClick = {
            scope.launch { viewModel.delSelectedNotes() }
        }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBar(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    scrollBehavior: TopAppBarScrollBehavior
) {
    var expandState by remember { mutableStateOf(false) }
    val incre = remember { mutableStateOf(true) }
    val choiceList = listOf("Tiêu đề", "Ngày sửa đổi")
    fun sortNotes() {
        val sortedList = when (uiState.sortList.value) {
            "Ngày sửa đổi" -> if (incre.value) uiState.noteList.value.sortedByDescending { it.day }
            else uiState.noteList.value.sortedBy { it.day }

            "Tiêu đề" -> if (incre.value) uiState.noteList.value.sortedBy { it.title }
            else uiState.noteList.value.sortedByDescending { it.title }

            else -> uiState.noteList.value
        }
        uiState.noteList.value = sortedList
    }

    fun updateSorting() {
        viewModel.updateSortedNotes()
        sortNotes()
    }

    TopAppBar(
        title = {},
        actions = {
            Row {
                Spacer(Modifier.weight(3f))
                IconButton(onClick = { expandState = true }, Modifier.weight(1.5f)) {
                    Row {
                        Icon(
                            painterResource(R.drawable.sort),
                            contentDescription = "",
                            Modifier.size(24.dp)
                        )
                        Text(
                            uiState.sortList.value,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
                DropdownMenu(
                    expanded = expandState,
                    onDismissRequest = { expandState = false },
                    shape = RoundedCornerShape(16),
                    offset = DpOffset(x = 230.dp, y = (-10).dp)
                ) {
                    choiceList.forEach { choice ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    choice, color = if (uiState.sortList.value == choice) Color.Red
                                    else MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                uiState.sortList.value = choice
                                updateSorting()
                                expandState = false
                            }
                        )
                    }
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp)
                        .align(Alignment.CenterVertically), color = Color.Gray
                )
                IconButton(onClick = {
                    incre.value = !incre.value
                    updateSorting()
                }, Modifier.weight(0.5f)) {
                    Icon(
                        imageVector = if (incre.value) Icons.Default.KeyboardArrowDown
                        else Icons.Default.KeyboardArrowUp,
                        contentDescription = ""
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTopAppBar(
    onNavBack: () -> Unit,
    uiState: NoteUiState,
    onValChange: (NoteDetails) -> Unit,
) {
    CenterAlignedTopAppBar(
        {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.noteDetail.title,
                    onValueChange = {
                        onValChange(
                            uiState.noteDetail.copy(title = it)
                        )
                    },
                    Modifier
                        .weight(2f)
                        .align(Alignment.CenterVertically), textStyle = TextStyle(
                        fontSize = 25.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    placeholder = { Text("Nhập tiêu đề", fontSize = 24.sp) },

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    )

                )
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                onNavBack()
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTopAppBar(
    onNavBack: () -> Unit,
    viewModel: EditViewModel,
    onValChange: (NoteDetails) -> Unit,
    onDelete: () -> Unit,
    onFavourite: () -> Unit,
    onHidden: () -> Unit,
    showReminderDialog: Boolean,
    onShowReminderDialogChange: (Boolean) -> Unit,
    navigateToCover: (Int) -> Unit,
    onSaveAsPdfFile: () -> Unit,
    onSaveAsWordFile: () -> Unit,
    fontViewModel: FontSettingsViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dropdownMenuState by remember { mutableStateOf(false) }
    var dropdownMenuFileState by rememberSaveable { mutableStateOf(false) }
    var dropdownShareFileMenu by rememberSaveable { mutableStateOf(false) }
    var dropdownFontSizeSelect by rememberSaveable { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                OutlinedTextField(
                    value = viewModel.noteUiState.noteDetail.title,
                    onValueChange = {
                        onValChange(
                            viewModel.noteUiState.noteDetail.copy(title = it)
                        )
                    },
                    Modifier
                        .weight(5f)
                        .horizontalScroll(rememberScrollState()),
                    textStyle = TextStyle(
                        fontSize = 25.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    placeholder = { Text("Nhập tiêu đề", fontSize = 20.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    maxLines = 1,
                    singleLine = true
                )
                IconButton(onClick = { dropdownMenuState = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    )
                }
                if (dropdownMenuState) {
                    DropdownMenu(
                        offset = DpOffset(x = 240.dp, y = -40.dp),
                        expanded = dropdownMenuState,
                        shape = RoundedCornerShape(24.dp),
                        onDismissRequest = { dropdownMenuState = false }
                    ) {
                        DropDownItem(
                            icon = Icons.Default.Notifications,
                            text = "Tạo nhắc nhở",
                            onItemClick = {
                                dropdownMenuState = false
                                onShowReminderDialogChange(true) }
                        )
                        DropDownItem(
                            icon = Icons.Default.Star,
                            text = if (viewModel.isFavourite == 1) "Xóa khỏi mục ưa thích" else "Thêm vào mục ưa thích",
                            onItemClick = {

                                onFavourite() }
                        )
                        DropDownItem(
                            icon = Icons.Default.Face,
                            text = "Sửa bìa",
                            onItemClick = { navigateToCover(viewModel.noteUiState.noteDetail.id) }
                        )
                        DropDownItem(
                            icon = if (viewModel.noteUiState.noteDetail.status == -1) Icons.Default.Refresh else Icons.Default.Lock,
                            text = if (viewModel.noteUiState.noteDetail.status == -1) "Hiện ghi chú" else "Ẩn ghi chú",
                            onItemClick = { onHidden() }
                        )
                        DropDownItem(
                            icon = Icons.Default.Share,
                            text = "Chia sẻ",
                            onItemClick = {
                                dropdownShareFileMenu = true
                                dropdownMenuState = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.save_as_file),
                                    contentDescription = "",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Lưu làm file",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                dropdownMenuFileState = true
                                dropdownMenuState = false
                            }
                        )
                        DropDownItem(
                            icon = Icons.Default.Delete,
                            text = "Xóa",
                            onItemClick = { onDelete() }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.font_size),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            text = {
                                Text(
                                    text = "Cỡ chữ",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                dropdownFontSizeSelect = true
                                dropdownMenuState = false
                            }
                        )


                    }
                }
                if (dropdownMenuFileState) {
                    DropdownMenu(
                        offset = DpOffset(x = 240.dp, y = (-40).dp),
                        expanded = dropdownMenuFileState,
                        shape = RoundedCornerShape(22),
                        onDismissRequest = {
                            dropdownMenuFileState = false
                        },
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.pdf),
                                    contentDescription = "",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Lưu file Pdf",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                onSaveAsPdfFile()
                                dropdownMenuFileState = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.word),
                                    contentDescription = "",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Lưu file Word",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                onSaveAsWordFile()
                                dropdownMenuFileState = false
                            }
                        )
                    }
                }
                if (dropdownShareFileMenu) {
                    DropdownMenu(
                        offset = DpOffset(x = 240.dp, y = (-40).dp),
                        shape = RoundedCornerShape(22),
                        expanded = dropdownShareFileMenu,
                        onDismissRequest = {
                            dropdownShareFileMenu = false
                        },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Dạng văn bản",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                viewModel.shareNote(context = context)
                                dropdownShareFileMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Dạng file Pdf",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                scope.launch {
                                    try {
                                        val pdfFile = viewModel.createPdfFile(context.cacheDir)
                                        viewModel.shareFile(context = context, pdfFile)
                                    } catch (e: Exception) {
                                        Log.e("Share", "Loi tao file: ${e.message}")
                                    }
                                }
                                dropdownMenuFileState = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Dạng file Word",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                scope.launch {
                                    try {
                                        val wordFile = viewModel.createWordFile(context.cacheDir)
                                        viewModel.shareFile(context = context, wordFile)
                                    } catch (e: Exception) {
                                        Log.e("Share", "Loi tao file: ${e.message}")
                                    }
                                }
                                dropdownMenuFileState = false
                            }
                        )
                    }
                }
                if (dropdownFontSizeSelect) {
                    DropdownMenu(
                        offset = DpOffset(x = 240.dp, y = (-40).dp),
                        shape = RoundedCornerShape(22),
                        expanded = dropdownFontSizeSelect,
                        onDismissRequest = { dropdownFontSizeSelect = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Nhỏ",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                fontViewModel.updateFontSize(16.sp)
                                dropdownFontSizeSelect = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Trung bình",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                fontViewModel.updateFontSize(20.sp)
                                dropdownFontSizeSelect = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Lớn",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            onClick = {
                                fontViewModel.updateFontSize(30.sp)
                                dropdownFontSizeSelect = false
                            }
                        )

                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                onNavBack()
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },

        )
}

@Composable
fun DropDownItem(icon: ImageVector, text: String, onItemClick: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        },
        text = { Text(text = text, color = MaterialTheme.colorScheme.onPrimary) },
        onClick = { onItemClick() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
    CenterAlignedTopAppBar(
        {
            Text(
                "Tìm Kiếm",
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        },
        expandedHeight = 220.dp,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        )

    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    onNavBack: () -> Unit,
    uiState: String,
    onValChange: (String) -> Unit,
) {
    CenterAlignedTopAppBar(
        {
            Row(Modifier.fillMaxWidth()) {

                OutlinedTextField(
                    value = uiState,
                    onValueChange = { newVal ->
                        onValChange(
                            newVal
                        )
                    },
                    Modifier
                        .weight(5f)
                        .height(58.dp)
                        .horizontalScroll(rememberScrollState()),
                    textStyle = TextStyle(fontSize = 25.sp),
                    placeholder = { Text("Tìm kiếm", fontSize = 20.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    maxLines = 1,
                    singleLine = true
                )

            }
        },
        navigationIcon = {
            IconButton(onClick = onNavBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverTopAppBar(title: String, onNavBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavBack) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.background)
    )

}
