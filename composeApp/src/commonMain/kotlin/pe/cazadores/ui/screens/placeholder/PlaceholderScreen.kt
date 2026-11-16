package pe.cazadores.ui.screens.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CazadoresDimens

/** Temporal (fase 1): se reemplaza por cada pantalla real en la fase 4. */
@Composable
fun PlaceholderScreen(
    title: String,
    onOpenDetail: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(CazadoresDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title.uppercase(), style = Cz.type.headline, color = Cz.colors.ink)
        Text("Pantalla en construcción.", style = Cz.type.body, color = Cz.colors.inkMuted)
        onOpenDetail?.let { TextButton(onClick = { it("demo") }) { Text("Abrir ficha de prueba") } }
        onBack?.let { TextButton(onClick = it) { Text("Volver") } }
    }
}
