package com.live.notesapp.domain.repository

import com.live.notesapp.domain.model.Note

interface NotesRepository {
    suspend fun getNotes(): Result<List<Note>>
    suspend fun addNote(note: Note): Result<Unit>
    suspend fun updateNote(note: Note): Result<Unit>
    suspend fun deleteNote(id: String): Result<Unit>
    suspend fun searchNotes(query: String): Result<List<Note>>
}