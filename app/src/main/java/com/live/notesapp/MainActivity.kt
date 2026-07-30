package com.live.notesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.presentation.MainViewModel
import com.live.notesapp.presentation.navigation.NavGraph
import com.live.notesapp.ui.theme.NotesAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.themeState.collectAsStateWithLifecycle()

            NotesAppTheme(appTheme = appTheme) {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    authRepository = authRepository
                )
            }
        }
    }
}
