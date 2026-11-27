package com.afterhours.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("afterhours_prefs")

/** Local settings only. No account; nothing here leaves the device. */
data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val chaos: Boolean = true,
)

class Prefs(private val context: Context) {
    private val ageKey = booleanPreferencesKey("age_confirmed")
    private val tutorialKey = booleanPreferencesKey("tutorial_seen")
    private val namesKey = stringPreferencesKey("saved_names")
    private val seenKey = stringSetPreferencesKey("seen_cards")
    private val reportedKey = stringSetPreferencesKey("reported_cards")
    private val soundKey = booleanPreferencesKey("sound")
    private val hapticsKey = booleanPreferencesKey("haptics")
    private val chaosKey = booleanPreferencesKey("chaos")
    private val languageKey = stringPreferencesKey("card_language")
    private val adsRemovedKey = booleanPreferencesKey("ads_removed")

    val ageConfirmed: Flow<Boolean> = context.dataStore.data.map { it[ageKey] ?: false }
    val tutorialSeen: Flow<Boolean> = context.dataStore.data.map { it[tutorialKey] ?: false }
    val settings: Flow<Settings> = context.dataStore.data.map {
        Settings(sound = it[soundKey] ?: true, haptics = it[hapticsKey] ?: true, chaos = it[chaosKey] ?: true)
    }
    /** Cached result of the last Play Billing check, so ads stay off while offline. */
    val adsRemoved: Flow<Boolean> = context.dataStore.data.map { it[adsRemovedKey] ?: false }
    val reportedCards: Flow<Set<String>> = context.dataStore.data.map { it[reportedKey] ?: emptySet() }

    suspend fun setAgeConfirmed() = set(ageKey, true)
    suspend fun setTutorialSeen() = set(tutorialKey, true)
    suspend fun setSound(on: Boolean) = set(soundKey, on)
    suspend fun setHaptics(on: Boolean) = set(hapticsKey, on)
    suspend fun setChaos(on: Boolean) = set(chaosKey, on)
    suspend fun setAdsRemoved(on: Boolean) = set(adsRemovedKey, on)
    suspend fun setLanguage(code: String) = set(languageKey, code)
    suspend fun language(): String? = context.dataStore.data.first()[languageKey]

    suspend fun savedNames(): List<String> =
        context.dataStore.data.first()[namesKey]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    suspend fun saveNames(names: List<String>) = set(namesKey, names.joinToString("\n"))

    suspend fun seenCards(): Set<String> = context.dataStore.data.first()[seenKey] ?: emptySet()

    suspend fun addSeenCards(ids: Set<String>) {
        if (ids.isEmpty()) return
        context.dataStore.edit { it[seenKey] = (it[seenKey] ?: emptySet()) + ids }
    }

    suspend fun clearSeenCards() { context.dataStore.edit { it.remove(seenKey) } }

    suspend fun addReportedCard(id: String) {
        context.dataStore.edit { it[reportedKey] = (it[reportedKey] ?: emptySet()) + id }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}
