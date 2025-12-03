# Icon Fitness Schedule - לוח שיעורים

אפליקציית אנדרואיד להצגת לוח שיעורי הסטודיו ברשת Icon Fitness.

## תכונות

- 📅 **לוח שיעורים שבועי** - צפייה בכל שיעורי הסטודיו לפי ימים
- ⭐ **מועדפים** - סימון שיעורים אהובים לגישה מהירה
- 🔍 **חיפוש** - חיפוש לפי שם שיעור, מדריך או חדר
- 🏢 **בחירת מועדון** - תמיכה במספר סניפים של Icon Fitness
- 💾 **מטמון חכם** - שמירת נתונים ל-4 שעות עם רענון ברקע
- 🔄 **Stale-While-Revalidate** - הצגת נתונים ישנים בזמן טעינת נתונים חדשים
- 🌙 **תמיכה ב-Dark Mode** - מצב כהה אוטומטי
- ➡️ **תמיכה ב-RTL** - ממשק בעברית מימין לשמאל

## סניפים נתמכים

- תל אביב
- נתניה
- רעננה
- הרצליה
- ראשון לציון
- פתח תקווה
- חיפה
- באר שבע
- כפר סבא
- רחובות
- אשדוד
- ירושלים
- מודיעין
- בת ים
- חולון

## התקנה

### הורדת APK
1. עבור ל-[Releases](../../releases)
2. הורד את קובץ ה-APK האחרון
3. התקן על מכשיר האנדרואיד שלך

### בניה מקומית
```bash
# Clone the repository
git clone https://github.com/yourusername/apk3.git
cd apk3

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## ארכיטקטורה

- **Kotlin** - שפת התכנות
- **Jetpack Compose** - UI מודרני
- **Hilt** - Dependency Injection
- **Coroutines & Flow** - תכנות אסינכרוני
- **DataStore** - שמירת העדפות ומטמון
- **OkHttp + JSoup** - שליפת נתונים מהאתר

## מבנה הפרויקט

```
app/src/main/java/com/iconfit/schedule/
├── data/
│   ├── model/          # מודלים של נתונים
│   ├── local/          # CacheManager
│   ├── remote/         # ScheduleFetcher
│   └── repository/     # ScheduleRepository
├── di/                 # Hilt modules
├── ui/
│   ├── components/     # Composable components
│   ├── screens/        # מסכים ראשיים
│   ├── theme/          # עיצוב
│   └── viewmodel/      # ViewModel
├── MainActivity.kt
└── IconFitApplication.kt
```

## רישיון

MIT License

## תודות

- [Icon Fitness](https://iconfitness.co.il) - רשת חדרי הכושר
