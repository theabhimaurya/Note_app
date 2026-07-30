package com.live.notesapp.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Notes : Screen("notes")
    object AddEditNote : Screen("add_edit_note?noteId={noteId}") {
        fun passNoteId(noteId: String? = null): String {
            return noteId?.let { "add_edit_note?noteId=$it" } ?: "add_edit_note"
        }
    }
    object Settings : Screen("settings")
}
