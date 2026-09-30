# ============================================================================
# Правила R8/ProGuard для релизной сборки «Интернет Радио».
# Room / Hilt / Media3 / Coil / OkHttp поставляют собственные consumer-правила —
# здесь только то, что нужно вручную: kotlinx.serialization и Retrofit.
# ============================================================================

# Атрибуты, без которых ломаются дженерики Retrofit и сериализация.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# ---- kotlinx.serialization ----
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
# Для каждого @Serializable-класса держим его сгенерированный $$serializer.
-if @kotlinx.serialization.Serializable class **
-keep class <1>$$serializer { *; }
# DTO радио-браузера сериализуются по именам полей — держим целиком.
-keep class com.tohn95.internetradio.data.remote.**$$serializer { *; }
-keepclassmembers class com.tohn95.internetradio.data.remote.** {
    <fields>;
    <init>(...);
}

# ---- Retrofit ----
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
# Интерфейс API читается рефлексией по аннотациям — держим методы.
-keep interface com.tohn95.internetradio.data.remote.RadioBrowserApi { *; }

# ---- OkHttp / Okio (на всякий случай — подавляем предупреждения) ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
