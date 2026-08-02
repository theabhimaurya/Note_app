package com.live.notesapp.presentation.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.presentation.login.LoginScreen
import com.live.notesapp.presentation.login.LoginViewModel
import com.live.notesapp.presentation.notes.AddEditNoteScreen
import com.live.notesapp.presentation.notes.AddEditNoteViewModel
import com.live.notesapp.presentation.notes.NotesScreen
import com.live.notesapp.presentation.notes.NotesViewModel
import com.live.notesapp.presentation.notes.ViewNoteScreen
import com.live.notesapp.presentation.notes.ViewNoteViewModel
import com.live.notesapp.presentation.settings.SettingsScreen
import com.live.notesapp.presentation.chat.ChatScreen
import com.live.notesapp.presentation.call.VideoCallScreen
import com.live.notesapp.presentation.profile.ProfileViewModel
import com.live.notesapp.presentation.profile.QRCodeScreen
import com.live.notesapp.presentation.profile.QRScannerScreen
import com.live.notesapp.presentation.signup.SignupScreen
import com.live.notesapp.presentation.signup.SignupViewModel
import com.live.notesapp.presentation.splash.SplashScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                authRepository = authRepository,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToNotes = {
                    navController.navigate(Screen.Root.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                onLoginSuccess = { navController.navigate(Screen.Root.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }}
            )
        }
        composable(Screen.Signup.route) {
            val viewModel: SignupViewModel = hiltViewModel()
            SignupScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onSignupSuccess = { navController.navigate(Screen.Root.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }}
            )
        }
        composable(Screen.Root.route) {
            MainScreen(
                rootNavController = navController,
                authRepository = authRepository
            )
        }
        composable(Screen.ViewNote.route) {
            ViewNoteScreen(
                onEdit = { noteId ->
                    navController.navigate(Screen.AddEditNote.passNoteId(noteId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddEditNote.route) {
            val viewModel: AddEditNoteViewModel = hiltViewModel()
            AddEditNoteScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Root.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.QRCode.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            QRCodeScreen(
                onScan = { navController.navigate(Screen.ScanQR.route) },
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
        composable(Screen.ScanQR.route) {
            QRScannerScreen(
                onScanSuccess = { result ->
                    // result is the scanned UID
                    navController.navigate(Screen.Chat.passUserId(result)) {
                        popUpTo(Screen.ScanQR.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Chat.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            ChatScreen(
                otherUserId = userId,
                onBack = { navController.popBackStack() },
                onNavigateToCall = { otherId, roomId, isCaller ->
                    navController.navigate(Screen.VideoCall.passArgs(otherId, roomId, isCaller))
                }
            )
        }
        composable(Screen.VideoCall.route) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: return@composable
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val isCaller = backStackEntry.arguments?.getString("isCaller")?.toBooleanStrictOrNull() ?: false
            VideoCallScreen(
                otherUserId = otherUserId,
                roomId = roomId,
                isCaller = isCaller,
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(if (authRepository.isUserLoggedIn()) Screen.Root.route else Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
