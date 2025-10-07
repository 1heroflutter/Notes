package com.example.mynotes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel

class FontSettingsViewModel : ViewModel() {
    var fontSize by mutableStateOf(20.sp)
        private set

    fun updateFontSize(newSize: TextUnit) {
        fontSize = newSize
    }

}
