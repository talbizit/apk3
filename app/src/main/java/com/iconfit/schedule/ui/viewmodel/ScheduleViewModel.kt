package com.iconfit.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iconfit.schedule.data.ClubsData
import com.iconfit.schedule.data.model.Club
import com.iconfit.schedule.data.model.FitnessClass
import com.iconfit.schedule.data.model.WeekSchedule
import com.iconfit.schedule.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AllClubsUiState(
    val isLoading: Boolean = true,
    val selectedDay: Int? = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1, // Today, null = All
    val searchQuery: String = "",
    val schedulesByClub: Map<String, WeekSchedule> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val expandedClubs: Set<String> = emptySet() // Which clubs are expanded
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllClubsUiState())
    val uiState: StateFlow<AllClubsUiState> = _uiState.asStateFlow()

    val clubs = ClubsData.clubs
    val clubsByRegion = ClubsData.clubsByRegion
    val regions = ClubsData.regions

    init {
        loadAllSchedules()
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            repository.getFavoritesFlow().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }
    }

    private fun loadAllSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val schedules = mutableMapOf<String, WeekSchedule>()
            clubs.forEach { club ->
                val result = repository.getScheduleForClub(club)
                result?.let { schedules[club.id] = it }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    schedulesByClub = schedules
                )
            }
        }
    }

    fun refresh() {
        loadAllSchedules()
    }

    fun selectDay(day: Int?) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleClubExpanded(clubId: String) {
        _uiState.update { state ->
            val expanded = state.expandedClubs.toMutableSet()
            if (expanded.contains(clubId)) {
                expanded.remove(clubId)
            } else {
                expanded.add(clubId)
            }
            state.copy(expandedClubs = expanded)
        }
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
            loadAllSchedules()
        }
    }

    fun getClassesForClub(clubId: String): List<FitnessClass> {
        val state = _uiState.value
        val schedule = state.schedulesByClub[clubId] ?: return emptyList()

        // Filter by day (if selectedDay is null, show all days)
        var classes = if (state.selectedDay != null) {
            schedule.classes.filter { it.dayOfWeek == state.selectedDay }
        } else {
            schedule.classes
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

        return classes.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    }

    fun hasClassesForDay(clubId: String): Boolean {
        return getClassesForClub(clubId).isNotEmpty()
    }

    fun getAllFavoriteClasses(): List<Pair<Club, FitnessClass>> {
        val state = _uiState.value
        val result = mutableListOf<Pair<Club, FitnessClass>>()

        clubs.forEach { club ->
            val schedule = state.schedulesByClub[club.id] ?: return@forEach
            schedule.classes
                .filter { state.favorites.contains(it.id) }
                .forEach { fitnessClass ->
                    result.add(Pair(club, fitnessClass))
                }
        }

        return result.sortedWith(compareBy({ it.second.dayOfWeek }, { it.second.startTime }))
    }

    fun getTodayDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        return (calendar.get(Calendar.DAY_OF_WEEK) - 1)
    }
}
