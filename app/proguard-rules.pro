# Keep Firebase and Web3J reflection-safe types
-keep class org.web3j.** { *; }
-dontwarn org.web3j.**

# Keep your data models to prevent Firebase deserialization crashes
-keep class com.shaplachottor.app.models.** { *; }

# Firebase Specific Rules
-keepattributes *Annotation*
-keepattributes Signature
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# AndroidX Navigation
-keep class androidx.navigation.** { *; }

# Meta (Facebook) SDK
-keep class com.facebook.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.facebook.**

