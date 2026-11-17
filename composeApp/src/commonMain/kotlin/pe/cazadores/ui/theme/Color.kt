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
    /** Cartel "Se busca": siempre papel, idéntico en claro y oscuro. */
    val paper: Color,
    val onPaper: Color,
    val onPaperMuted: Color,
    /** Sin texto: filetes y marcos sobre `paper`. */
    val paperLine: Color,
    /** Sin texto: fondo de la foto sobre `paper`. */
    val paperPhoto: Color,
    val posterRed: Color,
    val onPosterRed: Color,
    val rankS: Color,
    val onRankS: Color,
    val rankA: Color,
    val onRankA: Color,
    val rankB: Color,
    val onRankB: Color,
    val rankC: Color,
    val onRankC: Color,
    val isDark: Boolean,
)

private val Ink = Color(0xFF16181B)
private val Reward = Color(0xFFF2B93B)

// Tokens del cartel: mismos valores en ambos temas (el cartel es siempre papel).
private val Paper = Color(0xFFEFE4CC)
private val OnPaper = Color(0xFF2B1D10)
private val OnPaperMuted = Color(0xFF5A4630)
private val PaperLine = Color(0xFFCDBF9F)
private val PaperPhoto = Color(0xFFDCCFB0)
private val PosterRed = Color(0xFF7A1D12)
private val OnPosterRed = Color(0xFFFFF3E6)
private val RankB = Color(0xFFB0480C)
private val RankC = Color(0xFFEADFB8)

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
    paper = Paper,
    onPaper = OnPaper,
    onPaperMuted = OnPaperMuted,
    paperLine = PaperLine,
    paperPhoto = PaperPhoto,
    posterRed = PosterRed,
    onPosterRed = OnPosterRed,
    rankS = OnPaper,
    onRankS = Reward,
    rankA = PosterRed,
    onRankA = OnPosterRed,
    rankB = RankB,
    onRankB = OnPosterRed,
    rankC = RankC,
    onRankC = Color(0xFF3D3200),
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
    paper = Paper,
    onPaper = OnPaper,
    onPaperMuted = OnPaperMuted,
    paperLine = PaperLine,
    paperPhoto = PaperPhoto,
    posterRed = PosterRed,
    onPosterRed = OnPosterRed,
    rankS = OnPaper,
    onRankS = Reward,
    rankA = PosterRed,
    onRankA = OnPosterRed,
    rankB = RankB,
    onRankB = OnPosterRed,
    rankC = RankC,
    onRankC = Color(0xFF3D3200),
    isDark = true,
)

val LocalCazadoresColors = staticCompositionLocalOf { LightCazadoresColors }
