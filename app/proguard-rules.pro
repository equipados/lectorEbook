# Readium
-keep class org.readium.** { *; }
-keep class com.google.cloud.texttospeech.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
