package com.live.notesapp.data.repository

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.live.notesapp.domain.repository.TextRecognitionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextRecognitionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TextRecognitionRepository {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeTextFromImage(uri: Uri): Result<String> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            if (result.text.isBlank()) {
                Result.failure(Exception("No text found in image"))
            } else {
                Result.success(result.text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
