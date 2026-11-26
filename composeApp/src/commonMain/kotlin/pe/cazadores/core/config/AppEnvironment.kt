package pe.cazadores.core.config

/** Datos del entorno que cada plataforma provee por Koin. */
data class AppEnvironment(
    val platformName: String,
    val isDebug: Boolean,
    /** Fabricante y modelo (Android) o tipo de equipo (iOS), para el correo de soporte. */
    val deviceModel: String = "",
    val osVersion: String = "",
) {
    /** User-Agent honesto: `CazadoresPeru/2.0.0 (Android)`. */
    val userAgent: String get() = "CazadoresPeru/${AppInfo.VERSION_NAME} ($platformName)"
}
