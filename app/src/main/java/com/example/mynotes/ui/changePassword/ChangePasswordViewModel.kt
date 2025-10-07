package com.example.mynotes.ui.changePassword

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ChangePasswordViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState

    fun changePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
            user.reauthenticate(credential)
                .addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    _uiState.update { it.copy(message = "Đổi mật khẩu thành công", isSuccess = true) }
                                } else {
                                    _uiState.update { it.copy(message = "Đổi mật khẩu thất bại: ${updateTask.exception?.localizedMessage}") }
                                }
                            }
                    } else {
                        _uiState.update { it.copy(message = "Mật khẩu cũ không đúng: ${reauthTask.exception?.localizedMessage}") }
                    }
                }
        } else {
            _uiState.update { it.copy(message = "Không tìm thấy người dùng") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class ChangePasswordUiState(
    val message: String? = null,
    val isSuccess: Boolean = false
)
