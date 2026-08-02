package com.live.notesapp.presentation.call

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IncomingCallScreen(
    callerName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1E2C), Color(0xFF121212))
                )
            )
            .padding(32.dp)
    ) {
        // Caller Info Header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFFC0CA33),
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = callerName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Incoming Video Call...",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Action Buttons (Accept / Decline)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decline Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = onReject,
                    shape = CircleShape,
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Decline",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Decline", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }

            // Accept Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = onAccept,
                    shape = CircleShape,
                    containerColor = Color(0xFF43A047),
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Accept",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Accept", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}
