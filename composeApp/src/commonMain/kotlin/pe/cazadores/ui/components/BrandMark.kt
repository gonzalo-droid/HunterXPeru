package pe.cazadores.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.cazadores.ui.theme.Cz

/**
 * Símbolo de la marca (lupa con silueta y marco de enfoque, docs/brand) en su variante crema:
 * azulejo `paper` con trazo `onPaper` y marco `posterRed`. Como el cartel, es papel en ambos temas,
 * así que se lee igual sobre la cabecera oscura y no depende del tema (ver ContrastTest).
 * Decorativo: va siempre junto a un texto con el nombre de la app o el título de la pantalla.
 */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    val glyph = Cz.colors.onPaper
    val accent = Cz.colors.posterRed
    val vector = remember(glyph, accent) { brandVector(glyph, accent) }
    Box(
        modifier.size(size).background(Cz.colors.paper, RoundedCornerShape(size * 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(vector, contentDescription = null, modifier = Modifier.fillMaxSize().padding(size * 0.12f))
    }
}

/** Mismas rutas que el SVG de 1024 de diseño, recortadas a la caja del símbolo (x, y entre 111 y 891). */
private fun brandVector(glyph: Color, accent: Color): ImageVector =
    ImageVector.Builder("brand_mark", 24.dp, 24.dp, 780f, 780f).apply {
        group(translationX = -111f, translationY = -111f) {
            addPath(
                addPathNodes("M705,705L850,850"),
                stroke = SolidColor(glyph), strokeLineWidth = 72f, strokeLineCap = StrokeCap.Round,
            )
            addPath(
                addPathNodes("M145,475a330,330 0,1 1,660 0a330,330 0,1 1,-660 0"),
                stroke = SolidColor(glyph), strokeLineWidth = 58f,
            )
            addPath(
                addPathNodes(
                    "M354,290H305Q290,290 290,305V354M596,290H645Q660,290 660,305V354" +
                        "M290,596V645Q290,660 305,660H354M660,596V645Q660,660 645,660H596",
                ),
                stroke = SolidColor(accent), strokeLineWidth = 30f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
            addPath(
                addPathNodes(
                    "M406,420a69,69 0,1 1,138 0a69,69 0,1 1,-138 0z" +
                        "M343,581C343,512 402,485 475,485C548,485 607,512 607,581Q607,597 591,597H359Q343,597 343,581Z",
                ),
                fill = SolidColor(glyph),
            )
        }
    }.build()
