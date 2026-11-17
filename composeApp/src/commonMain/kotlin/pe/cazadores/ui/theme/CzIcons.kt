package pe.cazadores.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Íconos de trazo de 24 × 24 (los mismos de los mockups). Se dibujan con `Color.Black`
 * y se tiñen con `Icon(tint = …)`.
 */
object CzIcons {
    val Home by lazy { stroke("home", "M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z") }
    val Search by lazy { stroke("search", "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z", "M20 20l-3.5-3.5") }
    val Bookmark by lazy { stroke("bookmark", "M6 3h12v18l-6-4-6 4z") }
    val BookmarkFilled by lazy { filled("bookmark_filled", "M6 3h12v18l-6-4-6 4z") }
    val Settings by lazy {
        stroke(
            "settings",
            "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z",
            "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
        )
    }
    val Phone by lazy {
        stroke(
            "phone",
            "M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.7a2 2 0 0 1-.5 2.1L8 9.8a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.7.7a2 2 0 0 1 1.7 2z",
        )
    }
    val Share by lazy { stroke("share", "M12 3v13", "M7 8l5-5 5 5", "M5 14v6h14v-6") }
    val Back by lazy { stroke("back", "M15 18l-6-6 6-6") }
    val Filter by lazy { stroke("filter", "M4 6h16", "M7 12h10", "M10 18h4") }
    val Close by lazy { stroke("close", "M6 6l12 12", "M18 6L6 18") }
    val ChevronDown by lazy { stroke("chevron_down", "M6 9l6 6 6-6") }
    val ChevronRight by lazy { stroke("chevron_right", "M9 6l6 6-6 6") }
    val Edit by lazy { stroke("edit", "M4 20h4L19 9l-4-4L4 16z") }
    val Shield by lazy { stroke("shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z", "M9 12l2 2 4-4") }
    val Eye by lazy { stroke("eye", "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z", "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z") }
    val WifiOff by lazy {
        stroke("wifi_off", "M2 2l20 20", "M8.5 16.5a5 5 0 0 1 7 0", "M5 12.9a10 10 0 0 1 5.2-2.7", "M19 12.9a10 10 0 0 0-2.4-1.6", "M2 8.8a15 15 0 0 1 4.2-2.6", "M22 8.8A15 15 0 0 0 11 5", "M12 20h.01")
    }
    val Refresh by lazy { stroke("refresh", "M21 12a9 9 0 1 1-2.6-6.4", "M21 3v6h-6") }
    val Alert by lazy { stroke("alert", "M12 9v4", "M12 17h.01", "M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z") }
    val Info by lazy { stroke("info", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z", "M12 11v5", "M12 8h.01") }
    val External by lazy { stroke("external", "M14 4h6v6", "M20 4l-9 9", "M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5") }
    val Star by lazy { stroke("star", "M12 3l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3 6.4 20.2l1.1-6.2L3 9.6l6.2-.9z") }
    val Mail by lazy { stroke("mail", "M3 5h18v14H3z", "M3 6l9 7 9-7") }

    private fun stroke(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { data ->
                addPath(
                    pathData = addPathNodes(data),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun filled(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { data ->
                addPath(
                    pathData = addPathNodes(data),
                    fill = SolidColor(Color.Black),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
