package com.iconfit.schedule.data

import com.iconfit.schedule.data.model.Club

/**
 * List of Icon Fitness clubs in Israel organized by region
 * Website URLs link to official schedule pages on iconfitness.co.il
 * Schedule URLs link directly to PDF schedules when known
 */
object ClubsData {

    private const val BASE_URL = "https://www.iconfitness.co.il"
    private const val PDF_BASE = "https://www.iconfitness.co.il/wp-content/uploads"

    // North - צפון
    val northClubs = listOf(
        Club(id = "beit-shean", name = "Beit She'an", nameHebrew = "אייקון בית שאן", region = "צפון",
            websiteUrl = "$BASE_URL/beit-shean/", hebrewSlug = "בית-שאן"),
        Club(id = "tiberias", name = "Tiberias", nameHebrew = "אייקון טבריה", region = "צפון",
            websiteUrl = "$BASE_URL/tiberias/", hebrewSlug = "טבריה"),
        Club(id = "karmiel", name = "Karmiel", nameHebrew = "אייקון כרמיאל", region = "צפון",
            websiteUrl = "$BASE_URL/karmiel/", hebrewSlug = "כרמיאל"),
        Club(id = "maalot", name = "Ma'alot", nameHebrew = "אייקון מעלות", region = "צפון",
            websiteUrl = "$BASE_URL/maalot/", hebrewSlug = "מעלות"),
        Club(id = "nahariya", name = "Nahariya", nameHebrew = "אייקון נהריה", region = "צפון",
            websiteUrl = "$BASE_URL/nahariya/", hebrewSlug = "נהריה"),
        Club(id = "nesher", name = "Nesher", nameHebrew = "אייקון נשר", region = "צפון",
            websiteUrl = "$BASE_URL/nesher/", hebrewSlug = "נשר"),
        Club(id = "akko", name = "Akko", nameHebrew = "אייקון עכו", region = "צפון",
            websiteUrl = "$BASE_URL/akko/", hebrewSlug = "עכו"),
        Club(id = "afula", name = "Afula", nameHebrew = "אייקון עפולה", region = "צפון",
            websiteUrl = "$BASE_URL/afula/", hebrewSlug = "עפולה"),
        Club(id = "kiryat-motzkin", name = "Kiryat Motzkin", nameHebrew = "אייקון קרית מוצקין", region = "צפון",
            websiteUrl = "$BASE_URL/kiryat-motzkin/", hebrewSlug = "קרית-מוצקין"),
        Club(id = "kiryat-bialik", name = "Kiryat Bialik", nameHebrew = "אייקון קרית ביאליק", region = "צפון",
            websiteUrl = "$BASE_URL/kiryat-bialik/", hebrewSlug = "קרית-ביאליק"),
        Club(id = "zichron-yaakov", name = "Zichron Ya'akov", nameHebrew = "אייקון זכרון יעקב", region = "צפון",
            websiteUrl = "$BASE_URL/zichron-yaakov/", hebrewSlug = "זכרון-יעקב")
    )

    // Center - מרכז
    val centerClubs = listOf(
        Club(id = "or-akiva", name = "Or Akiva", nameHebrew = "אייקון אור עקיבא", region = "מרכז",
            websiteUrl = "$BASE_URL/or-akiva/", hebrewSlug = "אור-עקיבא"),
        Club(id = "bat-yam", name = "Bat Yam", nameHebrew = "אייקון בת-ים", region = "מרכז",
            websiteUrl = "$BASE_URL/bat-yam/", hebrewSlug = "בת-ים"),
        Club(id = "hod-hasharon", name = "Hod HaSharon", nameHebrew = "אייקון הוד השרון", region = "מרכז",
            websiteUrl = "$BASE_URL/hod-hasharon/", hebrewSlug = "הוד-השרון"),
        Club(id = "herzliya", name = "Herzliya", nameHebrew = "אייקון הרצליה", region = "מרכז",
            websiteUrl = "$BASE_URL/herzliya/", hebrewSlug = "הרצליה",
            scheduleUrl = "$PDF_BASE/2023/05/HERZLIYA_05-23.pdf"),
        Club(id = "hadera", name = "Hadera", nameHebrew = "אייקון חדרה", region = "מרכז",
            websiteUrl = "$BASE_URL/hadera/", hebrewSlug = "חדרה",
            scheduleUrl = "$PDF_BASE/2023/06/hadera-06-23-1.pdf"),
        Club(id = "hadera-beach", name = "Hadera Beach", nameHebrew = "חדרה מול החוף", region = "מרכז",
            websiteUrl = "$BASE_URL/hedera-mol/", hebrewSlug = "חדרה-מול-החוף"),
        Club(id = "holon", name = "Holon", nameHebrew = "אייקון חולון", region = "מרכז",
            websiteUrl = "$BASE_URL/holon/", hebrewSlug = "חולון"),
        Club(id = "yavne", name = "Yavne", nameHebrew = "אייקון יבנה", region = "מרכז",
            websiteUrl = "$BASE_URL/yavne/", hebrewSlug = "יבנה"),
        Club(id = "yehud", name = "Yehud", nameHebrew = "אייקון יהוד", region = "מרכז",
            websiteUrl = "$BASE_URL/yehud/", hebrewSlug = "יהוד"),
        Club(id = "jerusalem", name = "Jerusalem", nameHebrew = "אייקון ירושלים", region = "מרכז",
            websiteUrl = "$BASE_URL/jerusalem/", hebrewSlug = "ירושלים"),
        Club(id = "kfar-yona", name = "Kfar Yona", nameHebrew = "אייקון כפר יונה", region = "מרכז",
            websiteUrl = "$BASE_URL/kfar-yona/", hebrewSlug = "כפר-יונה"),
        Club(id = "kfar-saba", name = "Kfar Saba", nameHebrew = "אייקון כפר סבא", region = "מרכז",
            websiteUrl = "$BASE_URL/kfar-saba/", hebrewSlug = "כפר-סבא"),
        Club(id = "modiin", name = "Modi'in", nameHebrew = "אייקון מודיעין", region = "מרכז",
            websiteUrl = "$BASE_URL/מודיעין/", hebrewSlug = "מודיעין"),
        Club(id = "pardes-hana", name = "Pardes Hana", nameHebrew = "אייקון פרדס חנה", region = "מרכז",
            websiteUrl = "$BASE_URL/pardes-hana/", hebrewSlug = "פרדס-חנה"),
        Club(id = "petah-tikva-segula", name = "Petah Tikva Segula", nameHebrew = "אייקון פ\"ת סגולה", region = "מרכז",
            websiteUrl = "$BASE_URL/sgula/", hebrewSlug = "פתח-תקווה-סגולה"),
        Club(id = "rishon-lezion", name = "Rishon LeZion", nameHebrew = "אייקון ראשון לציון", region = "מרכז",
            websiteUrl = "$BASE_URL/rishon-lezion/", hebrewSlug = "ראשון-לציון",
            scheduleUrl = "$PDF_BASE/2024/02/ראשון-לציון-02.2024-מעודכן.pdf"),
        Club(id = "rehovot", name = "Rehovot", nameHebrew = "אייקון רחובות", region = "מרכז",
            websiteUrl = "$BASE_URL/rehovot/", hebrewSlug = "רחובות",
            scheduleUrl = "$PDF_BASE/2023/11/סטודיו-נוב-23-רחובות-copy.pdf"),
        Club(id = "ramla", name = "Ramla", nameHebrew = "אייקון רמלה", region = "מרכז",
            websiteUrl = "$BASE_URL/ramla/", hebrewSlug = "רמלה"),
        Club(id = "raanana", name = "Ra'anana", nameHebrew = "אייקון רעננה", region = "מרכז",
            websiteUrl = "$BASE_URL/רעננה/", hebrewSlug = "רעננה",
            scheduleUrl = "$PDF_BASE/2024/03/RAANANA-03-24.pdf"),
        Club(id = "netanya", name = "Netanya", nameHebrew = "אייקון נתניה", region = "מרכז",
            websiteUrl = "$BASE_URL/netanya/", hebrewSlug = "נתניה"),
        Club(id = "tel-aviv-shalom", name = "Tel Aviv Shalom Tower", nameHebrew = "אייקון תל אביב מגדל השלום", region = "מרכז",
            websiteUrl = "$BASE_URL/tel-aviv/", hebrewSlug = "תל-אביב-מגדל-שלום"),
        Club(id = "tel-aviv-ben-yehuda", name = "Tel Aviv Ben Yehuda", nameHebrew = "אייקון תל אביב בן יהודה", region = "מרכז",
            websiteUrl = "$BASE_URL/tel-aviv/", hebrewSlug = "תל-אביב-בן-יהודה"),
        Club(id = "tel-aviv-weizmann", name = "Tel Aviv Weizmann", nameHebrew = "אייקון תל אביב ויצמן", region = "מרכז",
            websiteUrl = "$BASE_URL/tel-aviv/", hebrewSlug = "תל-אביב-ויצמן"),
        Club(id = "tel-aviv-neot-afeka", name = "Tel Aviv Neot Afeka", nameHebrew = "אייקון תל אביב נאות אפקה", region = "מרכז",
            websiteUrl = "$BASE_URL/tel-aviv/", hebrewSlug = "תל-אביב-נאות-אפקה",
            scheduleUrl = "$PDF_BASE/2024/02/afeka-03-24.pdf")
    )

    // South - דרום
    val southClubs = listOf(
        Club(id = "eilat", name = "Eilat", nameHebrew = "אייקון אילת", region = "דרום",
            websiteUrl = "$BASE_URL/eilat/", hebrewSlug = "אילת"),
        Club(id = "ashdod", name = "Ashdod", nameHebrew = "אייקון אשדוד", region = "דרום",
            websiteUrl = "$BASE_URL/ashdod/", hebrewSlug = "אשדוד"),
        Club(id = "ashkelon-barnea", name = "Ashkelon Barnea", nameHebrew = "אייקון אשקלון ברנע", region = "דרום",
            websiteUrl = "$BASE_URL/ashkelon/", hebrewSlug = "אשקלון-ברנע"),
        Club(id = "ashkelon-city", name = "Ashkelon City", nameHebrew = "אייקון אשקלון סיטי", region = "דרום",
            websiteUrl = "$BASE_URL/ashkelon/", hebrewSlug = "אשקלון-סיטי"),
        Club(id = "beer-sheva", name = "Beer Sheva", nameHebrew = "אייקון באר-שבע", region = "דרום",
            websiteUrl = "$BASE_URL/beer-sheva/", hebrewSlug = "באר-שבע"),
        Club(id = "beer-sheva-park", name = "Beer Sheva Park", nameHebrew = "אייקון באר-שבע הפארק", region = "דרום",
            websiteUrl = "$BASE_URL/beer-sheva/", hebrewSlug = "באר-שבע-הפארק"),
        Club(id = "dimona", name = "Dimona", nameHebrew = "אייקון דימונה", region = "דרום",
            websiteUrl = "$BASE_URL/dimona/", hebrewSlug = "דימונה"),
        Club(id = "netivot", name = "Netivot", nameHebrew = "אייקון נתיבות", region = "דרום",
            websiteUrl = "$BASE_URL/netivot/", hebrewSlug = "נתיבות"),
        Club(id = "kiryat-gat", name = "Kiryat Gat", nameHebrew = "אייקון קרית גת", region = "דרום",
            websiteUrl = "$BASE_URL/kiryat-gat/", hebrewSlug = "קרית-גת"),
        Club(id = "sderot", name = "Sderot", nameHebrew = "אייקון שדרות", region = "דרום",
            websiteUrl = "$BASE_URL/sderot/", hebrewSlug = "שדרות")
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

    // Official Icon Fitness app package for deep linking
    const val ICON_APP_PACKAGE = "il.co.offline.iconfitness"
}
