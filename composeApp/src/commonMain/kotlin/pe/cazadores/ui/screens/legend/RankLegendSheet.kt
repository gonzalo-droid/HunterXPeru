package pe.cazadores.ui.screens.legend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.domain.model.RewardClass
import pe.cazadores.resources.Res
import pe.cazadores.resources.legend_danger_note
import pe.cazadores.resources.legend_intro
import pe.cazadores.resources.legend_row_a
import pe.cazadores.resources.legend_row_b
import pe.cazadores.resources.legend_row_c
import pe.cazadores.resources.legend_row_s
import pe.cazadores.resources.legend_title
import pe.cazadores.resources.rank_label
import pe.cazadores.ui.components.RankBadge
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/** Umbrales provisionales de cada clase (texto, no cálculo: el cálculo vive en `RewardClassCalculator`). */
private val ROWS: List<Pair<RewardClass, StringResource>> = listOf(
    RewardClass.S to Res.string.legend_row_s,
    RewardClass.A to Res.string.legend_row_a,
    RewardClass.B to Res.string.legend_row_b,
    RewardClass.C to Res.string.legend_row_c,
)

/** Leyenda de rangos. Hoja inferior sobre papel, igual en claro y oscuro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankLegendSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Cz.colors.paper,
        contentColor = Cz.colors.onPaper,
        shape = CazadoresShapes.sheet,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = CazadoresDimens.screenPadding).navigationBarsPadding().padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(Res.string.legend_title),
                style = Cz.type.headline,
                color = Cz.colors.onPaper,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(Res.string.legend_intro), style = Cz.type.bodySmall, color = Cz.colors.onPaperMuted)
            ROWS.forEach { (rewardClass, threshold) ->
                // FlowRow: con letra grande el umbral baja de línea en vez de cortarse.
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    RankBadge(rewardClass)
                    Text(
                        stringResource(Res.string.rank_label, rewardClass.name) + " · " + stringResource(threshold),
                        style = Cz.type.bodyStrong,
                        color = Cz.colors.onPaper,
                    )
                }
            }
            Text(stringResource(Res.string.legend_danger_note), style = Cz.type.caption, color = Cz.colors.onPaperMuted)
        }
    }
}
