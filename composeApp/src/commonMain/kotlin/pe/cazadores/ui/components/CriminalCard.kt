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
import pe.cazadores.core.RewardClassCalculator
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.domain.model.Criminal
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/** Fila de lista de Buscar: foto 72×92, nombre (2 líneas), delito titular (1), sello de rango y monto. */
@Composable
fun CriminalCard(criminal: Criminal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val crime = DangerLevelCalculator.assess(criminal.crimes).headlineCrime ?: criminal.mainCrime
    val rewardClass = RewardClassCalculator.of(criminal.reward)
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (crime.isNotBlank()) {
                // Franja `posterRed` con texto `onPosterRed` (4,5:1 en ambos temas): el rojo suelto sobre la
                // superficie oscura queda en 1,6:1 y `action` en 4,26:1.
                Text(
                    crime.sentenceCase(),
                    style = Cz.type.caption,
                    color = Cz.colors.onPosterRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(Cz.colors.posterRed, CazadoresShapes.badge).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            // FlowRow: con letra grande el sello y el monto pasan a dos líneas en vez de cortarse.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (rewardClass != null) RankBadge(rewardClass)
                RewardBadge(criminal.reward)
            }
        }
    }
}
