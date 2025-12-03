package com.iconfit.schedule.data

import com.iconfit.schedule.data.model.Club

/**
 * List of Icon Fitness clubs in Israel organized by region
 */
object ClubsData {

    // North - צפון
    val northClubs = listOf(
        Club(id = "beit-shean", name = "Beit She'an", nameHebrew = "אייקון בית שאן", region = "צפון"),
        Club(id = "tiberias", name = "Tiberias", nameHebrew = "אייקון טבריה", region = "צפון"),
        Club(id = "karmiel", name = "Karmiel", nameHebrew = "אייקון כרמיאל", region = "צפון"),
        Club(id = "maalot", name = "Ma'alot", nameHebrew = "אייקון מעלות", region = "צפון"),
        Club(id = "nahariya", name = "Nahariya", nameHebrew = "אייקון נהריה", region = "צפון"),
        Club(id = "nesher", name = "Nesher", nameHebrew = "אייקון נשר", region = "צפון"),
        Club(id = "akko", name = "Akko", nameHebrew = "אייקון עכו", region = "צפון"),
        Club(id = "afula", name = "Afula", nameHebrew = "אייקון עפולה", region = "צפון"),
        Club(id = "kiryat-motzkin", name = "Kiryat Motzkin", nameHebrew = "אייקון קרית מוצקין", region = "צפון"),
        Club(id = "kiryat-bialik", name = "Kiryat Bialik", nameHebrew = "אייקון קרית ביאליק", region = "צפון"),
        Club(id = "zichron-yaakov", name = "Zichron Ya'akov", nameHebrew = "אייקון זכרון יעקב", region = "צפון")
    )

    // Center - מרכז
    val centerClubs = listOf(
        Club(id = "or-akiva", name = "Or Akiva", nameHebrew = "אייקון אור עקיבא", region = "מרכז"),
        Club(id = "bat-yam", name = "Bat Yam", nameHebrew = "אייקון בת-ים", region = "מרכז"),
        Club(id = "hod-hasharon", name = "Hod HaSharon", nameHebrew = "אייקון הוד השרון", region = "מרכז"),
        Club(id = "herzliya", name = "Herzliya", nameHebrew = "אייקון הרצליה", region = "מרכז"),
        Club(id = "hadera", name = "Hadera", nameHebrew = "אייקון חדרה", region = "מרכז"),
        Club(id = "hadera-beach", name = "Hadera Beach", nameHebrew = "חדרה מול החוף", region = "מרכז"),
        Club(id = "holon", name = "Holon", nameHebrew = "אייקון חולון", region = "מרכז"),
        Club(id = "yavne", name = "Yavne", nameHebrew = "אייקון יבנה", region = "מרכז"),
        Club(id = "yehud", name = "Yehud", nameHebrew = "אייקון יהוד", region = "מרכז"),
        Club(id = "jerusalem", name = "Jerusalem", nameHebrew = "אייקון ירושלים", region = "מרכז"),
        Club(id = "kfar-yona", name = "Kfar Yona", nameHebrew = "אייקון כפר יונה", region = "מרכז"),
        Club(id = "kfar-saba", name = "Kfar Saba", nameHebrew = "אייקון כפר סבא", region = "מרכז"),
        Club(id = "modiin", name = "Modi'in", nameHebrew = "אייקון מודיעין", region = "מרכז"),
        Club(id = "pardes-hana", name = "Pardes Hana", nameHebrew = "אייקון פרדס חנה", region = "מרכז"),
        Club(id = "petah-tikva-segula", name = "Petah Tikva Segula", nameHebrew = "אייקון פ\"ת סגולה", region = "מרכז"),
        Club(id = "rishon-lezion", name = "Rishon LeZion", nameHebrew = "אייקון ראשון לציון", region = "מרכז"),
        Club(id = "rehovot", name = "Rehovot", nameHebrew = "אייקון רחובות", region = "מרכז"),
        Club(id = "ramla", name = "Ramla", nameHebrew = "אייקון רמלה", region = "מרכז"),
        Club(id = "raanana-gav-yam", name = "Ra'anana Gav Yam", nameHebrew = "אייקון רעננה גב ים", region = "מרכז"),
        Club(id = "raanana-lev-hapark", name = "Ra'anana Lev HaPark", nameHebrew = "אייקון רעננה לב הפארק", region = "מרכז"),
        Club(id = "netanya", name = "Netanya", nameHebrew = "אייקון נתניה", region = "מרכז"),
        Club(id = "tel-aviv-shalom", name = "Tel Aviv Shalom Tower", nameHebrew = "אייקון תל אביב מגדל השלום", region = "מרכז"),
        Club(id = "tel-aviv-ben-yehuda", name = "Tel Aviv Ben Yehuda", nameHebrew = "אייקון תל אביב בן יהודה", region = "מרכז"),
        Club(id = "tel-aviv-weizmann", name = "Tel Aviv Weizmann", nameHebrew = "אייקון תל אביב ויצמן", region = "מרכז"),
        Club(id = "tel-aviv-neot-afeka", name = "Tel Aviv Neot Afeka", nameHebrew = "אייקון תל אביב נאות אפקה", region = "מרכז")
    )

    // South - דרום
    val southClubs = listOf(
        Club(id = "eilat", name = "Eilat", nameHebrew = "אייקון אילת", region = "דרום"),
        Club(id = "ashdod", name = "Ashdod", nameHebrew = "אייקון אשדוד", region = "דרום"),
        Club(id = "ashkelon-barnea", name = "Ashkelon Barnea", nameHebrew = "אייקון אשקלון ברנע", region = "דרום"),
        Club(id = "ashkelon-city", name = "Ashkelon City", nameHebrew = "אייקון אשקלון סיטי", region = "דרום"),
        Club(id = "beer-sheva", name = "Beer Sheva", nameHebrew = "אייקון באר-שבע", region = "דרום"),
        Club(id = "beer-sheva-park", name = "Beer Sheva Park", nameHebrew = "אייקון באר-שבע הפארק", region = "דרום"),
        Club(id = "dimona", name = "Dimona", nameHebrew = "אייקון דימונה", region = "דרום"),
        Club(id = "netivot", name = "Netivot", nameHebrew = "אייקון נתיבות", region = "דרום"),
        Club(id = "kiryat-gat", name = "Kiryat Gat", nameHebrew = "אייקון קרית גת", region = "דרום"),
        Club(id = "sderot", name = "Sderot", nameHebrew = "אייקון שדרות", region = "דרום")
    )

    val clubs = northClubs + centerClubs + southClubs

    val regions = listOf("צפון", "מרכז", "דרום")

    val clubsByRegion = mapOf(
        "צפון" to northClubs,
        "מרכז" to centerClubs,
        "דרום" to southClubs
    )

    fun getClubById(id: String): Club? = clubs.find { it.id == id }

    fun getClubsByRegion(region: String): List<Club> = clubsByRegion[region] ?: emptyList()
}
