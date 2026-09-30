package dev.uniflow.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("settings")
data class Settings(val name: String = "Student", val theme: String = "system", val language: String = "system", val reminders: Boolean = false)
class Preferences(context: Context) {
    private val store = context.settingsStore
    private val name = stringPreferencesKey("name")
    private val theme = stringPreferencesKey("theme")
    private val language = stringPreferencesKey("language")
    private val reminders = booleanPreferencesKey("reminders")
    private val seeded = booleanPreferencesKey("seeded")
    val settings = store.data.map { Settings(it[name] ?: "Student", it[theme] ?: "system", it[language] ?: "system", it[reminders] ?: false) }
    val initialized = store.data.map { it[seeded] ?: false }
    suspend fun save(value: Settings) { store.edit { it[name] = value.name; it[theme] = value.theme; it[language] = value.language; it[reminders] = value.reminders } }
    suspend fun initialized() { store.edit { it[seeded] = true } }
}
