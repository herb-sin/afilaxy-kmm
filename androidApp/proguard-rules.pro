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

# Keep only the Kotlin reflection and coroutine internals that are accessed via reflection.
# Avoid blanket -keep class kotlin.** { *; } — it defeats R8 shrinking on all stdlib code.
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class kotlin.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep class kotlinx.serialization.** { *; }
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

# Remove logging in release (android.util.Log)
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** wtf(...);
}

# Logger KMM compartilhado — strip completo no release (auditoria 2026-04)
-assumenosideeffects class com.afilaxy.util.Logger {
    public static *** d(...);
    public static *** i(...);
    public static *** v(...);
    public static *** w(...);
    public static *** e(...);
}

-assumenosideeffects class com.afilaxy.app.performance.LogOptimizer {
    public static *** d(...);
    public static *** i(...);
    public static *** e(...);
}

# FileLogger — strip completo no release: isEnabled garante no-op, ProGuard remove chamadas
# (auditoria pré-produção 2026-04)
-assumenosideeffects class com.afilaxy.util.FileLogger {
    public static *** log(...);
}
# Preservar initialize() e getAllLogs() que podem ser invocados indiretamente
-keep class com.afilaxy.util.FileLogger { void initialize(android.content.Context); }

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

# Ktor + OkHttp: SLF4J não existe no Android (logging interno do OkHttp)
-dontwarn org.slf4j.**

# Ktor: classes opcionais não presentes no runtime Android
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
