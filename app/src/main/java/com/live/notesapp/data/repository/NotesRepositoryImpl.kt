package com.live.notesapp.data.repository

import com.live.notesapp.data.model.dto.NoteDto
import com.live.notesapp.data.model.dto.toDomain
import com.live.notesapp.data.model.dto.toDto
import com.live.notesapp.domain.model.Note
import com.live.notesapp.domain.repository.NotesRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

class NotesRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest
) : NotesRepository {

    private val table = postgrest["notes"]

    override suspend fun getNotes(): Result<List<Note>> {
        return try {
            val result = table.select().decodeList<NoteDto>()
            Result.success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addNote(note: Note): Result<Unit> {
        return try {
            table.insert(note.toDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNote(note: Note): Result<Unit> {
        return try {
            table.update(note.toDto()) {
                filter {
                    eq("id", note.id ?: "")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        return try {
            table.delete {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchNotes(query: String): Result<List<Note>> {
        return try {
            val result = table.select {
                filter {
                    or {
                        ilike("title", "%$query%")
                        ilike("description", "%$query%")
                    }
                }
            }.decodeList<NoteDto>()
            Result.success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}