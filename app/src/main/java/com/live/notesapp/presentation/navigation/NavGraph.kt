package com.live.notesapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.presentation.login.LoginScreen
import com.live.notesapp.presentation.login.LoginViewModel
import com.live.notesapp.presentation.notes.NotesScreen
import com.live.notesapp.presentation.notes.NotesViewModel
import com.live.notesapp.presentation.settings.SettingsScreen
import com.live.notesapp.presentation.signup.SignupScreen
import com.live.notesapp.presentation.signup.SignupViewModel
import com.live.notesapp.presentation.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
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
                    navController.navigate(Screen.Notes.route) {
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
                onLoginSuccess = { navController.navigate(Screen.Notes.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }}
            )
        }
        composable(Screen.Signup.route) {
            val viewModel: SignupViewModel = hiltViewModel()
            SignupScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onSignupSuccess = { navController.navigate(Screen.Notes.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }}
            )
        }
        composable(Screen.Notes.route) {
            val viewModel: NotesViewModel = hiltViewModel()
            NotesScreen(
                viewModel = viewModel,
                onLogout = { 
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Notes.route) { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
