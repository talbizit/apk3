package com.iconfit.schedule.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
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
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import org.jsoup.Jsoup
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume

@Singleton
class ScheduleFetcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: OkHttpClient
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        private const val BASE_URL = "https://www.iconfitness.co.il"
        private const val WEBVIEW_TIMEOUT_MS = 30000L
    }

    init {
        // Create SSL context that accepts all certificates (for debugging Cloudflare issues)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        // Modern TLS configuration
        val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
            .build()

        client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectionSpecs(listOf(spec, ConnectionSpec.CLEARTEXT))
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fetch list of clubs from the Icon Fitness website
     */
    suspend fun fetchClubs(): Result<List<Club>> = withContext(Dispatchers.IO) {
        try {
            // Try OkHttp first
            val html = fetchHtmlWithOkHttp(BASE_URL)
            if (html != null) {
                val clubs = parseClubsFromHtml(html)
                if (clubs.isNotEmpty()) {
                    return@withContext Result.success(clubs)
                }
            }

            // Fallback to WebView if OkHttp fails
            val webViewHtml = fetchHtmlWithWebView(BASE_URL)
            if (webViewHtml != null) {
                val clubs = parseClubsFromHtml(webViewHtml)
                if (clubs.isNotEmpty()) {
                    return@withContext Result.success(clubs)
                }
            }

            Result.failure(Exception("Could not fetch clubs"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchHtmlWithOkHttp(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-S928B Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.101 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "he-IL,he;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Cache-Control", "max-age=0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun fetchHtmlWithWebView(url: String): String? = withContext(Dispatchers.Main) {
        withTimeoutOrNull(WEBVIEW_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val webView = WebView(context)
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.101 Mobile Safari/537.36"
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                // Enable cookies
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webView, true)
                }

                var hasResumed = false

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        if (!hasResumed && pageUrl == url) {
                            // Wait a bit for JavaScript to render
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (!hasResumed) {
                                    hasResumed = true
                                    view?.evaluateJavascript(
                                        "(function() { return document.documentElement.outerHTML; })();"
                                    ) { html ->
                                        val cleanHtml = html?.let {
                                            // Remove escaping from the JavaScript string
                                            it.trim('"')
                                                .replace("\\u003C", "<")
                                                .replace("\\u003E", ">")
                                                .replace("\\\"", "\"")
                                                .replace("\\n", "\n")
                                                .replace("\\t", "\t")
                                                .replace("\\/", "/")
                                        }
                                        webView.destroy()
                                        continuation.resume(cleanHtml)
                                    }
                                }
                            }, 2000) // Wait 2 seconds for JS to render
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                }

                webView.loadUrl(url)

                continuation.invokeOnCancellation {
                    webView.stopLoading()
                    webView.destroy()
                }
            }
        }
    }

    /**
     * Parse clubs from the website HTML
     */
    private fun parseClubsFromHtml(html: String): List<Club> {
        val clubs = mutableListOf<Club>()
        val doc = Jsoup.parse(html)

        // Look for club links in various places
        val allLinks = doc.select("a[href]")

        val regionPatterns = mapOf(
            "צפון" to listOf("בית-שאן", "טבריה", "כרמיאל", "מעלות", "נהריה", "נשר", "עכו", "עפולה", "קרית-מוצקין", "קרית-ביאליק", "זכרון"),
            "מרכז" to listOf("הרצליה", "תל-אביב", "רעננה", "נתניה", "חולון", "בת-ים", "ראשון", "רחובות", "מודיעין", "ירושלים", "כפר-סבא", "פתח", "חדרה", "יבנה", "יהוד", "הוד-השרון", "רמלה", "אור-עקיבא", "פרדס"),
            "דרום" to listOf("באר-שבע", "אשדוד", "אשקלון", "אילת", "דימונה", "נתיבות", "קרית-גת", "שדרות")
        )

        val excludeSlugs = setOf("", "clubs", "about", "contact", "terms", "privacy", "faq", "careers",
            "accessibility", "login", "register", "ico_cinema", "new_pilates", "blog", "news")

        for (link in allLinks) {
            val href = link.attr("href")
            val text = link.text().trim()

            if (text.isBlank() || href.isBlank()) continue
            if (!href.contains("iconfitness.co.il") && !href.startsWith("/")) continue
            if (href.contains("#") || href.contains("javascript") || href.contains("?")) continue
            if (text.length < 3 || text.length > 60) continue

            val slug = when {
                href.contains("iconfitness.co.il/") -> href.substringAfter("iconfitness.co.il/").removeSuffix("/").takeIf { it.isNotBlank() }
                href.startsWith("/") -> href.removePrefix("/").removeSuffix("/").takeIf { it.isNotBlank() }
                else -> null
            }

            if (slug != null && !slug.contains("/") && slug !in excludeSlugs) {
                val region = regionPatterns.entries.find { (_, keywords) ->
                    keywords.any { keyword -> text.contains(keyword) || slug.contains(keyword.replace("-", "")) }
                }?.key ?: "מרכז"

                val clubId = slug.lowercase().replace(Regex("[^a-z0-9-]"), "-").replace(Regex("-+"), "-")

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
     */
    suspend fun fetchSchedule(club: Club): Result<WeekSchedule> = withContext(Dispatchers.IO) {
        try {
            // Try to fetch from website first
            val schedule = fetchScheduleFromWebsite(club)
            if (schedule != null && schedule.classes.isNotEmpty()) {
                return@withContext Result.success(schedule)
            }

            // Fall back to demo data
            Result.success(generateDemoSchedule(club))
        } catch (e: Exception) {
            Result.success(generateDemoSchedule(club))
        }
    }

    private suspend fun fetchScheduleFromWebsite(club: Club): WeekSchedule? {
        try {
            // Icon Fitness club pages use Hebrew slugs
            val scheduleUrl = when {
                club.websiteUrl.isNotBlank() -> club.websiteUrl
                club.hebrewSlug.isNotBlank() -> "$BASE_URL/${club.hebrewSlug}/"
                else -> "$BASE_URL/${club.id}/"
            }

            // Try multiple strategies to get schedule data

            // Strategy 1: Try the club page with OkHttp
            var html = fetchHtmlWithOkHttp(scheduleUrl)

            // Strategy 2: Try alternate URL patterns
            if (html == null || html.length < 1000) {
                val alternateUrls = listOf(
                    "$BASE_URL/${club.hebrewSlug}/schedule/",
                    "$BASE_URL/${club.hebrewSlug}/לוח-שיעורים/",
                    "$BASE_URL/schedule/${club.hebrewSlug}/",
                    "$BASE_URL/clubs/${club.hebrewSlug}/"
                )
                for (url in alternateUrls) {
                    val altHtml = fetchHtmlWithOkHttp(url)
                    if (altHtml != null && altHtml.length > 1000) {
                        html = altHtml
                        break
                    }
                }
            }

            // Strategy 3: Fallback to WebView for JavaScript-rendered content
            if (html == null || !html.contains("class")) {
                html = fetchHtmlWithWebView(scheduleUrl)
            }

            // Strategy 4: Look for embedded JSON data or API endpoints in the HTML
            if (html != null) {
                val scheduleFromJson = extractScheduleFromEmbeddedJson(html, club)
                if (scheduleFromJson != null && scheduleFromJson.classes.isNotEmpty()) {
                    return scheduleFromJson
                }

                // Parse HTML directly
                val parsedSchedule = parseScheduleHtml(html, club)
                if (parsedSchedule != null && parsedSchedule.classes.isNotEmpty()) {
                    return parsedSchedule
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ScheduleFetcher", "Error fetching schedule for ${club.nameHebrew}: ${e.message}")
        }
        return null
    }

    /**
     * Extract schedule data from embedded JSON in the page (common pattern for SPA sites)
     */
    private fun extractScheduleFromEmbeddedJson(html: String, club: Club): WeekSchedule? {
        try {
            val doc = Jsoup.parse(html)

            // Look for script tags with JSON data
            val scriptTags = doc.select("script")
            for (script in scriptTags) {
                val content = script.data()

                // Look for schedule-related JSON patterns
                if (content.contains("schedule") || content.contains("lessons") || content.contains("classes")) {
                    // Extract JSON arrays
                    val jsonArrayPattern = Regex("""\[\s*\{[^]]+?\}\s*(?:,\s*\{[^]]+?\}\s*)*\]""")
                    val matches = jsonArrayPattern.findAll(content)

                    for (match in matches) {
                        val jsonStr = match.value
                        // Look for schedule-like data (has time, day, or similar fields)
                        if (jsonStr.contains("\"time\"") || jsonStr.contains("\"hour\"") ||
                            jsonStr.contains("\"day\"") || jsonStr.contains("\"שעה\"") ||
                            jsonStr.contains("\"יום\"")) {
                            // Parse as schedule (simplified - would need proper JSON parsing)
                            val classes = parseJsonSchedule(jsonStr, club)
                            if (classes.isNotEmpty()) {
                                return WeekSchedule(
                                    clubId = club.id,
                                    classes = classes.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })),
                                    lastUpdated = System.currentTimeMillis(),
                                    weekStartDate = dateFormat.format(Calendar.getInstance().apply {
                                        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                                    }.time),
                                    isRealData = true
                                )
                            }
                        }
                    }
                }
            }

            // Look for data attributes on elements
            val dataElements = doc.select("[data-schedule], [data-lessons], [data-classes]")
            for (element in dataElements) {
                val jsonStr = element.attr("data-schedule").ifBlank {
                    element.attr("data-lessons").ifBlank { element.attr("data-classes") }
                }
                if (jsonStr.isNotBlank()) {
                    val classes = parseJsonSchedule(jsonStr, club)
                    if (classes.isNotEmpty()) {
                        return WeekSchedule(
                            clubId = club.id,
                            classes = classes,
                            lastUpdated = System.currentTimeMillis(),
                            weekStartDate = dateFormat.format(Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                            }.time),
                            isRealData = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ScheduleFetcher", "Error extracting JSON schedule: ${e.message}")
        }
        return null
    }

    /**
     * Parse JSON-formatted schedule data
     */
    private fun parseJsonSchedule(jsonStr: String, club: Club): List<FitnessClass> {
        val classes = mutableListOf<FitnessClass>()
        try {
            // Basic JSON parsing without a full library
            val itemPattern = Regex("""\{[^{}]+\}""")
            val items = itemPattern.findAll(jsonStr)

            for (item in items) {
                val obj = item.value

                // Extract fields (handles both English and Hebrew keys)
                val name = extractJsonField(obj, listOf("name", "title", "className", "שם", "שיעור")) ?: continue
                val instructor = extractJsonField(obj, listOf("instructor", "trainer", "teacher", "מדריך", "מאמן")) ?: ""
                val time = extractJsonField(obj, listOf("time", "hour", "startTime", "שעה", "זמן")) ?: "09:00"
                val room = extractJsonField(obj, listOf("room", "studio", "location", "חדר", "סטודיו")) ?: ""
                val dayStr = extractJsonField(obj, listOf("day", "dayOfWeek", "יום")) ?: "0"

                val dayOfWeek = dayStr.toIntOrNull() ?: parseDayName(dayStr)
                val startTime = time.takeIf { it.contains(":") } ?: "09:00"

                classes.add(FitnessClass(
                    id = "${club.id}_${dayOfWeek}_${startTime}_$name".hashCode().toString(),
                    name = name,
                    instructor = instructor,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = calculateEndTime(startTime, 45),
                    duration = 45,
                    room = room,
                    clubId = club.id,
                    category = detectCategory(name)
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("ScheduleFetcher", "Error parsing JSON schedule: ${e.message}")
        }
        return classes
    }

    private fun extractJsonField(json: String, keys: List<String>): String? {
        for (key in keys) {
            val pattern = Regex(""""$key"\s*:\s*"([^"]+)"""")
            val match = pattern.find(json)
            if (match != null) {
                return match.groupValues[1]
            }
            // Try numeric value
            val numPattern = Regex(""""$key"\s*:\s*(\d+)""")
            val numMatch = numPattern.find(json)
            if (numMatch != null) {
                return numMatch.groupValues[1]
            }
        }
        return null
    }

    private fun parseDayName(dayStr: String): Int {
        return when (dayStr.trim().lowercase()) {
            "sunday", "ראשון", "יום ראשון", "א", "0" -> 0
            "monday", "שני", "יום שני", "ב", "1" -> 1
            "tuesday", "שלישי", "יום שלישי", "ג", "2" -> 2
            "wednesday", "רביעי", "יום רביעי", "ד", "3" -> 3
            "thursday", "חמישי", "יום חמישי", "ה", "4" -> 4
            "friday", "שישי", "יום שישי", "ו", "5" -> 5
            "saturday", "שבת", "יום שבת", "ש", "6" -> 6
            else -> 0
        }
    }

    private fun parseScheduleHtml(html: String, club: Club): WeekSchedule? {
        try {
            val doc = Jsoup.parse(html)
            val classes = mutableListOf<FitnessClass>()

            android.util.Log.d("ScheduleFetcher", "Parsing HTML for ${club.nameHebrew}, HTML length: ${html.length}")

            // Strategy 1: Look for tables (common in Israeli fitness sites)
            classes.addAll(parseScheduleTables(doc, club))

            // Strategy 2: Look for day-based structure (Sunday-Saturday columns)
            if (classes.isEmpty()) {
                classes.addAll(parseDayBasedSchedule(doc, club))
            }

            // Strategy 3: Try generic selectors
            if (classes.isEmpty()) {
                val scheduleSelectors = listOf(
                    ".schedule-item", ".class-item", ".lesson-item", ".studio-class",
                    "[class*='schedule']", "[class*='class-list']", "[class*='lesson']",
                    ".day-classes .class", "[data-class]", "[data-lesson]",
                    ".event-item", ".activity-item", ".session-item",
                    "[class*='shiur']", "[class*='אירוע']", "[class*='פעילות']"
                )

                for (selector in scheduleSelectors) {
                    val elements = doc.select(selector)
                    if (elements.isNotEmpty()) {
                        android.util.Log.d("ScheduleFetcher", "Found ${elements.size} elements with selector: $selector")
                        elements.forEachIndexed { index, element ->
                            try {
                                val className = element.select(".class-name, .title, h3, h4, .name, strong, b")
                                    .firstOrNull()?.text()?.trim() ?: element.ownText().trim()

                                if (className.isBlank() || className.length < 2) return@forEachIndexed

                                val instructor = element.select(".instructor, .trainer, .teacher, .coach, .מדריך")
                                    .firstOrNull()?.text()?.trim() ?: ""

                                val timeText = element.select(".time, .hour, [class*='time'], td:first-child, .שעה")
                                    .firstOrNull()?.text()?.trim() ?: ""

                                val room = element.select(".room, .studio, .location, .place, .סטודיו")
                                    .firstOrNull()?.text()?.trim() ?: ""

                                val dayAttr = element.attr("data-day").toIntOrNull()
                                    ?: element.closest("[data-day]")?.attr("data-day")?.toIntOrNull()
                                    ?: (index % 7)

                                val timeParts = timeText.split("-", "–", "~").map { it.trim() }
                                val startTime = timeParts.getOrNull(0)?.takeIf { it.contains(":") } ?: "09:00"
                                val endTime = timeParts.getOrNull(1)?.takeIf { it.contains(":") } ?: calculateEndTime(startTime, 45)

                                classes.add(FitnessClass(
                                    id = "${club.id}_${dayAttr}_${startTime}_${className}".hashCode().toString(),
                                    name = className,
                                    instructor = instructor,
                                    dayOfWeek = dayAttr,
                                    startTime = startTime,
                                    endTime = endTime,
                                    duration = calculateDuration(startTime, endTime),
                                    room = room,
                                    clubId = club.id,
                                    category = detectCategory(className)
                                ))
                            } catch (e: Exception) {
                                // Skip malformed items
                            }
                        }
                    }
                    if (classes.isNotEmpty()) break
                }
            }

            android.util.Log.d("ScheduleFetcher", "Parsed ${classes.size} classes for ${club.nameHebrew}")

            if (classes.isEmpty()) return null

            return WeekSchedule(
                clubId = club.id,
                classes = classes.distinctBy { "${it.dayOfWeek}_${it.startTime}_${it.name}" }
                    .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })),
                lastUpdated = System.currentTimeMillis(),
                weekStartDate = dateFormat.format(Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                }.time),
                isRealData = true
            )
        } catch (e: Exception) {
            android.util.Log.e("ScheduleFetcher", "Error parsing HTML: ${e.message}")
            return null
        }
    }

    /**
     * Parse schedule from HTML tables (common in Israeli sites)
     */
    private fun parseScheduleTables(doc: org.jsoup.nodes.Document, club: Club): List<FitnessClass> {
        val classes = mutableListOf<FitnessClass>()

        // Find all tables
        val tables = doc.select("table")
        for (table in tables) {
            // Check if this looks like a schedule table
            val headerRow = table.select("thead tr, tr:first-child").firstOrNull()
            val headerCells = headerRow?.select("th, td") ?: continue

            // Hebrew day names to detect schedule tables
            val hebrewDays = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
            val headerText = headerCells.joinToString(" ") { it.text() }

            val dayMapping = mutableMapOf<Int, Int>()
            headerCells.forEachIndexed { colIndex, cell ->
                val text = cell.text().trim()
                hebrewDays.forEachIndexed { dayIndex, dayName ->
                    if (text.contains(dayName)) {
                        dayMapping[colIndex] = dayIndex
                    }
                }
            }

            if (dayMapping.isEmpty()) continue

            android.util.Log.d("ScheduleFetcher", "Found schedule table with ${dayMapping.size} day columns")

            // Parse data rows
            val rows = table.select("tbody tr, tr:not(:first-child)")
            for (row in rows) {
                val cells = row.select("td")
                if (cells.isEmpty()) continue

                // First cell might be time
                val timeCell = cells.firstOrNull()?.text()?.trim() ?: ""
                val timeMatch = Regex("""(\d{1,2}:\d{2})""").find(timeCell)
                val baseTime = timeMatch?.value ?: continue

                // Check each day column
                for ((colIndex, dayOfWeek) in dayMapping) {
                    if (colIndex >= cells.size) continue
                    val cell = cells[colIndex]
                    val cellText = cell.text().trim()

                    if (cellText.isNotBlank() && cellText.length > 2) {
                        // Parse class info from cell
                        val lines = cellText.split("\n", "\\n", "\r").map { it.trim() }.filter { it.isNotBlank() }
                        val className = lines.firstOrNull() ?: cellText
                        val instructor = lines.getOrNull(1)?.takeIf { it.length > 2 } ?: ""
                        val room = lines.getOrNull(2)?.takeIf { it.length > 1 } ?: ""

                        classes.add(FitnessClass(
                            id = "${club.id}_${dayOfWeek}_${baseTime}_$className".hashCode().toString(),
                            name = className,
                            instructor = instructor,
                            dayOfWeek = dayOfWeek,
                            startTime = baseTime,
                            endTime = calculateEndTime(baseTime, 45),
                            duration = 45,
                            room = room,
                            clubId = club.id,
                            category = detectCategory(className)
                        ))
                    }
                }
            }

            if (classes.isNotEmpty()) break
        }

        return classes
    }

    /**
     * Parse day-based schedule (e.g., separate sections for each day)
     */
    private fun parseDayBasedSchedule(doc: org.jsoup.nodes.Document, club: Club): List<FitnessClass> {
        val classes = mutableListOf<FitnessClass>()
        val hebrewDays = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")

        // Look for day containers/sections
        for ((dayIndex, dayName) in hebrewDays.withIndex()) {
            // Find elements containing the day name
            val dayContainers = doc.select(":containsOwn($dayName)")
            for (container in dayContainers) {
                // Look for sibling or child elements with class info
                val parent = container.parent() ?: continue
                val classElements = parent.select(".class, .lesson, .session, .event, li")

                for (element in classElements) {
                    val text = element.text().trim()
                    if (text.length < 4) continue

                    // Try to extract time and class name
                    val timeMatch = Regex("""(\d{1,2}:\d{2})""").find(text)
                    val time = timeMatch?.value ?: continue
                    val className = text.replace(time, "").trim()
                        .replace(Regex("""^[-–:]\s*"""), "")
                        .trim()

                    if (className.length > 2) {
                        classes.add(FitnessClass(
                            id = "${club.id}_${dayIndex}_${time}_$className".hashCode().toString(),
                            name = className,
                            instructor = "",
                            dayOfWeek = dayIndex,
                            startTime = time,
                            endTime = calculateEndTime(time, 45),
                            duration = 45,
                            room = "",
                            clubId = club.id,
                            category = detectCategory(className)
                        ))
                    }
                }
            }
        }

        return classes
    }

    private fun calculateDuration(start: String, end: String): Int {
        return try {
            val startParts = start.split(":").map { it.toInt() }
            val endParts = end.split(":").map { it.toInt() }
            val startMinutes = startParts[0] * 60 + startParts.getOrElse(1) { 0 }
            val endMinutes = endParts[0] * 60 + endParts.getOrElse(1) { 0 }
            (endMinutes - startMinutes).coerceIn(15, 120)
        } catch (e: Exception) {
            45
        }
    }

    private fun detectCategory(className: String): ClassCategory {
        val nameLower = className.lowercase()
        return when {
            nameLower.contains("spin") || nameLower.contains("cycling") || nameLower.contains("ספינינג") -> ClassCategory.CYCLING
            nameLower.contains("yoga") || nameLower.contains("יוגה") -> ClassCategory.YOGA
            nameLower.contains("pilates") || nameLower.contains("פילאטיס") -> ClassCategory.PILATES
            nameLower.contains("dance") || nameLower.contains("zumba") || nameLower.contains("ריקוד") || nameLower.contains("זומבה") -> ClassCategory.DANCE
            nameLower.contains("pump") || nameLower.contains("strength") || nameLower.contains("כוח") -> ClassCategory.STRENGTH
            nameLower.contains("aerob") || nameLower.contains("cardio") || nameLower.contains("step") || nameLower.contains("hiit") || nameLower.contains("אירובי") -> ClassCategory.CARDIO
            nameLower.contains("aqua") || nameLower.contains("water") || nameLower.contains("אקווה") -> ClassCategory.AQUA
            nameLower.contains("box") || nameLower.contains("combat") || nameLower.contains("קרב") || nameLower.contains("קיקבוקס") -> ClassCategory.MARTIAL_ARTS
            nameLower.contains("stretch") || nameLower.contains("מתיחות") || nameLower.contains("גמישות") -> ClassCategory.STRETCH
            nameLower.contains("trx") || nameLower.contains("functional") || nameLower.contains("cross") || nameLower.contains("queenax") -> ClassCategory.FUNCTIONAL
            else -> ClassCategory.OTHER
        }
    }

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
            Triple("Step", ClassCategory.CARDIO, 50),
            Triple("פילאטיס מכשירים", ClassCategory.PILATES, 55),
            Triple("HIIT", ClassCategory.CARDIO, 40),
            Triple("בטן וגב", ClassCategory.STRENGTH, 30),
            Triple("אקווה ג'ים", ClassCategory.AQUA, 45)
        )

        val instructors = listOf(
            "מיכל כהן", "דנה לוי", "יעל אברהם", "נועה שמעון",
            "רון דוד", "אורי גולן", "שירה ברק", "תמר יוסף",
            "איתי מזרחי", "ליאור פרץ", "מאיה שלום", "גיא רוזן"
        )

        val rooms = listOf("סטודיו 1", "סטודיו 2", "סטודיו 3", "אולם ספינינג", "בריכה")

        val morningTimes = listOf("06:30", "07:30", "08:30", "09:30", "10:30", "11:30")
        val afternoonTimes = listOf("16:00", "17:00", "18:00", "19:00", "20:00", "21:00")

        for (day in 0..6) {
            val isWeekend = day == 5 || day == 6
            val times = if (isWeekend) {
                if (day == 5) listOf("07:00", "08:00", "09:00", "10:00", "11:00")
                else listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00")
            } else {
                morningTimes + afternoonTimes
            }

            val clubOffset = kotlin.math.abs(clubSeed) % classTypes.size
            val instructorOffset = kotlin.math.abs(clubSeed / 7) % instructors.size

            times.forEachIndexed { index, startTime ->
                val classIndex = (clubOffset + day * 3 + index) % classTypes.size
                val classType = classTypes[classIndex]
                val instructorIndex = (instructorOffset + day * 2 + index) % instructors.size
                val instructor = instructors[instructorIndex]
                val room = rooms[(clubOffset + index) % rooms.size]
                val duration = classType.third

                classes.add(FitnessClass(
                    id = "${club.id}_${day}_${startTime}".hashCode().toString(),
                    name = classType.first,
                    instructor = instructor,
                    dayOfWeek = day,
                    startTime = startTime,
                    endTime = calculateEndTime(startTime, duration),
                    duration = duration,
                    room = room,
                    clubId = club.id,
                    category = classType.second
                ))
            }
        }

        return WeekSchedule(
            clubId = club.id,
            classes = classes.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime })),
            lastUpdated = System.currentTimeMillis(),
            weekStartDate = dateFormat.format(Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }.time),
            isRealData = false // Demo data
        )
    }

    private fun calculateEndTime(startTime: String, durationMinutes: Int): String {
        val parts = startTime.split(":").map { it.toIntOrNull() ?: 0 }
        var hours = parts[0]
        var minutes = parts.getOrElse(1) { 0 } + durationMinutes
        hours += minutes / 60
        minutes %= 60
        return String.format("%02d:%02d", hours % 24, minutes)
    }
}
