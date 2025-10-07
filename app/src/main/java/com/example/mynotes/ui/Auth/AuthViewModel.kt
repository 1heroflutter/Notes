package com.example.mynotes.ui.Auth

import android.provider.ContactsContract.CommonDataKinds.Email
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.mynotes.data.NotesRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class AuthViewModel :ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _signInState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val SignInState: StateFlow<FirebaseUser?> = _signInState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun handleSignInResult(task: Task<GoogleSignInAccount>){
        try{
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            signInWithGoogle(credential)
        }catch (e:ApiException){
            _errorMessage.value = "Đăng nhập thất bại: ${e.message}"
        }
    }
    private fun signInWithGoogle(credential: AuthCredential){
        auth.signInWithCredential(credential)
            .addOnCompleteListener{task->
                if(task.isSuccessful){
                    _signInState.value = auth.currentUser
                }else{
                    _errorMessage.value = "Xác thực thất bại"
                }
            }
    }
    fun handleFacebookAccessToken(token: AccessToken) {
        Log.d("FacebookLogin", "Handling token: ${token.token}")
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FacebookLogin", "Firebase sign-in success")
                    _signInState.value = auth.currentUser
                }else {
                    val exception = task.exception
                    if(exception is FirebaseAuthUserCollisionException){
                        _errorMessage.value ="Email của tài khoảng đã được sử dụng"
                    }

                    Log.e("FacebookLogin", "Firebase sign-in failed: ${task.exception}")
                    _errorMessage.value = "Gặp lỗi khi đăng nhập bằng Facebook"
                }
            }
    }

    fun signIn(email: String, passWord: String){
        auth.signInWithEmailAndPassword(email, passWord)
            .addOnCompleteListener{task->
                if(task.isSuccessful){
                    _signInState.value = auth.currentUser
                }else {
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            _errorMessage.value = "Email không tồn tại hoặc đã bị vô hiệu hóa"
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            _errorMessage.value = "Mật khẩu không chính xác"
                        }
                        else -> {
                            _errorMessage.value = exception?.localizedMessage ?: "Đăng nhập thất bại"
                        }
                    }

                }
            }
    }
    fun register(email: String, passWord: String, onSuccess: () -> Unit = {}) {
        auth.createUserWithEmailAndPassword(email, passWord)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _errorMessage.value = "Đăng ký thành công"
                    onSuccess()
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        _errorMessage.value = "Email đã được sử dụng!"
                    } else {
                        _errorMessage.value = exception?.localizedMessage ?: "Lỗi hệ thống! Vui lòng thử lại"
                        Log.e("AuthViewModel", "Đăng ký thất bại", exception)
                    }
                }
            }
    }

    fun signOut(){
        auth.signOut()
        _signInState.value = null
    }
    fun forgetPassword(email: String){
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener{task->
                if(task.isSuccessful){
                    _errorMessage.value = "Gửi thành công"
                }else{
                    _errorMessage.value = "Gửi thất bại"
                }
            }
    }
}