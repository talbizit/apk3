package com.iconfit.schedule.data

import com.iconfit.schedule.data.model.Club

/**
 * List of Icon Fitness clubs in Israel
 */
object ClubsData {
    val clubs = listOf(
        Club(
            id = "tel-aviv",
            name = "Tel Aviv",
            nameHebrew = "תל אביב",
            address = "אחד העם 9, תל אביב",
            scheduleUrl = "https://www.iconfitness.co.il/tel-aviv/"
        ),
        Club(
            id = "netanya",
            name = "Netanya",
            nameHebrew = "נתניה",
            address = "נתניה",
            scheduleUrl = "https://www.iconfitness.co.il/netanya/"
        ),
        Club(
            id = "raanana",
            name = "Ra'anana",
            nameHebrew = "רעננה",
            address = "רעננה",
            scheduleUrl = "https://www.iconfitness.co.il/רעננה/"
        ),
        Club(
            id = "herzliya",
            name = "Herzliya",
            nameHebrew = "הרצליה",
            address = "הרצליה",
            scheduleUrl = "https://www.iconfitness.co.il/herzliya/"
        ),
        Club(
            id = "rishon",
            name = "Rishon LeZion",
            nameHebrew = "ראשון לציון",
            address = "ראשון לציון",
            scheduleUrl = "https://www.iconfitness.co.il/rishon/"
        ),
        Club(
            id = "petah-tikva",
            name = "Petah Tikva",
            nameHebrew = "פתח תקווה",
            address = "פתח תקווה",
            scheduleUrl = "https://www.iconfitness.co.il/petah-tikva/"
        ),
        Club(
            id = "haifa",
            name = "Haifa",
            nameHebrew = "חיפה",
            address = "חיפה",
            scheduleUrl = "https://www.iconfitness.co.il/haifa/"
        ),
        Club(
            id = "beer-sheva",
            name = "Beer Sheva",
            nameHebrew = "באר שבע",
            address = "באר שבע",
            scheduleUrl = "https://www.iconfitness.co.il/beer-sheva/"
        ),
        Club(
            id = "kfar-saba",
            name = "Kfar Saba",
            nameHebrew = "כפר סבא",
            address = "כפר סבא",
            scheduleUrl = "https://www.iconfitness.co.il/kfar-saba/"
        ),
        Club(
            id = "rehovot",
            name = "Rehovot",
            nameHebrew = "רחובות",
            address = "רחובות",
            scheduleUrl = "https://www.iconfitness.co.il/rehovot/"
        ),
        Club(
            id = "ashdod",
            name = "Ashdod",
            nameHebrew = "אשדוד",
            address = "אשדוד",
            scheduleUrl = "https://www.iconfitness.co.il/ashdod/"
        ),
        Club(
            id = "jerusalem",
            name = "Jerusalem",
            nameHebrew = "ירושלים",
            address = "ירושלים",
            scheduleUrl = "https://www.iconfitness.co.il/jerusalem/"
        ),
        Club(
            id = "modiin",
            name = "Modi'in",
            nameHebrew = "מודיעין",
            address = "מודיעין",
            scheduleUrl = "https://www.iconfitness.co.il/modiin/"
        ),
        Club(
            id = "bat-yam",
            name = "Bat Yam",
            nameHebrew = "בת ים",
            address = "בת ים",
            scheduleUrl = "https://www.iconfitness.co.il/bat-yam/"
        ),
        Club(
            id = "holon",
            name = "Holon",
            nameHebrew = "חולון",
            address = "חולון",
            scheduleUrl = "https://www.iconfitness.co.il/holon/"
        )
    )

    fun getClubById(id: String): Club? = clubs.find { it.id == id }
}
