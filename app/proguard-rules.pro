# Oorbitt Launcher ProGuard Rules
-keepattributes *Annotation*

# Keep Koin
-keep class org.koin.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Suppress StringConcatFactory warnings
-dontwarn java.lang.invoke.StringConcatFactory
