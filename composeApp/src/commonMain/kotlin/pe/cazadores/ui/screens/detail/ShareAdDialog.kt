package pe.cazadores.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.resources.Res
import pe.cazadores.resources.share_ad_body
import pe.cazadores.resources.share_ad_cancel
import pe.cazadores.resources.share_ad_confirm
import pe.cazadores.resources.share_ad_title
import pe.cazadores.ui.components.PrimaryButton
import pe.cazadores.ui.components.TextAction
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/** Aviso antes del anuncio bonificado de Compartir (D33). */
@Composable
internal fun ShareAdDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Cz.colors.surface,
        shape = CazadoresShapes.card,
        title = { Text(stringResource(Res.string.share_ad_title), style = Cz.type.title, color = Cz.colors.ink) },
        text = { Text(stringResource(Res.string.share_ad_body), style = Cz.type.body, color = Cz.colors.ink) },
        // Botones apilados (primario arriba): con letra grande no caben lado a lado.
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PrimaryButton(stringResource(Res.string.share_ad_confirm), onConfirm, Modifier.fillMaxWidth())
                TextAction(stringResource(Res.string.share_ad_cancel), onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}
