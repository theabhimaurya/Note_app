package com.live.notesapp.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Root : Screen("root")
    object Notes : Screen("notes")
    object Home : Screen("home")
    object AI : Screen("ai")
    object Profile : Screen("profile")
    object ChatList : Screen("chat_list")
    object QRCode : Screen("qrcode")
    object ScanQR : Screen("scanqr")
    object ViewNote : Screen("view_note/{noteId}") {
        fun passNoteId(noteId: String): String {
            return "view_note/$noteId"
        }
    }
    object AddEditNote : Screen("add_edit_note?noteId={noteId}") {
        fun passNoteId(noteId: String? = null): String {
            return noteId?.let { "add_edit_note?noteId=$it" } ?: "add_edit_note"
        }
    }
    object Chat : Screen("chat/{userId}") {
        fun passUserId(userId: String): String {
            return "chat/$userId"
        }
    }
    object VideoCall : Screen("video_call/{otherUserId}/{roomId}/{isCaller}") {
        fun passArgs(otherUserId: String, roomId: String, isCaller: Boolean): String {
            return "video_call/$otherUserId/$roomId/$isCaller"
        }
    }
    object Settings : Screen("settings")
}
