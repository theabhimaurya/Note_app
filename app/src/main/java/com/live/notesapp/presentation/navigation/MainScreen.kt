package com.live.notesapp.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.presentation.notes.AddEditNoteViewModel
import com.live.notesapp.presentation.notes.NotesScreen
import com.live.notesapp.presentation.notes.NotesViewModel
import com.live.notesapp.presentation.chat.ChatListScreen
import com.live.notesapp.presentation.profile.ProfileScreen

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    authRepository: AuthRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val addEditViewModel: AddEditNoteViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                onFabClick = {
                    rootNavController.navigate(Screen.AddEditNote.passNoteId())
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ChatList.route,
            modifier = Modifier.padding(bottom = 62.dp)
        ) {
            composable(Screen.ChatList.route) {
                ChatListScreen(
                    onChatClick = { userId ->
                        rootNavController.navigate(Screen.Chat.passUserId(userId))
                    }
                )
            }
            composable(Screen.Home.route) {
                PlaceholderScreen("Home")
            }
            composable(Screen.Notes.route) {
                val viewModel: NotesViewModel = hiltViewModel()
                NotesScreen(
                    viewModel = viewModel,
                    onNavigateToViewNote = { noteId ->
                        rootNavController.navigate(Screen.ViewNote.passNoteId(noteId))
                    }
                )
            }
            composable(Screen.AI.route) {
                PlaceholderScreen("AI")
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onQRCode = {
                        rootNavController.navigate(Screen.QRCode.route)
                    },
                    onSettings = {
                        rootNavController.navigate(Screen.Settings.route)
                    },
                    onLogout = {
                        rootNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Root.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name Screen")
    }
}
