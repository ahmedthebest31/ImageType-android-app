# Moshi JSON serialization
-keep class com.ahmedsamy.imagetype.util.Template { *; }
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class * extends com.squareup.moshi.JsonAdapter$Factory

# Moshi Kotlin support (reflection-based adapter)
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# Moshi KotlinJsonAdapterFactory
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory { *; }

# Keep Moshi type adapters generated for model classes
-keep class **JsonAdapter { *; }
-keepclassmembers class * {
    static **JsonAdapter FACTORY;
}
