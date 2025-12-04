package com.iconfit.schedule.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iconfit.schedule.data.ClubsData
import com.iconfit.schedule.data.model.Club
import com.iconfit.schedule.data.model.FitnessClass
import com.iconfit.schedule.ui.components.ClassCard
import com.iconfit.schedule.ui.components.DaySelector
import com.iconfit.schedule.ui.components.SearchBar
import com.iconfit.schedule.ui.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "לוח שיעורים - Icon Fitness",
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
            // Day selector
            DaySelector(
                selectedDay = uiState.selectedDay,
                todayDay = viewModel.getTodayDayOfWeek(),
                onDaySelected = { viewModel.selectDay(it) }
            )

            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            // Content
            if (uiState.isLoading) {
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
                            text = "טוען לוחות שיעורים...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // All clubs list grouped by region
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    uiState.regions.forEach { region ->
                        val clubsInRegion = uiState.clubsByRegion[region] ?: emptyList()
                        val clubsWithClasses = clubsInRegion.filter { club ->
                            viewModel.hasClassesForDay(club.id)
                        }

                        // Only show region if it has clubs with classes (or no search query)
                        if (clubsWithClasses.isNotEmpty() || uiState.searchQuery.isBlank()) {
                            // Region header
                            item(key = "region_$region") {
                                RegionHeader(regionName = region)
                            }

                            // Clubs in region
                            items(
                                items = if (uiState.searchQuery.isNotBlank()) clubsWithClasses else clubsInRegion,
                                key = { "club_${it.id}" }
                            ) { club ->
                                val classes = viewModel.getClassesForClub(club.id)
                                val isExpanded = uiState.expandedClubs.contains(club.id)
                                val isRealData = viewModel.isRealData(club.id)

                                ClubCard(
                                    club = club,
                                    classCount = classes.size,
                                    isExpanded = isExpanded,
                                    isRealData = isRealData,
                                    onToggleExpand = { viewModel.toggleClubExpanded(club.id) },
                                    classes = classes,
                                    isFavorite = { viewModel.isFavorite(it) },
                                    onFavoriteClick = { viewModel.toggleFavorite(it) },
                                    onOpenWebsite = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(club.websiteUrl))
                                        context.startActivity(intent)
                                    },
                                    onOpenApp = {
                                        // Try to open Icon Fitness app
                                        val appIntent = context.packageManager.getLaunchIntentForPackage(ClubsData.ICON_APP_PACKAGE)
                                        if (appIntent != null) {
                                            context.startActivity(appIntent)
                                        } else {
                                            // App not installed, open Play Store
                                            val playStoreIntent = Intent(Intent.ACTION_VIEW,
                                                Uri.parse("https://play.google.com/store/apps/details?id=${ClubsData.ICON_APP_PACKAGE}"))
                                            context.startActivity(playStoreIntent)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Empty state for search
                    if (uiState.searchQuery.isNotBlank()) {
                        val totalClasses = uiState.clubs.sumOf { viewModel.getClassesForClub(it.id).size }
                        if (totalClasses == 0) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "לא נמצאו תוצאות",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionHeader(regionName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = regionName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ClubCard(
    club: Club,
    classCount: Int,
    isExpanded: Boolean,
    isRealData: Boolean,
    onToggleExpand: () -> Unit,
    classes: List<FitnessClass>,
    isFavorite: (String) -> Boolean,
    onFavoriteClick: (FitnessClass) -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 4.dp else 1.dp
        )
    ) {
        Column {
            // Club header (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = club.nameHebrew,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = when {
                            classCount > 0 && isRealData -> "$classCount שיעורים"
                            classCount > 0 -> "$classCount שיעורים (לדוגמה)"
                            else -> "לחץ לפרטים"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRealData)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "סגור" else "פתח",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Expanded content with classes
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    // Open official schedule buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenApp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("אפליקציה", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onOpenWebsite,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("לוח רשמי", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Data source notice
                    if (!isRealData) {
                        Text(
                            text = "נתונים לדוגמה - פתח את הלוח הרשמי לשיעורים עדכניים",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = "נתונים מעודכנים מהאתר",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    if (classes.isEmpty()) {
                        Text(
                            text = "אין שיעורים ביום זה",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    } else {
                        classes.forEach { fitnessClass ->
                            ClassCard(
                                fitnessClass = fitnessClass,
                                isFavorite = isFavorite(fitnessClass.id),
                                onFavoriteClick = { onFavoriteClick(fitnessClass) }
                            )
                        }
                    }
                }
            }
        }
    }
}
