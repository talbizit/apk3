package com.iconfit.schedule.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iconfit.schedule.data.model.CachedSchedule
import com.iconfit.schedule.data.model.WeekSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iconfit_cache")

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val CACHE_VALIDITY_MS = 4 * 60 * 60 * 1000L // 4 hours
        private fun scheduleKey(clubId: String) = stringPreferencesKey("schedule_$clubId")
        private fun cacheTimeKey(clubId: String) = longPreferencesKey("cache_time_$clubId")
        private val SELECTED_CLUB_KEY = stringPreferencesKey("selected_club")
        private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
    }

    // Cache schedule for a club
    suspend fun cacheSchedule(clubId: String, schedule: WeekSchedule) {
        context.dataStore.edit { prefs ->
            prefs[scheduleKey(clubId)] = json.encodeToString(schedule)
            prefs[cacheTimeKey(clubId)] = System.currentTimeMillis()
        }
    }

    // Get cached schedule with staleness info
    suspend fun getCachedSchedule(clubId: String): CachedSchedule? {
        val prefs = context.dataStore.data.first()
        val scheduleJson = prefs[scheduleKey(clubId)] ?: return null
        val cacheTime = prefs[cacheTimeKey(clubId)] ?: return null

        return try {
            val schedule = json.decodeFromString<WeekSchedule>(scheduleJson)
            val isStale = System.currentTimeMillis() - cacheTime > CACHE_VALIDITY_MS
            CachedSchedule(schedule, cacheTime, isStale)
        } catch (e: Exception) {
            null
        }
    }

    // Check if cache is valid (not stale)
    suspend fun isCacheValid(clubId: String): Boolean {
        val prefs = context.dataStore.data.first()
        val cacheTime = prefs[cacheTimeKey(clubId)] ?: return false
        return System.currentTimeMillis() - cacheTime <= CACHE_VALIDITY_MS
    }

    // Get cache age in milliseconds
    suspend fun getCacheAge(clubId: String): Long? {
        val prefs = context.dataStore.data.first()
        val cacheTime = prefs[cacheTimeKey(clubId)] ?: return null
        return System.currentTimeMillis() - cacheTime
    }

    // Clear cache for a specific club
    suspend fun clearCache(clubId: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(scheduleKey(clubId))
            prefs.remove(cacheTimeKey(clubId))
        }
    }

    // Clear all cache
    suspend fun clearAllCache() {
        context.dataStore.edit { it.clear() }
    }

    // Save selected club
    suspend fun saveSelectedClub(clubId: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_CLUB_KEY] = clubId
        }
    }

    // Get selected club
    fun getSelectedClubFlow(): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[SELECTED_CLUB_KEY]
        }
    }

    suspend fun getSelectedClub(): String? {
        return context.dataStore.data.first()[SELECTED_CLUB_KEY]
    }

    // Save favorites
    suspend fun saveFavorites(favorites: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[FAVORITES_KEY] = favorites
        }
    }

    // Get favorites flow
    fun getFavoritesFlow(): Flow<Set<String>> {
        return context.dataStore.data.map { prefs ->
            prefs[FAVORITES_KEY] ?: emptySet()
        }
    }

    suspend fun getFavorites(): Set<String> {
        return context.dataStore.data.first()[FAVORITES_KEY] ?: emptySet()
    }

    // Toggle favorite
    suspend fun toggleFavorite(classId: String): Boolean {
        val current = getFavorites().toMutableSet()
        val isNowFavorite = if (current.contains(classId)) {
            current.remove(classId)
            false
        } else {
            current.add(classId)
            true
        }
        saveFavorites(current)
        return isNowFavorite
    }
}
