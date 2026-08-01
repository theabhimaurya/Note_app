package com.live.notesapp.presentation.notes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.live.notesapp.domain.model.Note
import com.live.notesapp.domain.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewNoteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ViewNoteViewModel
    private lateinit var notesRepository: NotesRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        notesRepository = FakeNotesRepository()
        savedStateHandle = SavedStateHandle()
        viewModel = ViewNoteViewModel(notesRepository, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when CopyTextClicked event is received, emit CopyToClipboard effect with formatted text`() = runTest {
        val note = Note(
            id = "1",
            title = "Test Title",
            description = "Test Description"
        )
        val expectedFormattedText = "Test Title\n\nTest Description"

        viewModel.uiEffect.test {
            viewModel.onEvent(ViewNoteUiEvent.CopyTextClicked(note))
            
            val effect = awaitItem()
            assert(effect is ViewNoteUiEffect.CopyToClipboard)
            assertEquals(expectedFormattedText, (effect as ViewNoteUiEffect.CopyToClipboard).text)
        }
    }
}

class FakeNotesRepository : NotesRepository {
    override suspend fun getNotes(): Result<List<Note>> = Result.success(emptyList())
    override suspend fun addNote(note: Note): Result<Unit> = Result.success(Unit)
    override suspend fun updateNote(note: Note): Result<Unit> = Result.success(Unit)
    override suspend fun deleteNote(id: String): Result<Unit> = Result.success(Unit)
    override suspend fun searchNotes(query: String): Result<List<Note>> = Result.success(emptyList())
}
