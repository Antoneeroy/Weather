# Keep Open-Meteo & Geocoding DTO models serialized with Moshi
-keepclassmembers class com.example.OpenMeteo* {
    <fields>;
}
-keep class com.example.OpenMeteo* { *; }
-keepclassmembers class com.example.Geocoding* {
    <fields>;
}
-keep class com.example.Geocoding* { *; }

# Moshi Kotlin reflection and adapters
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# WorkManager Worker classes
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Preserve line numbers for readable stack traces in production
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

