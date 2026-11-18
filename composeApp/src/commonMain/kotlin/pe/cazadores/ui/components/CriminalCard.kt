package pe.cazadores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.cazadores.core.DangerLevelCalculator
import pe.cazadores.core.RewardClassCalculator
import pe.cazadores.core.text.SplitName
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.core.text.splitDisplayName
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.RewardClass
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/**
 * Fila de lista de Buscar (propuesta D, D24): foto 72×92; apellidos y nombres; recompensa con el sello S/A/B
 * al lado; delito titular hasta 3 líneas. La clase C (la gran mayoría) no lleva sello en la tarjeta.
 */
@Composable
fun CriminalCard(criminal: Criminal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val crime = remember(criminal) { DangerLevelCalculator.assess(criminal.crimes).headlineCrime } ?: criminal.mainCrime
    val rewardClass = remember(criminal.reward) { RewardClassCalculator.of(criminal.reward) }?.takeIf { it != RewardClass.C }
    val name = remember(criminal.displayName) { criminal.displayName.splitDisplayName() }
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
            criminal = criminal,
            contentDescription = null,
            full = false,
            modifier = Modifier.size(width = 72.dp, height = 92.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CardName(name, color = Cz.colors.ink, mutedColor = Cz.colors.inkMuted, small = false)
            // FlowRow: con letra grande el sello baja a la línea siguiente en vez de apretar el monto.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                PosterAmount(criminal.reward, preferred = Cz.type.amountSmall, color = Cz.colors.ink)
                if (rewardClass != null) RankBadge(rewardClass)
            }
            // En oscuro el vino sobre `surface` casi no se ve: el filete usa `action` (solo decorativo).
            CardCrime(crime, color = Cz.colors.ink, rule = if (Cz.colors.isDark) Cz.colors.action else Cz.colors.posterRed)
        }
    }
}

/** Apellidos fuertes y, debajo, los nombres en tono suave. Sin coma en el nombre, un solo bloque. */
@Composable
internal fun CardName(name: SplitName, color: Color, mutedColor: Color, small: Boolean) {
    Column {
        Text(
            name.surnames,
            style = Cz.type.title,
            color = color,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        name.givenNames?.let {
            Text(
                it,
                style = if (small) Cz.type.caption else Cz.type.bodySmall,
                color = mutedColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Delito titular como texto (sin franja de color), hasta 3 líneas, con un filete a la izquierda. */
@Composable
internal fun CardCrime(crime: String, color: Color, rule: Color) {
    if (crime.isBlank()) return
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(rule, CazadoresShapes.badge))
        Text(
            crime.sentenceCase(),
            style = Cz.type.caption,
            color = color,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
