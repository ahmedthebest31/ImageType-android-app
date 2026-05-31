package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "imagetype_preferences")

class PreferencesManager(private val context: Context) {
    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
        val KEY_TEMPLATES = stringPreferencesKey("custom_templates")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: "Dark"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "en"
    }

    val hapticsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HAPTICS] ?: true
    }

    val templatesFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_TEMPLATES] ?: "[]"
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = theme
        }
    }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }

    suspend fun saveHaptics(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAPTICS] = enabled
        }
    }

    suspend fun saveTemplates(templatesJson: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TEMPLATES] = templatesJson
        }
    }
}
