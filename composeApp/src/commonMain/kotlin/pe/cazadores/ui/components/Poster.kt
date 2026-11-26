package pe.cazadores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import pe.cazadores.resources.saved_delisted
import pe.cazadores.resources.saved_remove_action
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CzIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import pe.cazadores.core.DangerAssessment
import pe.cazadores.core.DangerLevelCalculator
import pe.cazadores.core.RewardClassCalculator
import pe.cazadores.ui.theme.CazadoresTheme
import pe.cazadores.core.RewardFormatter
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.RewardClass
import pe.cazadores.core.text.splitDisplayName
import androidx.compose.ui.graphics.Color
import pe.cazadores.resources.Res
import pe.cazadores.resources.poster_for
import pe.cazadores.resources.poster_more_crimes
import pe.cazadores.resources.poster_wanted
import pe.cazadores.resources.reward_cd
import pe.cazadores.resources.reward_none
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/**
 * Monto en una sola línea. Si no cabe baja de estilo (preferido -> `amountSmall` -> `caption`) y, como último
 * recurso, se parte en varias líneas: nunca se recortan dígitos.
 */
@Composable
internal fun PosterAmount(
    amount: Double,
    modifier: Modifier = Modifier,
    preferred: TextStyle = Cz.type.amount,
    color: Color = Cz.colors.onPaper,
) {
    val text = RewardFormatter.format(amount) ?: stringResource(Res.string.reward_none)
    val spoken = stringResource(Res.string.reward_cd, RewardFormatter.spoken(amount))
    val styles = listOf(preferred, Cz.type.amountSmall, Cz.type.caption).distinct()
    val fontScale = LocalDensity.current.fontScale
    var step by remember(text, fontScale, preferred) { mutableIntStateOf(0) }
    val last = step >= styles.lastIndex
    Text(
        text = text,
        style = styles[step.coerceAtMost(styles.lastIndex)],
        color = color,
        maxLines = if (last) Int.MAX_VALUE else 1,
        softWrap = last,
        overflow = TextOverflow.Visible,
        onTextLayout = { if (!last && it.hasVisualOverflow) step += 1 },
        modifier = modifier.semantics { contentDescription = spoken },
    )
}

/**
 * Cartel "SE BUSCA" (siempre sobre papel, igual en claro y oscuro). Sin alturas fijas en el texto; la foto
 * llega por [photo] y el cartel le da un marco con proporción fija.
 */
@Composable
fun WantedPoster(
    criminal: Criminal,
    assessment: DangerAssessment,
    rewardClass: RewardClass?,
    onRankClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    photo: @Composable () -> Unit,
) {
    val crime = assessment.headlineCrime ?: criminal.mainCrime
    val extra = criminal.crimes.size - 1
    Column(
        modifier
            .fillMaxWidth()
            .clip(CazadoresShapes.cardLarge)
            .background(Cz.colors.paper)
            .border(1.dp, Cz.colors.paperLine, CazadoresShapes.cardLarge)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(Res.string.poster_wanted),
            style = Cz.type.display,
            color = Cz.colors.onPaper,
            textAlign = TextAlign.Center,
        )
        if (crime.isNotBlank()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.poster_for, crime.uppercase()),
                    style = Cz.type.title,
                    color = Cz.colors.onPosterRed,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Cz.colors.posterRed, CazadoresShapes.badge)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                if (extra > 0) {
                    Text(
                        pluralStringResource(Res.plurals.poster_more_crimes, extra, extra),
                        style = Cz.type.caption,
                        color = Cz.colors.onPaperMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Box(
            Modifier
                .widthIn(max = 240.dp)
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(CazadoresShapes.photo)
                .background(Cz.colors.paperPhoto)
                .border(1.dp, Cz.colors.paperLine, CazadoresShapes.photo),
        ) { photo() }
        Text(
            criminal.displayName,
            style = Cz.type.headline,
            color = Cz.colors.onPaper,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            PosterAmount(criminal.reward)
            if (rewardClass != null) RankBadge(rewardClass, onClick = onRankClick)
        }
    }
}

/**
 * Cartel clavado para el tablón y Mis carteles (propuesta D, D24): foto con el sello S/A/B en la esquina,
 * apellidos y nombres, recompensa y delito (hasta 3 líneas, con filete vino). La clase C no lleva sello.
 */
@Composable
fun PosterMini(criminal: Criminal, onClick: () -> Unit, modifier: Modifier = Modifier, onRemove: (() -> Unit)? = null) {
    val removeLabel = stringResource(Res.string.saved_remove_action)
    val rewardClass = remember(criminal.reward) { RewardClassCalculator.of(criminal.reward) }?.takeIf { it != RewardClass.C }
    val name = remember(criminal.displayName) { criminal.displayName.splitDisplayName() }
    Column(
        modifier
            .clip(CazadoresShapes.card)
            .background(Cz.colors.paper)
            .border(1.dp, Cz.colors.paperLine, CazadoresShapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (onRemove != null) customActions = listOf(CustomAccessibilityAction(removeLabel) { onRemove(); true })
            }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            CriminalPhoto(
                criminal = criminal,
                contentDescription = null,
                full = false,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f),
            )
            // Sello opaco en la esquina superior izquierda: no tapa la cara ni el botón de quitar.
            if (rewardClass != null) RankBadge(rewardClass, Modifier.align(Alignment.TopStart).padding(6.dp))
            if (onRemove != null) {
                // Botón propio (no hijo fusionado): talla táctil mínima y contraste sobre cualquier foto.
                Box(
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(CazadoresDimens.touchTarget)
                        .clickable(role = Role.Button, onClick = onRemove)
                        .semantics { contentDescription = removeLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(28.dp).background(Cz.colors.paper, CircleShape).border(1.dp, Cz.colors.paperLine, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(CzIcons.Close, contentDescription = null, tint = Cz.colors.onPaper, modifier = Modifier.size(16.dp)) }
                }
            }
        }
        CardName(name, color = Cz.colors.onPaper, mutedColor = Cz.colors.onPaperMuted, small = true)
        // Texto y no solo color: el monto y el delito son los del último padrón en que figuró.
        if (criminal.delisted) {
            Text(stringResource(Res.string.saved_delisted), style = Cz.type.caption, color = Cz.colors.onPaperMuted)
        }
        PosterAmount(criminal.reward, preferred = Cz.type.amountSmall)
        val crime = remember(criminal) { DangerLevelCalculator.assess(criminal.crimes).headlineCrime } ?: criminal.mainCrime
        // El cartel es papel en ambos temas: el filete vino siempre se ve.
        CardCrime(crime, color = Cz.colors.onPaper, rule = Cz.colors.posterRed)
    }
}

/** Una fila de la cuadrícula de carteles (compartida por Inicio y Mis carteles). Si hay [onRemove], cada cartel lo muestra. */
@Composable
fun PosterGridRow(
    row: List<Criminal>,
    perRow: Int,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRemove: ((Criminal) -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.forEach { criminal ->
            PosterMini(
                criminal, { onOpenDetail(criminal.hash) }, Modifier.weight(1f).fillMaxHeight(),
                onRemove = onRemove?.let { remove -> { remove(criminal) } },
            )
        }
        // Un cartel suelto no debe estirarse a todo el ancho.
        repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

/** Cuadrícula no perezosa para listas cortas (tablón de Inicio). Las largas usan [PosterGridRow] en un LazyColumn. */
@Composable
fun PosterGrid(items: List<Criminal>, perRow: Int, onOpenDetail: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(perRow).forEach { row -> PosterGridRow(row, perRow, onOpenDetail) }
    }
}

@Preview(widthDp = 320)
@Composable
private fun WantedPosterLongPreview() {
    CazadoresTheme {
        val criminal = Criminal(
            id = 1, hash = "p1", displayName = "GONZALO EDUARDO MONTEVERDE BUSSALLEU", reward = 500_000.0,
            crimes = listOf("VIOLACIÓN SEXUAL DE MENOR DE EDAD", "ROBO AGRAVADO", "EXTORSIÓN"),
        )
        WantedPoster(criminal, DangerLevelCalculator.assess(criminal.crimes), RewardClassCalculator.of(criminal.reward), {}) {
            CriminalPhoto("p1", null, null, Modifier.fillMaxSize())
        }
    }
}

@Preview(widthDp = 320)
@Composable
private fun WantedPosterNoRewardPreview() {
    CazadoresTheme {
        val criminal = Criminal(id = 2, hash = "p2", displayName = "JUAN PÉREZ", reward = 0.0, crimes = listOf("HURTO"))
        WantedPoster(criminal, DangerLevelCalculator.assess(criminal.crimes), RewardClassCalculator.of(criminal.reward), null) {
            CriminalPhoto("p2", null, null, Modifier.fillMaxSize())
        }
    }
}
