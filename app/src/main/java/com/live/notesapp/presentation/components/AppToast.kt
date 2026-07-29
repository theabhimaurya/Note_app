package com.live.notesapp.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.swapnil.customtoast.CustomToast

@Composable
fun AppToast(
    message: String?,
    isError: Boolean = false,
    onDismiss: () -> Unit
) {
    if (message != null) {
        CustomToast(
            message = message,
            visibility = true,
            onDismiss = onDismiss,
            durationMillis = 3000L,
            progressBarColor = if (isError) Color.Red else Color.Green,
            imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
            alignment = Alignment.BottomCenter,
            backgroundColor = Color.White,
            borderColor = if (isError) Color.Red else Color.Green,
            textColor = Color.Black,
            modifier = Modifier.padding(16.dp)
        )
    }
}
