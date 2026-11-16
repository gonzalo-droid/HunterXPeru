package pe.cazadores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.cazadores.core.DangerLevelCalculator
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.domain.model.Criminal
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/** Fila de lista (Buscar, Guardados): foto 72×92, nombre condensado, delito, recompensa y nivel. */
@Composable
fun CriminalCard(criminal: Criminal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val level = DangerLevelCalculator.of(criminal.crimes)
    Row(
        modifier
            .fillMaxWidth()
            .clip(CazadoresShapes.card)
            .background(Cz.colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CriminalPhoto(
            cacheKey = criminal.hash,
            base64 = criminal.photoBase64,
            contentDescription = null,
            modifier = Modifier.size(width = 72.dp, height = 92.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                criminal.displayName,
                style = Cz.type.title,
                color = Cz.colors.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (criminal.mainCrime.isNotBlank()) {
                Text(
                    criminal.crimes.joinToString(" · ") { it.sentenceCase() },
                    style = Cz.type.caption,
                    color = Cz.colors.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // FlowRow: con letra grande la recompensa y el nivel pasan a dos líneas en vez de cortarse.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                RewardBadge(criminal.reward)
                DangerBadge(level)
            }
        }
    }
}
