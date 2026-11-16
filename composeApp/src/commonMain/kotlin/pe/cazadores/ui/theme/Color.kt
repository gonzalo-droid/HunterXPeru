package pe.cazadores.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens "Expediente cívico" (docs/kmp-migration/02-DISENO-UX.md §2).
 * El rojo `action` se reserva para llamar; la recompensa siempre lleva texto tinta.
 */
@Immutable
data class CazadoresColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val inkMuted: Color,
    val line: Color,
    /** Borde de controles: ≥ 3:1 contra `surface` (WCAG 1.4.11). */
    val lineStrong: Color,
    val action: Color,
    val onAction: Color,
    val reward: Color,
    val onReward: Color,
    /** Header oscuro de Inicio y bloque "Si lo reconoces": oscuro en ambos temas. */
    val header: Color,
    val onHeader: Color,
    val onHeaderMuted: Color,
    val headerField: Color,
    val levelExtremo: Color,
    val onLevelExtremo: Color,
    val levelMuyAlto: Color,
    val onLevelMuyAlto: Color,
    val levelAlto: Color,
    val onLevelAlto: Color,
    val photoPlaceholder: Color,
    val photoSilhouette: Color,
    val isDark: Boolean,
)

private val Ink = Color(0xFF16181B)
private val Reward = Color(0xFFF2B93B)

val LightCazadoresColors = CazadoresColors(
    bg = Color(0xFFF3F1EC),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFECE8E0),
    ink = Ink,
    inkMuted = Color(0xFF4A4F57),
    line = Color(0xFFDAD6CE),
    lineStrong = Color(0xFF7C8189),
    action = Color(0xFFB4121B),
    onAction = Color(0xFFFFFFFF),
    reward = Reward,
    onReward = Ink,
    header = Ink,
    onHeader = Color(0xFFFFFFFF),
    onHeaderMuted = Color(0xFFB9B5AD),
    headerField = Color(0xFF2A2D32),
    levelExtremo = Color(0xFF8E1B14),
    onLevelExtremo = Color(0xFFFFFFFF),
    levelMuyAlto = Color(0xFFB0480C),
    onLevelMuyAlto = Color(0xFFFFFFFF),
    levelAlto = Color(0xFFEADFB8),
    onLevelAlto = Color(0xFF3D3200),
    photoPlaceholder = Color(0xFFD9D5CC),
    photoSilhouette = Color(0xFFB9B4A9),
    isDark = false,
)

val DarkCazadoresColors = CazadoresColors(
    bg = Color(0xFF121315),
    surface = Color(0xFF1C1E21),
    surfaceAlt = Color(0xFF24272B),
    ink = Color(0xFFEDEBE6),
    inkMuted = Color(0xFFB9B5AD),
    line = Color(0xFF2E3136),
    lineStrong = Color(0xFF6E737B),
    action = Color(0xFFE5484D),
    // Texto tinta sobre el rojo claro del modo oscuro: 4,5:1 (blanco daría 3,9:1). Ver ContrastTest.
    onAction = Ink,
    reward = Reward,
    onReward = Ink,
    header = Color(0xFF0B0C0D),
    onHeader = Color(0xFFFFFFFF),
    onHeaderMuted = Color(0xFFB9B5AD),
    headerField = Color(0xFF24272B),
    levelExtremo = Color(0xFF8E1B14),
    onLevelExtremo = Color(0xFFFFFFFF),
    levelMuyAlto = Color(0xFFB0480C),
    onLevelMuyAlto = Color(0xFFFFFFFF),
    levelAlto = Color(0xFFEADFB8),
    onLevelAlto = Color(0xFF3D3200),
    photoPlaceholder = Color(0xFF2A2D32),
    photoSilhouette = Color(0xFF4A4F57),
    isDark = true,
)

val LocalCazadoresColors = staticCompositionLocalOf { LightCazadoresColors }
