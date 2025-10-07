package com.example.mynotes.ui.Auth

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mynotes.AppViewModelProvider
import com.example.mynotes.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigateBack: () -> Unit,
    navigateToForgotPassword: () -> Unit,
    callbackManager: CallbackManager
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Log.d("FacebookLogin", "Login success, token: ${result.accessToken}")
                    viewModel.handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Log.d("FacebookLogin", "Facebook login cancelled")
                }

                override fun onError(error: FacebookException) {
                    Log.e("FacebookLogin", "Facebook login error: ${error.message}")
                }
            })
    }
    Login(
        viewModel, context = context, modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        },
        navigateBack = navigateBack,
        navigateToForgotPassword = navigateToForgotPassword
    )
    val errorMessage by viewModel.errorMessage.collectAsState()
    errorMessage?.let { error ->
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }

    val user by viewModel.SignInState.collectAsState()
    LaunchedEffect(user) {
        if (user != null) {
            Toast.makeText(context, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
            navigateBack()
        }
    }
}

@Composable
fun Login(
    viewModel: AuthViewModel,
    context: Context,
    navigateBack: () -> Unit,
    navigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current
    var currentStep by rememberSaveable { mutableStateOf(0) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            viewModel.handleSignInResult(task)
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IconButton(
            onClick = { navigateBack() },
            modifier = Modifier
                .padding(WindowInsets.systemBars.asPaddingValues())
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.icon),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (currentStep) {
                0 -> WelcomeScreen(
                    onEmailClick = { currentStep = 1 },
                    onGoogleClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    onFacebookClick = {
                        LoginManager.getInstance().logInWithReadPermissions(
                            activity as Activity,
                            listOf("email", "public_profile")
                        )
                    }
                )

                1 -> EmailLoginForm(
                    onBack = { currentStep = 0 },
                    viewModel = viewModel,
                    onForgotPassword = navigateToForgotPassword
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (currentStep == index) MaterialTheme.colorScheme.primary else Color.Gray)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onGoogleClick: () -> Unit,
    onFacebookClick: () -> Unit,
    onEmailClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Chào mừng đến với Notes",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            "Hơn cả một ứng dụng ghi chú!",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGoogleClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tiếp tục với Google", color = Color.White)
            Icon(
                painter = painterResource(R.drawable.google),
                contentDescription = "",
                modifier = Modifier
                    .size(22.dp)
                    .padding(start = 5.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onFacebookClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tiếp tục với Facebook", color = Color.White)
            Icon(
                painter = painterResource(R.drawable.facebook),
                contentDescription = "",
                modifier = Modifier
                    .size(22.dp)
                    .padding(start = 5.dp),
                tint = Color.Unspecified
            )

        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onEmailClick,
            border = BorderStroke(1.dp, Color.Black),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.onPrimary)
        ) {
            Text("Tiếp tục với Email", color = MaterialTheme.colorScheme.background)
            Icon(
                imageVector = Icons.Default.MailOutline,
                contentDescription = "",
                modifier = Modifier.padding(start = 5.dp),
                tint = MaterialTheme.colorScheme.background
            )

        }
    }
}

@Composable
fun EmailLoginForm(
    onBack: () -> Unit,
    viewModel: AuthViewModel,
    onForgotPassword: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    var password by rememberSaveable { mutableStateOf("") }
    var reEnterPass by rememberSaveable { mutableStateOf("") }
    var passVisible by rememberSaveable { mutableStateOf(false) }
    var isLoginMode by rememberSaveable { mutableStateOf(true) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Login",
                    color = if (isLoginMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { isLoginMode = true }
                )
                if (isLoginMode) {
                    Divider(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        thickness = 2.dp,
                        modifier = Modifier.width(50.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Register",
                    color = if (!isLoginMode) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { isLoginMode = false }
                )
                if (!isLoginMode) {
                    Divider(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        thickness = 2.dp,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email") },
            singleLine = true,
            trailingIcon = {
                Icon(imageVector = Icons.Default.MailOutline, contentDescription = null)
            },
            supportingText = {
                if (email.isNotEmpty() && !email.contains("@")) Text("Vui lòng nhập email hợp lệ")
            },
            shape = RoundedCornerShape(12.dp),
            isError = email.isNotEmpty() && !email.contains("@"),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it.trim() },
            label = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = { passVisible = !passVisible }) {
                    Icon(
                        painter = if (passVisible) painterResource(R.drawable.visibility) else painterResource(R.drawable.visible_off),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            supportingText = {
                if (password.isNotEmpty() && password.length < 6) Text("Mật khẩu phải nhiều hơn 6 ký tự")
            },
            isError = password.isNotEmpty() && password.length < 6,
            modifier = Modifier.fillMaxWidth()
        )

        if (!isLoginMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = reEnterPass,
                onValueChange = { reEnterPass = it.trim() },
                label = { Text("Nhập lại Mật khẩu") },
                singleLine = true,
                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { passVisible = !passVisible }) {
                        Icon(
                            painter = if (passVisible) painterResource(R.drawable.visibility) else painterResource(R.drawable.visible_off),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                supportingText = {
                    if (reEnterPass.isNotEmpty() && password != reEnterPass) Text("Mật khẩu không khớp")
                },
                isError = reEnterPass.isNotEmpty() && password != reEnterPass,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Quên mật khẩu?",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier
                    .clickable { onForgotPassword() }
                    .align(Alignment.End)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLoginMode) viewModel.signIn(email, password)
                else viewModel.register(email, password){
                    Toast.makeText(context, "${viewModel.errorMessage.value}", Toast.LENGTH_SHORT).show()
                    isLoginMode = true
                }
            },
            enabled = email.contains("@") && password.length >= 6 && (isLoginMode || password == reEnterPass),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoginMode) "Đăng nhập" else "Đăng ký")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBack) {
            Text("Trở lại")
        }
    }
}


