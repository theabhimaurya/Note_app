package com.live.notesapp.domain.repository

import android.net.Uri

interface TextRecognitionRepository {
    suspend fun recognizeTextFromImage(uri: Uri): Result<String>
}
