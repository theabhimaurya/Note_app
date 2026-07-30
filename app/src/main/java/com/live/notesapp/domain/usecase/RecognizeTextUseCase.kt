package com.live.notesapp.domain.usecase

import android.net.Uri
import com.live.notesapp.domain.repository.TextRecognitionRepository
import javax.inject.Inject

class RecognizeTextUseCase @Inject constructor(
    private val repository: TextRecognitionRepository
) {
    suspend operator fun invoke(uri: Uri): Result<String> {
        return repository.recognizeTextFromImage(uri)
    }
}
