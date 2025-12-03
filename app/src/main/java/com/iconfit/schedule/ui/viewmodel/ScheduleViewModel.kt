package com.iconfit.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iconfit.schedule.data.model.Club
import com.iconfit.schedule.data.model.FitnessClass
import com.iconfit.schedule.data.model.LoadingState
import com.iconfit.schedule.data.model.ScheduleUiState
import com.iconfit.schedule.data.model.WeekSchedule
import com.iconfit.schedule.data.repository.ScheduleRepository
import com.iconfit.schedule.data.repository.ScheduleResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load clubs
            val clubs = repository.getClubs()
            _uiState.update { it.copy(clubs = clubs) }

            // Load favorites
            repository.getFavoritesFlow().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }

        viewModelScope.launch {
            // Load previously selected club or default to first
            val selectedClubId = repository.getSelectedClub()
            val clubs = repository.getClubs()
            val club = selectedClubId?.let { repository.getClubById(it) } ?: clubs.firstOrNull()

            if (club != null) {
                selectClub(club)
            }
        }
    }

    fun selectClub(club: Club) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedClub = club,
                    schedule = LoadingState.Loading
                )
            }

            repository.saveSelectedClub(club.id)
            loadSchedule(club)
        }
    }

    private fun loadSchedule(club: Club, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getSchedule(club, forceRefresh).collect { result ->
                when (result) {
                    is ScheduleResult.Fresh -> {
                        _uiState.update {
                            it.copy(
                                schedule = LoadingState.Success(result.schedule),
                                isRefreshingInBackground = false,
                                lastUpdateTime = System.currentTimeMillis()
                            )
                        }
                    }
                    is ScheduleResult.Cached -> {
                        _uiState.update {
                            it.copy(
                                schedule = LoadingState.Success(result.schedule, isStale = result.isStale),
                                isRefreshingInBackground = false,
                                lastUpdateTime = result.cachedAt
                            )
                        }
                    }
                    is ScheduleResult.RefreshingInBackground -> {
                        _uiState.update {
                            it.copy(
                                schedule = LoadingState.Success(result.schedule, isStale = true, isRefreshing = true),
                                isRefreshingInBackground = true,
                                lastUpdateTime = result.cachedAt
                            )
                        }
                    }
                    is ScheduleResult.Refreshed -> {
                        _uiState.update {
                            it.copy(
                                schedule = LoadingState.Success(result.schedule),
                                isRefreshingInBackground = false,
                                lastUpdateTime = System.currentTimeMillis()
                            )
                        }
                    }
                    is ScheduleResult.Error -> {
                        if (result.cachedSchedule != null) {
                            _uiState.update {
                                it.copy(
                                    schedule = LoadingState.Success(result.cachedSchedule, isStale = true),
                                    isRefreshingInBackground = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    schedule = LoadingState.Error(result.message),
                                    isRefreshingInBackground = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        val club = _uiState.value.selectedClub ?: return
        loadSchedule(club, forceRefresh = true)
    }

    fun selectDay(day: Int?) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavorite(fitnessClass: FitnessClass) {
        viewModelScope.launch {
            repository.toggleFavorite(fitnessClass.id)
        }
    }

    fun isFavorite(classId: String): Boolean {
        return _uiState.value.favorites.contains(classId)
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearAllCache()
            _uiState.value.selectedClub?.let { club ->
                loadSchedule(club, forceRefresh = true)
            }
        }
    }

    fun getFilteredClasses(): List<FitnessClass> {
        val state = _uiState.value
        val schedule = (state.schedule as? LoadingState.Success)?.data ?: return emptyList()

        var classes = schedule.classes

        // Filter by day
        state.selectedDay?.let { day ->
            classes = classes.filter { it.dayOfWeek == day }
        }

        // Filter by search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            classes = classes.filter {
                it.name.lowercase().contains(query) ||
                it.instructor.lowercase().contains(query) ||
                it.room.lowercase().contains(query)
            }
        }

        return classes
    }

    fun getClassesGroupedByDay(): Map<Int, List<FitnessClass>> {
        return getFilteredClasses().groupBy { it.dayOfWeek }
    }

    fun getFavoriteClasses(): List<FitnessClass> {
        val state = _uiState.value
        val schedule = (state.schedule as? LoadingState.Success)?.data ?: return emptyList()

        return schedule.classes.filter { state.favorites.contains(it.id) }
    }

    fun getTodayDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        // Convert from Calendar (Sunday=1) to our format (Sunday=0)
        return (calendar.get(Calendar.DAY_OF_WEEK) - 1)
    }
}
