package com.live.notesapp

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.live.notesapp.domain.manager.TokenManager
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.presentation.MainViewModel
import com.live.notesapp.presentation.navigation.NavGraph
import com.live.notesapp.presentation.navigation.Screen
import com.live.notesapp.ui.theme.NotesAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var tokenManager: TokenManager

    private val viewModel: MainViewModel by viewModels()

    private var incomingCallData by mutableStateOf<Triple<String, String, Boolean>?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sync FCM token
        tokenManager.syncCurrentToken()

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        handleIntent(intent)

        val startDestination = incomingCallData?.let { (otherUserId, roomId, isCaller) ->
            incomingCallData = null
            Screen.VideoCall.passArgs(otherUserId, roomId, isCaller)
        } ?: Screen.Splash.route

        setContent {
            val appTheme by viewModel.themeState.collectAsStateWithLifecycle()

            NotesAppTheme(appTheme = appTheme) {
                val navController = rememberNavController()

                LaunchedEffect(incomingCallData) {
                    incomingCallData?.let { (otherUserId, roomId, isCaller) ->
                        navController.navigate(Screen.VideoCall.passArgs(otherUserId, roomId, isCaller))
                        incomingCallData = null
                    }
                }

                NavGraph(
                    navController = navController,
                    authRepository = authRepository,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("NAVIGATE_TO_CALL", false) == true) {
            val otherUserId = intent.getStringExtra("OTHER_USER_ID") ?: return
            val roomId = intent.getStringExtra("ROOM_ID") ?: return
            val isCaller = intent.getBooleanExtra("IS_CALLER", false)
            incomingCallData = Triple(otherUserId, roomId, isCaller)
        }
    }
}

