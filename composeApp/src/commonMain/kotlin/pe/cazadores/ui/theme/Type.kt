package pe.cazadores.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import pe.cazadores.resources.Res
import pe.cazadores.resources.archivo
import pe.cazadores.resources.ibm_plex_mono_regular
import pe.cazadores.resources.ibm_plex_mono_semibold

/** Archivo es variable (ejes `wght` y `wdth`): una familia normal y otra condensada al 70 %. */
@Composable
private fun archivoFamily(width: Float, weights: List<FontWeight>): FontFamily = FontFamily(
    weights.map { weight ->
        Font(
            resource = Res.font.archivo,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.width(width),
            ),
        )
    },
)

@Immutable
data class CazadoresTypography(
    /** Nombres en ficha, títulos de pantalla. Condensada 800, en mayúsculas. */
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    /** Montos de recompensa. */
    val amount: TextStyle,
    val amountSmall: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val bodySmall: TextStyle,
    val button: TextStyle,
    /** Rótulos de sección en mayúsculas con tracking. */
    val overline: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
)

@Composable
fun cazadoresTypography(): CazadoresTypography {
    val body = archivoFamily(100f, listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold))
    val condensed = archivoFamily(70f, listOf(FontWeight.ExtraBold))
    val mono = FontFamily(
        Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal),
        Font(Res.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
    )
    return CazadoresTypography(
        display = TextStyle(fontFamily = condensed, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 34.sp),
        headline = TextStyle(fontFamily = condensed, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 30.sp),
        title = TextStyle(fontFamily = condensed, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 22.sp),
        amount = TextStyle(fontFamily = condensed, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 30.sp),
        amountSmall = TextStyle(fontFamily = condensed, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 20.sp),
        body = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyStrong = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
        button = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
        overline = TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.08.em),
        caption = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
        mono = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        monoSmall = TextStyle(fontFamily = mono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.1.em),
    )
}

internal fun CazadoresTypography.toMaterial(): Typography = Typography(
    displaySmall = display,
    headlineMedium = headline,
    titleLarge = title,
    titleMedium = bodyStrong,
    bodyLarge = body,
    bodyMedium = bodySmall,
    bodySmall = caption,
    labelLarge = button,
    labelMedium = caption,
    labelSmall = caption,
)

val LocalCazadoresTypography = staticCompositionLocalOf<CazadoresTypography> {
    error("CazadoresTypography no provista: envuelve la UI en CazadoresTheme")
}
