# Add project specific ProGuard rules here.

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.iconfit.schedule.**$$serializer { *; }
-keepclassmembers class com.iconfit.schedule.** {
    *** Companion;
}
-keepclasseswithmembers class com.iconfit.schedule.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# JSoup
-keep class org.jsoup.** { *; }
-keeppackagenames org.jsoup.nodes
-dontwarn org.jspecify.annotations.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
