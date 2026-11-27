package pe.cazadores.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.ads.AdBanner
import pe.cazadores.ads.AdPlacement
import pe.cazadores.core.DateFormatter
import pe.cazadores.core.RewardClassCalculator
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.core.text.titleCase
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.RewardPosition
import pe.cazadores.domain.model.Sex
import pe.cazadores.resources.Res
import pe.cazadores.resources.detail_crimes
import pe.cazadores.resources.detail_regime_body
import pe.cazadores.resources.detail_regime_cta
import pe.cazadores.resources.detail_department
import pe.cazadores.resources.detail_if_you_see
import pe.cazadores.resources.detail_if_you_see_body
import pe.cazadores.resources.detail_photo_cd
import pe.cazadores.resources.detail_province
import pe.cazadores.resources.detail_sex
import pe.cazadores.resources.detail_sex_female
import pe.cazadores.resources.detail_sex_male
import pe.cazadores.resources.detail_source
import pe.cazadores.resources.detail_position
import pe.cazadores.resources.detail_position_rank
import pe.cazadores.resources.detail_position_tied
import pe.cazadores.resources.detail_position_tied_one
import pe.cazadores.resources.source_note
import pe.cazadores.resources.detail_unknown
import pe.cazadores.ui.components.CriminalPhoto
import pe.cazadores.ui.components.WantedPoster
import pe.cazadores.ui.components.OfflineBanner
import pe.cazadores.ui.components.SectionLabel
import pe.cazadores.ui.components.SkeletonBlock
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

@Composable
internal fun DetailBody(content: DetailContent.Loaded, onOpenLegend: () -> Unit, onOpenRegime: () -> Unit) {
    val c = content.criminal
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = CazadoresDimens.screenPadding).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (content.fromCache) OfflineBanner(content.dataAtMillis)

        WantedPoster(
            criminal = c,
            assessment = content.danger,
            rewardClass = RewardClassCalculator.of(c.reward),
            onRankClick = onOpenLegend,
        ) {
            CriminalPhoto(
                criminal = c,
                contentDescription = stringResource(Res.string.detail_photo_cd, c.displayName),
                modifier = Modifier.fillMaxSize(),
                shape = CazadoresShapes.photo,
            )
        }

        content.position?.let { PositionCard(it) }

        if (content.specialRegime) RegimeNotice(onOpenRegime)

        if (c.crimes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(stringResource(Res.string.detail_crimes))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    c.crimes.forEach { crime ->
                        Text(
                            crime.sentenceCase(),
                            style = Cz.type.bodyStrong,
                            color = Cz.colors.ink,
                            modifier = Modifier.background(Cz.colors.surface, CazadoresShapes.badge)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        DataRows(c)

        Column(
            Modifier.fillMaxWidth().background(Cz.colors.header, CazadoresShapes.card).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(Res.string.detail_if_you_see).uppercase(),
                style = Cz.type.title,
                color = Cz.colors.onHeader,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(Res.string.detail_if_you_see_body), style = Cz.type.bodySmall, color = Cz.colors.onHeader)
        }

        Text(
            stringResource(Res.string.detail_source, DateFormatter.date(content.dataAtMillis)),
            style = Cz.type.caption,
            color = Cz.colors.inkMuted,
        )
        Text(stringResource(Res.string.source_note), style = Cz.type.caption, color = Cz.colors.inkMuted)
        AdBanner(AdPlacement.DETAIL_BANNER, Modifier.fillMaxWidth())
    }
}

/** Puesto nacional por recompensa, sobre papel como el cartel (papel en ambos temas). */
@Composable
internal fun PositionCard(position: RewardPosition) {
    Column(
        Modifier.fillMaxWidth().background(Cz.colors.paper, CazadoresShapes.card)
            .border(1.dp, Cz.colors.paperLine, CazadoresShapes.card).padding(14.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(Res.string.detail_position).uppercase(), style = Cz.type.overline, color = Cz.colors.onPaperMuted)
        Text(
            stringResource(
                Res.string.detail_position_rank,
                position.rank.toString(),
                position.total.toString(),
                position.topPercent,
            ),
            style = Cz.type.bodyStrong,
            color = Cz.colors.onPaper,
        )
        when (position.tiedWith) {
            0 -> Unit
            1 -> Text(stringResource(Res.string.detail_position_tied_one), style = Cz.type.bodySmall, color = Cz.colors.onPaperMuted)
            else -> Text(stringResource(Res.string.detail_position_tied, position.tiedWith), style = Cz.type.bodySmall, color = Cz.colors.onPaperMuted)
        }
    }
}

/** Aviso con borde punteado rojo: solo si algún delito es extorsión o sicariato. Abre la pantalla del régimen. */
@Composable
internal fun RegimeNotice(onClick: () -> Unit) {
    val red = Cz.colors.posterRed
    val density = LocalDensity.current
    Column(
        Modifier.fillMaxWidth()
            .clip(CazadoresShapes.card)
            .background(Cz.colors.paper)
            .drawBehind {
                val stroke = with(density) { 2.dp.toPx() }
                drawRoundRect(
                    color = red,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(with(density) { 14.dp.toPx() }),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(Res.string.detail_regime_body), style = Cz.type.bodyStrong, color = Cz.colors.onPaper)
        Text(
            stringResource(Res.string.detail_regime_cta),
            style = Cz.type.bodySmall,
            color = Cz.colors.onPaper,
            textDecoration = TextDecoration.Underline,
        )
    }
}

@Composable
internal fun DataRows(c: Criminal) {
    val unknown = stringResource(Res.string.detail_unknown)
    val rows = listOf(
        stringResource(Res.string.detail_department) to (c.departamento?.titleCase() ?: unknown),
        stringResource(Res.string.detail_province) to (c.provincia?.titleCase() ?: unknown),
        stringResource(Res.string.detail_sex) to when (c.sex) {
            Sex.MALE -> stringResource(Res.string.detail_sex_male)
            Sex.FEMALE -> stringResource(Res.string.detail_sex_female)
            null -> unknown
        },
    )
    Column(Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card)) {
        rows.forEachIndexed { index, (label, value) ->
            // FlowRow: con letra grande el valor baja de línea en vez de cortarse.
            FlowRow(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = Cz.type.body, color = Cz.colors.inkMuted)
                Text(value, style = Cz.type.bodyStrong, color = Cz.colors.ink)
            }
            if (index < rows.lastIndex) HorizontalDivider(color = Cz.colors.line, modifier = Modifier.padding(horizontal = 14.dp))
        }
    }
}

@Composable
internal fun DetailSkeleton() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = CazadoresDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(420.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(72.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(80.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(150.dp))
    }
}
