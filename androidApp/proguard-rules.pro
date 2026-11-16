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
