package pe.cazadores.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
        Text(text, style = Cz.type.button, textAlign = TextAlign.Center)
    }
}

/** Rojo: reservado para llamar a la línea. */
@Composable
fun CallButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, minHeight: Dp = CazadoresDimens.buttonHeightLarge) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        shape = CazadoresShapes.button,
        colors = ButtonDefaults.buttonColors(containerColor = Cz.colors.action, contentColor = Cz.colors.onAction),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) { ButtonContent(text, icon) }
}

/** Acción principal neutra (tinta sobre papel). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = CazadoresDimens.buttonHeightLarge),
        shape = CazadoresShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = Cz.colors.ink,
            contentColor = Cz.colors.bg,
            disabledContainerColor = Cz.colors.line,
            disabledContentColor = Cz.colors.inkMuted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) { ButtonContent(text, icon) }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = Cz.colors.ink,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = CazadoresDimens.buttonHeight),
        shape = CazadoresShapes.button,
        border = BorderStroke(1.5.dp, contentColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) { ButtonContent(text, icon) }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Cz.colors.ink) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = CazadoresDimens.touchTarget),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) { Text(text, style = Cz.type.bodyStrong) }
}
