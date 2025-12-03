package com.iconfit.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iconfit.schedule.data.model.HebrewDays

data class DayOption(
    val day: Int?, // null for "All"
    val label: String,
    val isToday: Boolean = false
)

@Composable
fun DaySelector(
    selectedDay: Int?,
    todayDay: Int,
    onDaySelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        DayOption(null, "הכל"),
    ) + HebrewDays.days.mapIndexed { index, name ->
        DayOption(index, name, index == todayDay)
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        reverseLayout = true // RTL support
    ) {
        items(days.reversed()) { dayOption ->
            DayChip(
                day = dayOption,
                isSelected = selectedDay == dayOption.day,
                onClick = { onDaySelected(dayOption.day) }
            )
        }
    }
}

@Composable
private fun DayChip(
    day: DayOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = day.label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            if (day.isToday && !isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
