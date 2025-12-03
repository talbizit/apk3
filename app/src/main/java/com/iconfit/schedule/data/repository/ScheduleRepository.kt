package com.iconfit.schedule.data.repository

import com.iconfit.schedule.data.ClubsData
import com.iconfit.schedule.data.local.CacheManager
import com.iconfit.schedule.data.model.CachedSchedule
import com.iconfit.schedule.data.model.Club
import com.iconfit.schedule.data.model.WeekSchedule
import com.iconfit.schedule.data.remote.ScheduleFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class ScheduleResult {
    data class Fresh(val schedule: WeekSchedule) : ScheduleResult()
    data class Cached(val schedule: WeekSchedule, val isStale: Boolean, val cachedAt: Long) : ScheduleResult()
    data class RefreshingInBackground(val schedule: WeekSchedule, val cachedAt: Long) : ScheduleResult()
    data class Refreshed(val schedule: WeekSchedule) : ScheduleResult()
    data class Error(val message: String, val cachedSchedule: WeekSchedule?) : ScheduleResult()
}

@Singleton
class ScheduleRepository @Inject constructor(
    private val cacheManager: CacheManager,
    private val scheduleFetcher: ScheduleFetcher
) {
    /**
     * Get schedule with stale-while-revalidate strategy:
     * 1. Return cached data immediately if available (even if stale)
     * 2. If stale, refresh in background and emit new data when ready
     * 3. If no cache, fetch fresh data
     */
    fun getSchedule(club: Club, forceRefresh: Boolean = false): Flow<ScheduleResult> = flow {
        // Check cache first
        val cached = cacheManager.getCachedSchedule(club.id)

        if (cached != null && !forceRefresh) {
            if (cached.isStale) {
                // Return stale data immediately, then refresh
                emit(ScheduleResult.RefreshingInBackground(cached.schedule, cached.cachedAt))

                // Fetch fresh data
                val result = scheduleFetcher.fetchSchedule(club)
                result.fold(
                    onSuccess = { freshSchedule ->
                        cacheManager.cacheSchedule(club.id, freshSchedule)
                        emit(ScheduleResult.Refreshed(freshSchedule))
                    },
                    onFailure = {
                        // Keep using stale data
                        emit(ScheduleResult.Error(it.message ?: "שגיאה בטעינת הנתונים", cached.schedule))
                    }
                )
            } else {
                // Cache is valid
                emit(ScheduleResult.Cached(cached.schedule, isStale = false, cached.cachedAt))
            }
        } else {
            // No cache or force refresh - fetch fresh data
            val result = scheduleFetcher.fetchSchedule(club)
            result.fold(
                onSuccess = { freshSchedule ->
                    cacheManager.cacheSchedule(club.id, freshSchedule)
                    emit(ScheduleResult.Fresh(freshSchedule))
                },
                onFailure = {
                    // Return cached data if available, even if stale
                    if (cached != null) {
                        emit(ScheduleResult.Error(it.message ?: "שגיאה בטעינת הנתונים", cached.schedule))
                    } else {
                        emit(ScheduleResult.Error(it.message ?: "שגיאה בטעינת הנתונים", null))
                    }
                }
            )
        }
    }

    /**
     * Force refresh schedule
     */
    suspend fun refreshSchedule(club: Club): Result<WeekSchedule> {
        return scheduleFetcher.fetchSchedule(club).also { result ->
            result.onSuccess { schedule ->
                cacheManager.cacheSchedule(club.id, schedule)
            }
        }
    }

    /**
     * Get all clubs
     */
    fun getClubs(): List<Club> = ClubsData.clubs

    /**
     * Get club by ID
     */
    fun getClubById(id: String): Club? = ClubsData.getClubById(id)

    /**
     * Get favorites flow
     */
    fun getFavoritesFlow(): Flow<Set<String>> = cacheManager.getFavoritesFlow()

    /**
     * Toggle favorite
     */
    suspend fun toggleFavorite(classId: String): Boolean = cacheManager.toggleFavorite(classId)

    /**
     * Get favorites
     */
    suspend fun getFavorites(): Set<String> = cacheManager.getFavorites()

    /**
     * Save selected club
     */
    suspend fun saveSelectedClub(clubId: String) = cacheManager.saveSelectedClub(clubId)

    /**
     * Get selected club flow
     */
    fun getSelectedClubFlow(): Flow<String?> = cacheManager.getSelectedClubFlow()

    /**
     * Get selected club
     */
    suspend fun getSelectedClub(): String? = cacheManager.getSelectedClub()

    /**
     * Clear all cache
     */
    suspend fun clearAllCache() = cacheManager.clearAllCache()

    /**
     * Get cache age for club
     */
    suspend fun getCacheAge(clubId: String): Long? = cacheManager.getCacheAge(clubId)
}
