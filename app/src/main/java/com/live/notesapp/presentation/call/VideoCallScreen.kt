package com.live.notesapp.presentation.call

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.getstream.webrtc.android.compose.FloatingVideoRenderer
import io.getstream.webrtc.android.compose.VideoRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import org.webrtc.RendererCommon

@Composable
fun VideoCallScreen(
    otherUserId: String,
    roomId: String,
    isCaller: Boolean = false,
    onBack: () -> Unit,
    viewModel: VideoCallViewModel = hiltViewModel()
) {
    val sessionManager by viewModel.sessionManager.collectAsState()
    val otherUser by viewModel.otherUser.collectAsState()
    val localVideoTrack by remember(sessionManager) {
        sessionManager?.localVideoTrack ?: MutableStateFlow(null)
    }.collectAsState()
    val remoteVideoTrack by remember(sessionManager) {
        sessionManager?.remoteVideoTrack ?: MutableStateFlow(null)
    }.collectAsState()
    val callState by remember(sessionManager) {
        sessionManager?.callState ?: MutableStateFlow(com.live.notesapp.webrtc.WebRtcSessionManager.CallState.IDLE)
    }.collectAsState()

    var callDurationSeconds by remember { mutableIntStateOf(0) }
    var parentBounds by remember { mutableStateOf(IntSize(0, 0)) }
    
    LaunchedEffect(callState) {
        if (callState == com.live.notesapp.webrtc.WebRtcSessionManager.CallState.CONNECTED) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                callDurationSeconds++
            }
        } else if (callState == com.live.notesapp.webrtc.WebRtcSessionManager.CallState.ENDED) {
            kotlinx.coroutines.delay(1500)
            onBack()
        } else {
            callDurationSeconds = 0
        }
    }

    val formattedTime = remember(callDurationSeconds, callState) {
        when (callState) {
            com.live.notesapp.webrtc.WebRtcSessionManager.CallState.CALLING -> "Calling..."
            com.live.notesapp.webrtc.WebRtcSessionManager.CallState.RINGING -> "Ringing..."
            com.live.notesapp.webrtc.WebRtcSessionManager.CallState.CONNECTED -> {
                val minutes = callDurationSeconds / 60
                val seconds = callDurationSeconds % 60
                String.format("%02d:%02d", minutes, seconds)
            }
            com.live.notesapp.webrtc.WebRtcSessionManager.CallState.ENDED -> "Call Ended"
            com.live.notesapp.webrtc.WebRtcSessionManager.CallState.IDLE -> "Connecting..."
        }
    }

    var isMuted by remember { mutableStateOf(false) }
    var isCameraOn by remember { mutableStateOf(true) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    val rendererEvents = remember {
        object : RendererCommon.RendererEvents {
            override fun onFirstFrameRendered() {}
            override fun onFrameResolutionChanged(width: Int, height: Int, rotation: Int) {}
        }
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            viewModel.initializeSession()
        }
    }

    LaunchedEffect(sessionManager) {
        if (sessionManager != null && (isCaller || viewModel.isCaller)) {
            viewModel.startCall()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1C1E))
            .onSizeChanged { parentBounds = it }
    ) {
        // Remote Video (Fullscreen)
        val eglContext = sessionManager?.eglContext
        if (remoteVideoTrack != null && eglContext != null) {
            VideoRenderer(
                modifier = Modifier.fillMaxSize(),
                videoTrack = remoteVideoTrack!!,
                eglBaseContext = eglContext,
                rendererEvents = rendererEvents
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Waiting for connection...", color = Color.White)
            }
        }

        // Top Info
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = otherUser?.name ?: "User",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formattedTime,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        // Local Video (Floating & Draggable)
        if (localVideoTrack != null && eglContext != null) {
            FloatingVideoRenderer(
                modifier = Modifier
                    .size(120.dp, 180.dp)
                    .clip(RoundedCornerShape(16.dp)),
                videoTrack = localVideoTrack!!,
                parentBounds = parentBounds,
                paddingValues = PaddingValues(16.dp),
                eglBaseContext = eglContext,
                rendererEvents = rendererEvents
            )
        }

        // Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isMuted = !isMuted
                    viewModel.setMicEnabled(!isMuted)
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    isCameraOn = !isCameraOn
                    viewModel.setCameraEnabled(isCameraOn)
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Camera",
                    tint = Color.White
                )
            }

            FloatingActionButton(
                onClick = { 
                    viewModel.endCall()
                    onBack()
                },
                containerColor = Color.Red,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "End Call")
            }
        }
    }
}
