package com.afterhours.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("afterhours_prefs")

/** Stores only the local 18+ confirmation. No account, no personal data. */
class Prefs(private val context: Context) {
    private val ageKey = booleanPreferencesKey("age_confirmed")

    val ageConfirmed: Flow<Boolean> = context.dataStore.data.map { it[ageKey] ?: false }

    suspend fun setAgeConfirmed() {
        context.dataStore.edit { it[ageKey] = true }
    }
}
