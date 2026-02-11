# ProGuard Rules - Afilaxy KMM

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class dev.gitlive.firebase.** { *; }

# Keep Koin classes
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# Keep Kotlin Multiplatform
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Keep Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.runtime.snapshots.**
-keep class androidx.compose.runtime.snapshots.** { *; }

# Fix Compose lock verification
-keepclassmembers class androidx.compose.runtime.snapshots.SnapshotStateList {
    boolean conditionalUpdate(boolean, kotlin.jvm.functions.Function1);
    java.lang.Object mutate(kotlin.jvm.functions.Function1);
    void update(boolean, kotlin.jvm.functions.Function1);
}

# Keep KMM ViewModels
-keep class com.rickclephas.kmm.viewmodel.** { *; }
-keep class com.afilaxy.presentation.** { *; }

# Keep Domain models
-keep class com.afilaxy.domain.model.** { *; }
-keepclassmembers class com.afilaxy.domain.model.** { *; }

# Keep Repository interfaces
-keep interface com.afilaxy.domain.repository.** { *; }

# Keep Data classes
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

-assumenosideeffects class com.afilaxy.app.performance.LogOptimizer {
    public static *** d(...);
    public static *** i(...);
}

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Firebase Analytics
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Suppress warnings
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
