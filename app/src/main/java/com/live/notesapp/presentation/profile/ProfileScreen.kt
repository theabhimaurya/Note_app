package com.live.notesapp.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(
    onQRCode: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onAddPin: () -> Unit = {},
    onSettings: () -> Unit = {},
    onInviteFriend: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        uiState = uiState,
        onQRCode = onQRCode,
        onEditProfile = onEditProfile,
        onAddPin = onAddPin,
        onSettings = onSettings,
        onInviteFriend = onInviteFriend,
        onLogout = { viewModel.showLogoutDialog() }
    )

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideLogoutDialog() },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout and clear local data?") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout(onLogout) }) {
                    Text("Logout", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideLogoutDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onQRCode: () -> Unit,
    onEditProfile: () -> Unit,
    onAddPin: () -> Unit,
    onSettings: () -> Unit,
    onInviteFriend: () -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FB))
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Profile Image Section
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.name.take(1).uppercase(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // User Name
        Text(
            text = uiState.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email Pill
        Surface(
            color = Color(0xFFD7E9F7),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = uiState.email,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color(0xFF2C86C5),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Menu Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ProfileMenuItem(
                    icon = Icons.Default.QrCode2,
                    title = "My QR Code",
                    onClick = onQRCode
                )
                MenuDivider()
                ProfileMenuItem(
                    icon = Icons.Default.Edit,
                    title = "Edit Profile",
                    onClick = onEditProfile
                )
                MenuDivider()
                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    title = "Add Pin",
                    onClick = onAddPin
                )
                MenuDivider()
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    onClick = onSettings
                )
                MenuDivider()
                ProfileMenuItem(
                    icon = Icons.Default.GroupAdd,
                    title = "Invite a friend",
                    onClick = onInviteFriend
                )
                MenuDivider()
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Logout",
                    titleColor = Color(0xFFE57373),
                    showChevron = false,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = Color.LightGray.copy(alpha = 0.3f)
    )
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = Color.DarkGray,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == Color(0xFFE57373)) titleColor else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileContent(
            uiState = ProfileUiState(
                name = "Abhi",
                email = "abhi@gmail.com"
            ),
            onQRCode = {},
            onEditProfile = {},
            onAddPin = {},
            onSettings = {},
            onInviteFriend = {},
            onLogout = {}
        )
    }
}
