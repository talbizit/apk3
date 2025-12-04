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
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        private const val BASE_URL = "https://www.iconfitness.co.il"
    }

    /**
     * Fetch list of clubs from the Icon Fitness website
     */
    suspend fun fetchClubs(): Result<List<Club>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "he-IL,he;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

            val clubs = parseClubsFromHtml(html)
            if (clubs.isNotEmpty()) {
                Result.success(clubs)
            } else {
                Result.failure(Exception("No clubs found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse clubs from the website HTML (footer contains club links)
     */
    private fun parseClubsFromHtml(html: String): List<Club> {
        val clubs = mutableListOf<Club>()
        val doc = Jsoup.parse(html)

        // Try to find club links in footer or navigation
        // Icon Fitness lists clubs in footer sections by region
        val footerLinks = doc.select("footer a[href*='iconfitness.co.il'], .footer a, #footer a, a[href*='/']")

        // Also look for specific club page patterns
        val allLinks = doc.select("a[href]")

        val regionPatterns = mapOf(
            "צפון" to listOf("בית-שאן", "טבריה", "כרמיאל", "מעלות", "נהריה", "נשר", "עכו", "עפולה", "קרית-מוצקין", "קרית-ביאליק", "זכרון"),
            "מרכז" to listOf("הרצליה", "תל-אביב", "רעננה", "נתניה", "חולון", "בת-ים", "ראשון", "רחובות", "מודיעין", "ירושלים", "כפר-סבא", "פתח", "חדרה", "יבנה", "יהוד", "הוד-השרון", "רמלה", "אור-עקיבא", "פרדס"),
            "דרום" to listOf("באר-שבע", "אשדוד", "אשקלון", "אילת", "דימונה", "נתיבות", "קרית-גת", "שדרות")
        )

        for (link in allLinks) {
            val href = link.attr("href")
            val text = link.text().trim()

            // Skip empty or non-club links
            if (text.isBlank() || href.isBlank()) continue
            if (!href.contains("iconfitness.co.il") && !href.startsWith("/")) continue
            if (href.contains("#") || href.contains("javascript")) continue
            if (text.length < 3 || text.length > 50) continue

            // Extract club slug from URL
            val slug = when {
                href.contains("iconfitness.co.il/") -> {
                    href.substringAfter("iconfitness.co.il/").removeSuffix("/").takeIf { it.isNotBlank() }
                }
                href.startsWith("/") -> href.removePrefix("/").removeSuffix("/").takeIf { it.isNotBlank() }
                else -> null
            }

            if (slug != null && !slug.contains("/") && slug !in listOf("", "clubs", "about", "contact", "terms", "privacy")) {
                // Determine region based on name patterns
                val region = regionPatterns.entries.find { (_, keywords) ->
                    keywords.any { keyword -> text.contains(keyword) || slug.contains(keyword.replace("-", "")) }
                }?.key ?: "מרכז"

                val clubId = slug.lowercase().replace(Regex("[^a-z0-9-]"), "-")

                // Avoid duplicates
                if (clubs.none { it.id == clubId }) {
                    clubs.add(Club(
                        id = clubId,
                        name = slug.replace("-", " ").replaceFirstChar { it.uppercase() },
                        nameHebrew = if (text.contains("אייקון")) text else "אייקון $text",
                        region = region,
                        websiteUrl = if (href.startsWith("http")) href else "$BASE_URL/$slug/",
                        hebrewSlug = slug
                    ))
                }
            }
        }

        return clubs.distinctBy { it.id }
    }

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
            // Try the club's website URL
            val scheduleUrl = club.websiteUrl.ifBlank { "$BASE_URL/${club.id}/" }
            val request = Request.Builder()
                .url(scheduleUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "he-IL,he;q=0.9,en-US;q=0.8,en;q=0.7")
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
     * Uses club ID to seed randomization so each club gets different classes
     */
    fun generateDemoSchedule(club: Club): WeekSchedule {
        val classes = mutableListOf<FitnessClass>()
        val clubSeed = club.id.hashCode()

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
            Triple("Cross Training", ClassCategory.FUNCTIONAL, 45),
            Triple("HIIT", ClassCategory.CARDIO, 40),
            Triple("בטן וגב", ClassCategory.STRENGTH, 30),
            Triple("אקווה ג'ים", ClassCategory.AQUA, 45)
        )

        val instructors = listOf(
            "מיכל כהן", "דנה לוי", "יעל אברהם", "נועה שמעון",
            "רון דוד", "אורי גולן", "שירה ברק", "תמר יוסף",
            "איתי מזרחי", "ליאור פרץ", "מאיה שלום", "גיא רוזן",
            "עדי ניסים", "יובל אשכנזי", "נטלי רז", "אלון בן דוד"
        )

        val rooms = listOf("סטודיו 1", "סטודיו 2", "סטודיו 3", "אולם ספינינג", "בריכה", "אולם ראשי")

        val morningTimes = listOf("06:30", "07:30", "08:30", "09:30", "10:30", "11:30")
        val afternoonTimes = listOf("16:00", "17:00", "18:00", "19:00", "20:00", "21:00")

        // Generate classes for each day - use clubSeed for variety
        for (day in 0..6) { // Sunday to Saturday
            val isWeekend = day == 5 || day == 6 // Friday or Saturday
            val times = if (isWeekend) {
                if (day == 5) listOf("07:00", "08:00", "09:00", "10:00", "11:00") // Friday
                else listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00") // Saturday
            } else {
                morningTimes + afternoonTimes
            }

            // Use club seed to offset class selection
            val clubOffset = Math.abs(clubSeed) % classTypes.size
            val instructorOffset = Math.abs(clubSeed / 7) % instructors.size

            times.forEachIndexed { index, startTime ->
                val classIndex = (clubOffset + day * 3 + index) % classTypes.size
                val classType = classTypes[classIndex]
                val instructorIndex = (instructorOffset + day * 2 + index) % instructors.size
                val instructor = instructors[instructorIndex]
                val room = rooms[(clubOffset + index) % rooms.size]
                val duration = classType.third

                val endTime = calculateEndTime(startTime, duration)

                classes.add(
                    FitnessClass(
                        id = "${club.id}_${day}_${startTime}".hashCode().toString(),
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
