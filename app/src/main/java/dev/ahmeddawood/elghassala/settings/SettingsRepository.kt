package dev.ahmeddawood.elghassala.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "elghassala_settings")

enum class AppLanguage { SYSTEM, ARABIC, ENGLISH }
enum class AppTheme { SYSTEM, LIGHT, DARK }

class SettingsRepository(private val context: Context) {

    private object Keys {
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val funEnabled = booleanPreferencesKey("fun_enabled")
        val funFrequency = stringPreferencesKey("fun_frequency")
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map {
        when (it[Keys.language]) {
            "ar" -> AppLanguage.ARABIC
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    val theme: Flow<AppTheme> = context.dataStore.data.map {
        when (it[Keys.theme]) {
            "light" -> AppTheme.LIGHT
            "dark" -> AppTheme.DARK
            else -> AppTheme.SYSTEM
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit {
            it[Keys.language] = when (language) {
                AppLanguage.ARABIC -> "ar"
                AppLanguage.ENGLISH -> "en"
                AppLanguage.SYSTEM -> "system"
            }
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit {
            it[Keys.theme] = when (theme) {
                AppTheme.LIGHT -> "light"
                AppTheme.DARK -> "dark"
                AppTheme.SYSTEM -> "system"
            }
        }
    }

    /** Playful feedback messages: on/off and how often (often / balanced / rare). */
    val funEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.funEnabled] ?: true }
    val funFrequency: Flow<String> = context.dataStore.data.map { it[Keys.funFrequency] ?: "balanced" }

    suspend fun setFunEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.funEnabled] = value }
    }

    suspend fun setFunFrequency(value: String) {
        context.dataStore.edit { it[Keys.funFrequency] = value }
    }
}
