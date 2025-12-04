package com.iconfit.schedule.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Club(
    val id: String,
    val name: String,
    val nameHebrew: String,
    val region: String = "",
    val address: String = "",
    val websiteUrl: String = "",  // Link to official schedule page
    val hebrewSlug: String = ""   // Hebrew URL slug for website
)

@Serializable
data class FitnessClass(
    val id: String,
    val name: String,
    val instructor: String,
    val dayOfWeek: Int, // 0 = Sunday, 6 = Saturday
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val duration: Int,     // minutes
    val room: String = "",
    val clubId: String,
    val category: ClassCategory = ClassCategory.OTHER
)

@Serializable
enum class ClassCategory {
    CYCLING,      // ספינינג
    YOGA,         // יוגה
    PILATES,      // פילאטיס
    DANCE,        // ריקוד
    STRENGTH,     // כוח
    CARDIO,       // אירובי
    AQUA,         // מים
    MARTIAL_ARTS, // אומנויות לחימה
    STRETCH,      // מתיחות
    FUNCTIONAL,   // פונקציונלי
    OTHER
}

@Serializable
data class WeekSchedule(
    val clubId: String,
    val classes: List<FitnessClass>,
    val lastUpdated: Long, // timestamp
    val weekStartDate: String // "yyyy-MM-dd"
)

@Serializable
data class CachedSchedule(
    val schedule: WeekSchedule,
    val cachedAt: Long,
    val isStale: Boolean = false
)

@Serializable
data class Favorite(
    val classId: String,
    val className: String,
    val instructor: String,
    val dayOfWeek: Int,
    val startTime: String,
    val clubId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
data class UserPreferences(
    val selectedClubId: String? = null,
    val favorites: Set<String> = emptySet(), // class IDs
    val lastSyncTime: Long = 0
)

// Hebrew day names
object HebrewDays {
    val days = listOf(
        "ראשון",   // Sunday
        "שני",     // Monday
        "שלישי",   // Tuesday
        "רביעי",   // Wednesday
        "חמישי",   // Thursday
        "שישי",    // Friday
        "שבת"      // Saturday
    )

    fun getDayName(dayOfWeek: Int): String = days.getOrElse(dayOfWeek) { "" }
}

// Category Hebrew names
object CategoryNames {
    val names = mapOf(
        ClassCategory.CYCLING to "ספינינג",
        ClassCategory.YOGA to "יוגה",
        ClassCategory.PILATES to "פילאטיס",
        ClassCategory.DANCE to "ריקוד",
        ClassCategory.STRENGTH to "אימון כוח",
        ClassCategory.CARDIO to "אירובי",
        ClassCategory.AQUA to "אקווה",
        ClassCategory.MARTIAL_ARTS to "אומנויות לחימה",
        ClassCategory.STRETCH to "מתיחות",
        ClassCategory.FUNCTIONAL to "פונקציונלי",
        ClassCategory.OTHER to "אחר"
    )

    fun getName(category: ClassCategory): String = names[category] ?: "אחר"
}
