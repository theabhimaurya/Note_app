package com.live.notesapp.di

import android.util.Log
import com.live.notesapp.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.util.AttributeKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val startTimeKey = AttributeKey<Long>("StartTime")

    @OptIn(SupabaseInternal::class)
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        // Use full path for BuildConfig to avoid unresolved reference until next build
        val isDebug = com.live.notesapp.BuildConfig.DEBUG

        return createSupabaseClient(
            supabaseUrl = Constants.SUPABASE_URL,
            supabaseKey = Constants.SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)

            httpConfig {
                if (isDebug) {
                    install(Logging) {
                        logger = object : Logger {
                            override fun log(message: String) {
                                // Mask common sensitive fields in JSON bodies
                                val sanitizedMessage = message
                                    .replace(Regex("(\"password\"\\s*:\\s*\")[^\"]*(\")"), "$1***$2")
                                    .replace(Regex("(\"refresh_token\"\\s*:\\s*\")[^\"]*(\")"), "$1***$2")
                                    .replace(Regex("(\"access_token\"\\s*:\\s*\")[^\"]*(\")"), "$1***$2")

                                when {
                                    sanitizedMessage.startsWith("REQUEST") -> {
                                        Log.d("Supabase", "==================================================")
                                        Log.d("Supabase", "SUPABASE REQUEST")
                                        Log.d("Supabase", "==================================================")
                                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                        Log.d("Supabase", "Time        : $timestamp")
                                        Log.d("Supabase", sanitizedMessage)
                                    }
                                    sanitizedMessage.startsWith("RESPONSE") -> {
                                        Log.d("Supabase", "==================================================")
                                        Log.d("Supabase", "SUPABASE RESPONSE")
                                        Log.d("Supabase", "==================================================")
                                        Log.d("Supabase", sanitizedMessage)
                                    }
                                    else -> {
                                        if (sanitizedMessage.isNotBlank()) {
                                            Log.d("Supabase", sanitizedMessage)
                                        }
                                    }
                                }
                            }
                        }
                        level = LogLevel.ALL
                        sanitizeHeader { header ->
                            header.equals("Authorization", ignoreCase = true) || 
                            header.equals("apikey", ignoreCase = true)
                        }
                    }

                    // Custom Plugin to measure and log execution time (Duration)
                    val durationPlugin = createClientPlugin("SupabaseDurationPlugin") {
                        onRequest { request, _ ->
                            request.attributes.put(startTimeKey, System.currentTimeMillis())
                        }
                        onResponse { response ->
                            val startTime = response.call.attributes.getOrNull(startTimeKey)
                            if (startTime != null) {
                                val duration = System.currentTimeMillis() - startTime
                                Log.d("Supabase", "Duration    : $duration ms")
                                Log.d("Supabase", "==================================================")
                            }
                        }
                    }
                    install(durationPlugin)
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth {
        return client.auth
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: SupabaseClient): Postgrest {
        return client.postgrest
    }
}
