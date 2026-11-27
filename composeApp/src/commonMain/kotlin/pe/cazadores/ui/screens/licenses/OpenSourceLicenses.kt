package pe.cazadores.ui.screens.licenses

/** Una pieza que la app incluye y su licencia. [url] lleva al texto de la licencia o a los términos. */
data class LicenseEntry(val name: String, val author: String, val license: String, val url: String)

private const val APACHE_2 = "Apache License 2.0"
private const val APACHE_2_URL = "https://www.apache.org/licenses/LICENSE-2.0"
private const val OFL = "SIL Open Font License 1.1"

/**
 * Lo que se distribuye dentro de la app (no las herramientas de build ni las de test).
 * Mantener al día con `gradle/libs.versions.toml`, los paquetes SPM de `iosApp` y `composeResources/font`.
 */
val OPEN_SOURCE_LICENSES = listOf(
    LicenseEntry("Kotlin, kotlinx.coroutines, kotlinx.serialization, kotlinx-datetime", "JetBrains", APACHE_2, APACHE_2_URL),
    LicenseEntry("Compose Multiplatform (runtime, UI, Material 3, recursos)", "JetBrains y Google", APACHE_2, APACHE_2_URL),
    LicenseEntry("AndroidX: Activity, Lifecycle, Navigation, Room, SQLite, DataStore", "Google", APACHE_2, APACHE_2_URL),
    LicenseEntry("Ktor", "JetBrains", APACHE_2, APACHE_2_URL),
    LicenseEntry("OkHttp", "Square", APACHE_2, APACHE_2_URL),
    LicenseEntry("Koin", "Kotzilla", APACHE_2, APACHE_2_URL),
    LicenseEntry("Firebase iOS SDK (Crashlytics)", "Google", APACHE_2, APACHE_2_URL),
    LicenseEntry("Archivo", "Omnibus-Type", OFL, "https://openfontlicense.org/open-font-license-official-text/"),
    LicenseEntry("IBM Plex Mono", "IBM", OFL, "https://openfontlicense.org/open-font-license-official-text/"),
)

/** SDK que no son de código abierto: se usan bajo los términos de su proveedor. */
val THIRD_PARTY_SDKS = listOf(
    LicenseEntry("Google Mobile Ads SDK y User Messaging Platform", "Google", "Términos de Google AdMob", "https://admob.google.com/home/terms-of-service/"),
    LicenseEntry("Firebase Crashlytics para Android", "Google", "Términos de Firebase", "https://firebase.google.com/terms"),
)
