# ProGuard Rules - Afilaxy KMM
#
# Firebase, GMS, Compose, Koin, Ktor e AndroidX empacotam consumer-rules.pro próprios via AAR.
# NÃO adicionar -keep class genérico para essas libs — anula o R8.

# ── KMM ViewModels e domínio (acessados via reflection pelo Koin e KMM bridge) ──────────────
-keep class com.rickclephas.kmm.viewmodel.** { *; }
-keep class com.afilaxy.presentation.** { *; }
-keep class com.afilaxy.domain.model.** { *; }
-keepclassmembers class com.afilaxy.domain.model.** { *; }
-keep interface com.afilaxy.domain.repository.** { *; }

# ── GitLive Firebase wrapper (KMM) — usa reflection para decodificar documentos Firestore ───
-keep class dev.gitlive.firebase.** { *; }
-keepclassmembers class dev.gitlive.firebase.** { *; }

# ── kotlinx.serialization ────────────────────────────────────────────────────────────────────
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ── Kotlin reflection e coroutines ──────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class kotlin.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ── Fix Compose snapshot lock verification (crash reproduzível sem este keep) ───────────────
-keep class androidx.compose.runtime.snapshots.** { *; }
-keepclassmembers class androidx.compose.runtime.snapshots.SnapshotStateList {
    boolean conditionalUpdate(boolean, kotlin.jvm.functions.Function1);
    java.lang.Object mutate(kotlin.jvm.functions.Function1);
    void update(boolean, kotlin.jvm.functions.Function1);
}

# ── Strip de logging no release ─────────────────────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** wtf(...);
}
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
-assumenosideeffects class com.afilaxy.util.FileLogger {
    public static *** log(...);
}
-keep class com.afilaxy.util.FileLogger { void initialize(android.content.Context); }

# ── Otimização R8 ────────────────────────────────────────────────────────────────────────────
# !field/* e !class/merging/* preservados: reflection via Koin/Firestore quebra sem eles.
# Simplificações aritméticas e de cast habilitadas (seguras).
-optimizations !field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# ── Suppress warnings de dependências opcionais ─────────────────────────────────────────────
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
