package com.iconfit.schedule.data.model

sealed class LoadingState<out T> {
    data object Loading : LoadingState<Nothing>()
    data class Success<T>(val data: T, val isStale: Boolean = false, val isRefreshing: Boolean = false) : LoadingState<T>()
    data class Error(val message: String, val cachedData: Any? = null) : LoadingState<Nothing>()
}

data class ScheduleUiState(
    val clubs: List<Club> = emptyList(),
    val selectedClub: Club? = null,
    val schedule: LoadingState<WeekSchedule> = LoadingState.Loading,
    val selectedDay: Int? = null, // null = all days
    val searchQuery: String = "",
    val favorites: Set<String> = emptySet(),
    val isRefreshingInBackground: Boolean = false,
    val lastUpdateTime: Long? = null
)

data class FilteredClasses(
    val classes: List<FitnessClass>,
    val groupedByDay: Map<Int, List<FitnessClass>>
)
