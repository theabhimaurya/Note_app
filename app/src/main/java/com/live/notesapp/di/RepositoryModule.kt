package com.live.notesapp.di

import com.live.notesapp.data.repository.AuthRepositoryImpl
import com.live.notesapp.data.repository.NotesRepositoryImpl
import com.live.notesapp.data.repository.TextRecognitionRepositoryImpl
import com.live.notesapp.data.repository.ThemeRepositoryImpl
import com.live.notesapp.domain.repository.AuthRepository
import com.live.notesapp.domain.repository.NotesRepository
import com.live.notesapp.domain.repository.TextRecognitionRepository
import com.live.notesapp.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNotesRepository(
        notesRepositoryImpl: NotesRepositoryImpl
    ): NotesRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindTextRecognitionRepository(
        textRecognitionRepositoryImpl: TextRecognitionRepositoryImpl
    ): TextRecognitionRepository
}
