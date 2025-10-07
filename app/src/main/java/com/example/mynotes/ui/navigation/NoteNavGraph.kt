package com.example.mynotes.ui.navigation

import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mynotes.AppViewModelProvider
import com.example.mynotes.ui.Auth.AuthScreen
import com.example.mynotes.ui.Auth.AuthViewModel
import com.example.mynotes.ui.Auth.ForgotPasswordScreen
import com.example.mynotes.ui.edit.CoverScreen
import com.example.mynotes.ui.edit.NoteCoverDestination
import com.example.mynotes.ui.NoteAppTheme
import com.example.mynotes.ui.addNote.AddScreen
import com.example.mynotes.ui.changePassword.ChangePasswordScreen
import com.example.mynotes.ui.draw.DrawingScreen
import com.example.mynotes.ui.edit.EditScreen
import com.example.mynotes.ui.edit.EditViewModel
import com.example.mynotes.ui.edit.NoteEditDestination
import com.example.mynotes.ui.edit.NoteEditDestination.noteIdArg
import com.example.mynotes.ui.fileReader.FileReaderScreen
import com.example.mynotes.ui.fileReader.FileReaderViewModel
import com.example.mynotes.ui.home.HomeScreen
import com.example.mynotes.ui.search.SearchScreen
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.Auth

@RequiresApi(35)
@Composable
fun NoteNavHost(
    navController: NavHostController,
    activity: AppCompatActivity,
    callbackManager: CallbackManager,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
    NavHost(
        navController = navController,
        startDestination = "Home",
        modifier = modifier
    ) {
        composable(route = "Home") {
            NoteAppTheme {
                HomeScreen(
                    activity = activity,
                    navigateToItemEntry = { navController.navigate("AddScreen") },
                    navigateToEdit = { navController.navigate("EditScreen/${it}") },
                    navigateToSearch = { navController.navigate("SearchScreen") },
                    logout = { authViewModel.signOut() },
                    login = { navController.navigate("AuthScreen") },
                    navigateToFileReader = { navController.navigate("FileReader") },
                    navigateToChangePassword = {navController.navigate("ChangePassword")}
                )
            }

        }
        composable(route = "AddScreen") {
            NoteAppTheme {
                AddScreen(
                    navigateBack = { navController.popBackStack() },
                )
            }
        }
        composable(
            route = "EditScreen/{$noteIdArg}",
            arguments = listOf(navArgument(NoteEditDestination.noteIdArg) {
                type = NavType.IntType
            })
        ) {
            NoteAppTheme {
                EditScreen(
                    navigateBack = { navController.popBackStack() },
                    navigateToCover = { navController.navigate("CoverScreen/${it}") },
                )
            }
        }
        composable(route = "SearchScreen") {
            NoteAppTheme {
                SearchScreen(
                    navigateBack = { navController.popBackStack() },
                    navigateToEdit = { navController.navigate("EditScreen/${it}") }
                )
            }
        }
        composable(
            route = "CoverScreen/{$noteIdArg}",
            arguments = listOf(navArgument(NoteCoverDestination.noteIdArg) {
                type = NavType.IntType
            })
        ) { backStackEntry ->
            CoverScreen(
                navigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "AuthScreen",
        ) {
            AuthScreen(
                navigateBack = { navController.popBackStack() },
                navigateToForgotPassword = { navController.navigate("ForgotPasswordScreen") },
                callbackManager = callbackManager
            )
        }
        composable(
            route = "ForgotPasswordScreen",
        ) {
            ForgotPasswordScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "DrawingScreen"
        ) {
            DrawingScreen(
                navigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "FileReader"
        ) {
            FileReaderScreen(
                navigateToDrawing = { drawingId ->
                    if (drawingId != null) {
                        navController.navigate("drawing/$drawingId")
                    } else {
                        navController.navigate("DrawingScreen")
                    }
                },
                navigateBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(
            "drawing/{drawingId}",
            arguments = listOf(navArgument("drawingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val drawingId = backStackEntry.arguments?.getLong("drawingId")

            DrawingScreen(
                drawingId = drawingId,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            "ChangePassword",
        ) {
            ChangePasswordScreen(
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
