package com.live.notesapp.utils

import io.github.jan.supabase.exceptions.RestException

fun Throwable.getSupabaseErrorMessage(): String {
    return when (this) {
        is RestException -> {
            val rawMessage = this.message ?: "An unknown error occurred"
            
            // Pattern: code (message: code)
            val regex = Regex("""\((.*):.*\)""")
            val match = regex.find(rawMessage)
            if (match != null) {
                match.groupValues[1].trim()
            } else if (rawMessage.contains("(") && rawMessage.contains(")")) {
                rawMessage.substringAfter("(").substringBefore(")").trim()
            } else {
                rawMessage.substringBefore("\n")
            }
        }
        else -> this.message ?: "An unknown error occurred"
    }
}
