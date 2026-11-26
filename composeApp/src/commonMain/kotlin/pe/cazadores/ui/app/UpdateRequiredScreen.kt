package pe.cazadores.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.update_required_action
import pe.cazadores.resources.update_required_body
import pe.cazadores.resources.update_required_title
import pe.cazadores.ui.components.MessageState
import pe.cazadores.ui.components.PrimaryButton
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

/** El padrón publicado pide un build más nuevo (UPD-02): única salida, ir a la tienda. */
@Composable
fun UpdateRequiredScreen(actions: ExternalActions = koinInject()) {
    Box(Modifier.fillMaxSize().background(Cz.colors.bg).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        MessageState(
            icon = CzIcons.Refresh,
            title = stringResource(Res.string.update_required_title),
            body = stringResource(Res.string.update_required_body),
            action = { PrimaryButton(text = stringResource(Res.string.update_required_action), onClick = actions::openStorePage) },
        )
    }
}
