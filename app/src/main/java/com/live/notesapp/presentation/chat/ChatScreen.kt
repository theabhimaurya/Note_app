package com.live.notesapp.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.live.notesapp.domain.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUserId: String,
    onBack: () -> Unit,
    onNavigateToCall: (String, String, Boolean) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val otherUser by viewModel.otherUser
    val currentUserId by viewModel.currentUserId
    val currentRoomId by viewModel.currentRoomId
    val incomingCall by viewModel.incomingCall
    val error by viewModel.error
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // If user is already in the ChatScreen with the caller, auto-start the video call directly without requiring manual accept
    LaunchedEffect(incomingCall, currentRoomId) {
        val call = incomingCall
        val roomId = currentRoomId
        if (call != null && roomId != null) {
            viewModel.dismissCall()
            onNavigateToCall(call.senderId, roomId, false)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(otherUserId) {
        viewModel.initChat(otherUserId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val displayName = otherUser?.name?.takeIf { it.isNotBlank() } ?: "User"
                            Text(
                                text = displayName.trim().take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = otherUser?.name ?: "User",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Online",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Green
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        currentRoomId?.let { roomId ->
                            viewModel.initiateCall(otherUserId, roomId)
                            onNavigateToCall(otherUserId, roomId, true)
                        }
                    }) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Video Call")
                    }
                    IconButton(onClick = { /* Call */ }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call")
                    }
                    IconButton(onClick = { /* More */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        placeholder = { Text("Type messages...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC0CA33),
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        shape = CircleShape,
                        containerColor = Color(0xFFC0CA33),
                        modifier = Modifier.size(48.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isMine = message.senderId == currentUserId
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMine: Boolean) {
    val bubbleColor = if (isMine) Color(0xFF1A1C1E) else Color(0xFFF1F0F0)
    val textColor = if (isMine) Color.White else Color.Black
    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.content,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 14.sp
            )
        }
        Text(
            text = formatTime(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
