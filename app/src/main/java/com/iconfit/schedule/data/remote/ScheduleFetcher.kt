package com.iconfit.schedule.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.iconfit.schedule.data.model.ClassCategory
import com.iconfit.schedule.data.model.Club
import com.iconfit.schedule.data.model.FitnessClass
import com.iconfit.schedule.data.model.WeekSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ScheduleFetcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Fetch schedule for a club
     * Falls back to demo data if fetching fails
     */
    suspend fun fetchSchedule(club: Club): Result<WeekSchedule> = withContext(Dispatchers.IO) {
        try {
            // Try to fetch from website first
            val schedule = fetchFromWebsite(club)
            if (schedule != null && schedule.classes.isNotEmpty()) {
                return@withContext Result.success(schedule)
            }

            // Fall back to demo data
            Result.success(generateDemoSchedule(club))
        } catch (e: Exception) {
            // Return demo data on any error
            Result.success(generateDemoSchedule(club))
        }
    }

    private suspend fun fetchFromWebsite(club: Club): WeekSchedule? {
        return try {
            // Icon Fitness uses Fizikal platform - construct schedule URL
            val scheduleUrl = "https://www.iconfitness.co.il/club/${club.id}/schedule"
            val request = Request.Builder()
                .url(scheduleUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "he-IL,he;q=0.9,en;q=0.8")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return null

            parseScheduleHtml(html, club)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseScheduleHtml(html: String, club: Club): WeekSchedule? {
        try {
            val doc = Jsoup.parse(html)
            val classes = mutableListOf<FitnessClass>()

            // Try to find schedule table/list elements
            // Common patterns for schedule pages
            val scheduleElements = doc.select(
                ".schedule-item, .class-item, .lesson-item, " +
                "[class*=schedule], [class*=class], [class*=lesson], " +
                "tr[data-day], .day-schedule .item"
            )

            if (scheduleElements.isEmpty()) {
                return null
            }

            scheduleElements.forEachIndexed { index, element ->
                try {
                    val className = element.select(".class-name, .title, h3, h4, .name")
                        .firstOrNull()?.text() ?: return@forEachIndexed

                    val instructor = element.select(".instructor, .trainer, .teacher")
                        .firstOrNull()?.text() ?: ""

                    val time = element.select(".time, .hour, [class*=time]")
                        .firstOrNull()?.text() ?: ""

                    val room = element.select(".room, .studio, .location")
                        .firstOrNull()?.text() ?: ""

                    val dayAttr = element.attr("data-day")
                    val day = dayAttr.toIntOrNull() ?: (index % 7)

                    val timeParts = time.split("-", "–").map { it.trim() }
                    val startTime = timeParts.getOrNull(0) ?: "09:00"
                    val endTime = timeParts.getOrNull(1) ?: "10:00"

                    classes.add(
                        FitnessClass(
                            id = "${club.id}_${day}_${startTime}_${className}".hashCode().toString(),
                            name = className,
                            instructor = instructor,
                            dayOfWeek = day,
                            startTime = startTime,
                            endTime = endTime,
                            duration = calculateDuration(startTime, endTime),
                            room = room,
                            clubId = club.id,
                            category = detectCategory(className)
                        )
                    )
                } catch (e: Exception) {
                    // Skip malformed items
                }
            }

            if (classes.isEmpty()) return null

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

            return WeekSchedule(
                clubId = club.id,
                classes = classes.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })),
                lastUpdated = System.currentTimeMillis(),
                weekStartDate = dateFormat.format(calendar.time)
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateDuration(start: String, end: String): Int {
        return try {
            val startParts = start.split(":").map { it.toInt() }
            val endParts = end.split(":").map { it.toInt() }
            val startMinutes = startParts[0] * 60 + startParts[1]
            val endMinutes = endParts[0] * 60 + endParts[1]
            (endMinutes - startMinutes).coerceAtLeast(30)
        } catch (e: Exception) {
            45 // Default duration
        }
    }

    private fun detectCategory(className: String): ClassCategory {
        val nameLower = className.lowercase()
        return when {
            nameLower.contains("spin") || nameLower.contains("cycling") || nameLower.contains("ספינינג") -> ClassCategory.CYCLING
            nameLower.contains("yoga") || nameLower.contains("יוגה") -> ClassCategory.YOGA
            nameLower.contains("pilates") || nameLower.contains("פילאטיס") -> ClassCategory.PILATES
            nameLower.contains("dance") || nameLower.contains("zumba") || nameLower.contains("ריקוד") || nameLower.contains("זומבה") -> ClassCategory.DANCE
            nameLower.contains("pump") || nameLower.contains("strength") || nameLower.contains("כוח") || nameLower.contains("body pump") -> ClassCategory.STRENGTH
            nameLower.contains("aerob") || nameLower.contains("cardio") || nameLower.contains("step") || nameLower.contains("אירובי") -> ClassCategory.CARDIO
            nameLower.contains("aqua") || nameLower.contains("water") || nameLower.contains("אקווה") || nameLower.contains("מים") -> ClassCategory.AQUA
            nameLower.contains("box") || nameLower.contains("combat") || nameLower.contains("martial") || nameLower.contains("לחימה") || nameLower.contains("קרב") -> ClassCategory.MARTIAL_ARTS
            nameLower.contains("stretch") || nameLower.contains("מתיחות") || nameLower.contains("גמישות") -> ClassCategory.STRETCH
            nameLower.contains("trx") || nameLower.contains("functional") || nameLower.contains("cross") || nameLower.contains("queenax") || nameLower.contains("פונקציונלי") -> ClassCategory.FUNCTIONAL
            else -> ClassCategory.OTHER
        }
    }

    /**
     * Generate demo schedule data for testing
     */
    fun generateDemoSchedule(club: Club): WeekSchedule {
        val classes = mutableListOf<FitnessClass>()
        val classTypes = listOf(
            Triple("ספינינג", ClassCategory.CYCLING, 45),
            Triple("יוגה", ClassCategory.YOGA, 60),
            Triple("פילאטיס", ClassCategory.PILATES, 55),
            Triple("זומבה", ClassCategory.DANCE, 50),
            Triple("Body Pump", ClassCategory.STRENGTH, 45),
            Triple("TRX", ClassCategory.FUNCTIONAL, 45),
            Triple("אירובי", ClassCategory.CARDIO, 50),
            Triple("קיקבוקס", ClassCategory.MARTIAL_ARTS, 45),
            Triple("מתיחות", ClassCategory.STRETCH, 30),
            Triple("Queenax", ClassCategory.FUNCTIONAL, 45),
            Triple("יוגה זורמת", ClassCategory.YOGA, 75),
            Triple("Step Aerobics", ClassCategory.CARDIO, 50),
            Triple("פילאטיס מכשירים", ClassCategory.PILATES, 55),
            Triple("ספינינג אנרגטי", ClassCategory.CYCLING, 50),
            Triple("Cross Training", ClassCategory.FUNCTIONAL, 45)
        )

        val instructors = listOf(
            "מיכל כהן", "דנה לוי", "יעל אברהם", "נועה שמעון",
            "רון דוד", "אורי גולן", "שירה ברק", "תמר יוסף",
            "איתי מזרחי", "ליאור פרץ", "מאיה שלום", "גיא רוזן"
        )

        val rooms = listOf("סטודיו 1", "סטודיו 2", "סטודיו 3", "אולם ספינינג", "בריכה")

        val morningTimes = listOf("06:30", "07:30", "08:30", "09:30", "10:30", "11:30")
        val afternoonTimes = listOf("16:00", "17:00", "18:00", "19:00", "20:00", "21:00")

        // Generate classes for each day
        for (day in 0..6) { // Sunday to Saturday
            val isWeekend = day == 5 || day == 6 // Friday or Saturday
            val times = if (isWeekend) {
                if (day == 5) listOf("07:00", "08:00", "09:00", "10:00", "11:00") // Friday
                else listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00") // Saturday
            } else {
                morningTimes + afternoonTimes
            }

            times.forEachIndexed { index, startTime ->
                val classType = classTypes[(day * 3 + index) % classTypes.size]
                val instructor = instructors[(day * 2 + index) % instructors.size]
                val room = rooms[index % rooms.size]
                val duration = classType.third

                val endTime = calculateEndTime(startTime, duration)

                classes.add(
                    FitnessClass(
                        id = "${club.id}_${day}_${startTime}_${classType.first}".hashCode().toString(),
                        name = classType.first,
                        instructor = instructor,
                        dayOfWeek = day,
                        startTime = startTime,
                        endTime = endTime,
                        duration = duration,
                        room = room,
                        clubId = club.id,
                        category = classType.second
                    )
                )
            }
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        return WeekSchedule(
            clubId = club.id,
            classes = classes.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })),
            lastUpdated = System.currentTimeMillis(),
            weekStartDate = dateFormat.format(calendar.time)
        )
    }

    private fun calculateEndTime(startTime: String, durationMinutes: Int): String {
        val parts = startTime.split(":").map { it.toInt() }
        var hours = parts[0]
        var minutes = parts[1] + durationMinutes
        hours += minutes / 60
        minutes %= 60
        return String.format("%02d:%02d", hours % 24, minutes)
    }
}
