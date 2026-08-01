package com.live.notesapp.presentation.notes

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.live.notesapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewNoteScreen(
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewNoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ViewNoteUiEffect.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(effect.text))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, context.getString(R.string.note_copied), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.note?.let { note ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Pin Button
                            ActionButton(
                                icon = Icons.Default.PushPin,
                                color = Color(0xFFB07D4E),
                                onClick = { /* TODO */ }
                            )
                            // Edit Button
                            ActionButton(
                                icon = Icons.Default.Edit,
                                color = Color(0xFF3F51B5),
                                onClick = { note.id?.let { onEdit(it) } }
                            )
                            // Search Button
                            ActionButton(
                                icon = Icons.Default.Search,
                                color = Color(0xFF66BB6A),
                                onClick = { /* TODO */ }
                            )
                            // Copy Button
                            ActionButton(
                                icon = Icons.Default.ContentCopy,
                                color = Color(0xFF7E57C2),
                                onClick = { viewModel.onEvent(ViewNoteUiEvent.CopyTextClicked(note)) }
                            )
                        }

                        // Delete Button
                        ActionButton(
                            icon = Icons.Default.Delete,
                            color = Color(0xFFFF5252),
                            onClick = { viewModel.onEvent(ViewNoteUiEvent.DeleteClicked) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Category Label
                    Surface(
                        color = Color(0xFFFDF9E3),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "Notes",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF8B7E58)
                        )
                    }

                    // Title
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date
                    Text(
                        text = note.createdAt?.take(10) ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Content
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                    )
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ViewNoteUiEvent.DismissDeleteDialog) },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(ViewNoteUiEvent.ConfirmDeleteClicked) }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(ViewNoteUiEvent.DismissDeleteDialog) }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
