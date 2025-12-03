package com.iconfit.schedule.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iconfit.schedule.data.model.HebrewDays
import com.iconfit.schedule.data.model.LoadingState
import com.iconfit.schedule.ui.components.ClassCard
import com.iconfit.schedule.ui.components.ClubSelector
import com.iconfit.schedule.ui.components.DaySelector
import com.iconfit.schedule.ui.components.SearchBar
import com.iconfit.schedule.ui.components.StatusIndicator
import com.iconfit.schedule.ui.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "לוח שיעורים",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "רענן"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Club selector
            ClubSelector(
                clubs = uiState.clubs,
                selectedClub = uiState.selectedClub,
                onClubSelected = { viewModel.selectClub(it) }
            )

            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            // Day selector
            DaySelector(
                selectedDay = uiState.selectedDay,
                todayDay = viewModel.getTodayDayOfWeek(),
                onDaySelected = { viewModel.selectDay(it) }
            )

            // Status indicator
            val scheduleState = uiState.schedule
            if (scheduleState is LoadingState.Success) {
                StatusIndicator(
                    isRefreshing = scheduleState.isRefreshing,
                    isStale = scheduleState.isStale,
                    lastUpdateTime = uiState.lastUpdateTime
                )
            }

            // Content
            when (val state = uiState.schedule) {
                is LoadingState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "טוען לוח שיעורים...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                is LoadingState.Success -> {
                    val filteredClasses = viewModel.getFilteredClasses()
                    val classesGroupedByDay = viewModel.getClassesGroupedByDay()

                    if (filteredClasses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (uiState.searchQuery.isNotEmpty())
                                        Icons.Default.SearchOff
                                    else Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (uiState.searchQuery.isNotEmpty())
                                        "לא נמצאו תוצאות"
                                    else "אין שיעורים",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            if (uiState.selectedDay != null) {
                                // Show classes for selected day
                                items(filteredClasses, key = { it.id }) { fitnessClass ->
                                    ClassCard(
                                        fitnessClass = fitnessClass,
                                        isFavorite = viewModel.isFavorite(fitnessClass.id),
                                        onFavoriteClick = { viewModel.toggleFavorite(fitnessClass) }
                                    )
                                }
                            } else {
                                // Show grouped by day
                                classesGroupedByDay.entries.sortedBy { it.key }.forEach { (day, classes) ->
                                    item(key = "header_$day") {
                                        DayHeader(dayName = HebrewDays.getDayName(day))
                                    }
                                    items(classes, key = { it.id }) { fitnessClass ->
                                        ClassCard(
                                            fitnessClass = fitnessClass,
                                            isFavorite = viewModel.isFavorite(fitnessClass.id),
                                            onFavoriteClick = { viewModel.toggleFavorite(fitnessClass) }
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }

                is LoadingState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "😕",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(dayName: String) {
    Text(
        text = "יום $dayName",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        textAlign = TextAlign.End
    )
}
