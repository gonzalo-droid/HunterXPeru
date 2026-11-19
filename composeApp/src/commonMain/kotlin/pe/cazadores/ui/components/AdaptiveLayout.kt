package pe.cazadores.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import pe.cazadores.ui.theme.CazadoresDimens

/** Margen lateral: el de siempre en teléfonos; en pantallas anchas, el que centra [CazadoresDimens.contentMaxWidth]. */
fun contentGutter(availableWidth: Dp): Dp =
    max(CazadoresDimens.screenPadding, (availableWidth - CazadoresDimens.contentMaxWidth) / 2)

/** Carteles por fila: 1 con letra muy grande; 3 si el contenido mide ≥ 560 dp; si no, 2. */
fun posterColumnsFor(contentWidth: Dp, fontScale: Float): Int = when {
    fontScale > 1.3f -> 1
    contentWidth >= 560.dp -> 3
    else -> 2
}

@Composable
fun posterColumns(contentWidth: Dp): Int = posterColumnsFor(contentWidth, LocalDensity.current.fontScale)

/** Mide el ancho disponible y entrega a [content] el margen lateral y el ancho de contenido resultante. */
@Composable
fun AdaptiveWidth(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(gutter: Dp, contentWidth: Dp) -> Unit,
) {
    BoxWithConstraints(modifier) {
        // Ancho sin límite (p. ej. dentro de un scroll horizontal): no hay nada que centrar.
        val gutter = if (maxWidth == Dp.Infinity) CazadoresDimens.screenPadding else contentGutter(maxWidth)
        content(gutter, maxWidth - gutter * 2)
    }
}
