package pe.cazadores.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import pe.cazadores.ui.theme.Cz

/** Rótulo de sección en mayúsculas ("DELITOS IMPUTADOS"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = Cz.type.overline,
        color = Cz.colors.inkMuted,
        modifier = modifier.semantics { heading() },
    )
}

/** Título de pantalla condensado. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Cz.type.headline, color = Cz.colors.ink, modifier = modifier.semantics { heading() })
}
