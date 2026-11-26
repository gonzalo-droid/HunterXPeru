# kotlinx.serialization: conservar serializers generados.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# Ktor
-dontwarn org.slf4j.**
-dontwarn io.ktor.**

# DataStore Preferences: su protobuf interno lee los campos de GeneratedMessageLite por reflexión.
# Sin esto, R8 (modo completo) los elimina y la primera lectura de preferencias nunca emite.
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Crashlytics: conservar archivo y línea para que el mapping de R8 dé trazas legibles.
-keepattributes SourceFile,LineNumberTable
