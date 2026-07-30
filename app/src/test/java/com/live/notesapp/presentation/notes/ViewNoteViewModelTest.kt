package com.live.notesapp.presentation.notes

import app.cash.turbine.test
import com.live.notesapp.domain.model.Note
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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ViewNoteViewModel()
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
