package com.live.notesapp.domain.repository

import com.live.notesapp.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
    suspend fun clear()
}
