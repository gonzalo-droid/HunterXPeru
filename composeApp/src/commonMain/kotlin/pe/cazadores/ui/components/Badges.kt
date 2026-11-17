package pe.cazadores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.core.DangerAssessment
import pe.cazadores.core.RewardFormatter
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.domain.model.DangerLevel
import pe.cazadores.domain.model.RewardClass
import pe.cazadores.resources.Res
import pe.cazadores.resources.danger_alto
import pe.cazadores.resources.danger_cd
import pe.cazadores.resources.danger_extremo
import pe.cazadores.resources.danger_meter_base
import pe.cazadores.resources.danger_meter_by
import pe.cazadores.resources.danger_meter_unofficial
import pe.cazadores.resources.danger_muy_alto
import pe.cazadores.resources.rank_cd
import pe.cazadores.resources.reward_cd
import pe.cazadores.resources.reward_none
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/** Recompensa sobre amarillo, siempre con texto tinta. */
@Composable
fun RewardBadge(amount: Double, modifier: Modifier = Modifier, style: TextStyle = Cz.type.amountSmall) {
    val text = RewardFormatter.format(amount) ?: stringResource(Res.string.reward_none)
    val spoken = stringResource(Res.string.reward_cd, RewardFormatter.spoken(amount))
    Text(
        text = text,
        style = style,
        color = Cz.colors.onReward,
        maxLines = 1,
        modifier = modifier
            .semantics { contentDescription = spoken }
            .background(Cz.colors.reward, CazadoresShapes.badge)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun dangerLabel(level: DangerLevel): String = stringResource(
    when (level) {
        DangerLevel.EXTREMO -> Res.string.danger_extremo
        DangerLevel.MUY_ALTO -> Res.string.danger_muy_alto
        DangerLevel.ALTO -> Res.string.danger_alto
    },
)

/** Nivel con texto y nº de marcas ▲, no solo color. */
@Composable
fun DangerBadge(level: DangerLevel, modifier: Modifier = Modifier) {
    val (bg, fg) = when (level) {
        DangerLevel.EXTREMO -> Cz.colors.levelExtremo to Cz.colors.onLevelExtremo
        DangerLevel.MUY_ALTO -> Cz.colors.levelMuyAlto to Cz.colors.onLevelMuyAlto
        DangerLevel.ALTO -> Cz.colors.levelAlto to Cz.colors.onLevelAlto
    }
    val label = dangerLabel(level)
    val description = stringResource(Res.string.danger_cd, label)
    Text(
        text = "▲".repeat(level.marks) + " " + label.uppercase(),
        style = Cz.type.overline,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .semantics { contentDescription = description }
            .background(bg, CazadoresShapes.badge)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Colores (fondo, texto) de cada clase de recompensa. */
@Composable
private fun rankColors(rewardClass: RewardClass): Pair<Color, Color> = when (rewardClass) {
    RewardClass.S -> Cz.colors.rankS to Cz.colors.onRankS
    RewardClass.A -> Cz.colors.rankA to Cz.colors.onRankA
    RewardClass.B -> Cz.colors.rankB to Cz.colors.onRankB
    RewardClass.C -> Cz.colors.rankC to Cz.colors.onRankC
}

/**
 * Sello de clase "S ★★★★" (estimada por la app). Con [onClick] muestra ⓘ y es un botón de 48 dp mínimo
 * (abre la leyenda).
 */
@Composable
fun RankBadge(rewardClass: RewardClass, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val (bg, fg) = rankColors(rewardClass)
    val description = stringResource(Res.string.rank_cd, rewardClass.name, rewardClass.stars)
    var base = modifier
    if (onClick != null) {
        base = base.minimumInteractiveComponentSize().clickable(role = Role.Button, onClick = onClick)
    }
    Box(base.semantics(mergeDescendants = true) { contentDescription = description }, contentAlignment = Alignment.Center) {
        Text(
            text = rewardClass.name + " " + "★".repeat(rewardClass.stars) + if (onClick != null) " ⓘ" else "",
            style = Cz.type.overline,
            color = fg,
            modifier = Modifier
                .background(bg, CazadoresShapes.badge)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * Medidor de peligrosidad estimada: 3 barras (nº de marcas = nivel), etiqueta, delito determinante
 * (o nivel base) y aviso "no oficial". Pensado para ir sobre papel.
 */
@Composable
fun DangerMeter(assessment: DangerAssessment, modifier: Modifier = Modifier) {
    val level = assessment.level
    val filled = Cz.colors.posterRed
    val label = dangerLabel(level)
    val description = stringResource(Res.string.danger_cd, label)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(if (index < level.marks) filled else Cz.colors.paperLine, CazadoresShapes.badge),
                )
            }
        }
        Text(label.uppercase(), style = Cz.type.overline, color = Cz.colors.onPaper)
        val headline = assessment.headlineCrime
        if (assessment.matched && headline != null) {
            Text(
                stringResource(Res.string.danger_meter_by, headline.sentenceCase()),
                style = Cz.type.caption,
                color = Cz.colors.onPaperMuted,
            )
        } else {
            Text(stringResource(Res.string.danger_meter_base), style = Cz.type.caption, color = Cz.colors.onPaperMuted)
        }
        Text(stringResource(Res.string.danger_meter_unofficial), style = Cz.type.caption, color = Cz.colors.onPaperMuted)
    }
}
