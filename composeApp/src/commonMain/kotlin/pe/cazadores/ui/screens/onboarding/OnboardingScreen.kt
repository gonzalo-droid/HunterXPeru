package pe.cazadores.ui.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.domain.model.RewardClass
import pe.cazadores.resources.Res
import pe.cazadores.resources.legend_row_a
import pe.cazadores.resources.legend_row_b
import pe.cazadores.resources.legend_row_c
import pe.cazadores.resources.legend_row_s
import pe.cazadores.resources.poster_wanted
import pe.cazadores.resources.onboarding_1_body
import pe.cazadores.resources.onboarding_1_title
import pe.cazadores.resources.onboarding_2_body
import pe.cazadores.resources.onboarding_2_title
import pe.cazadores.resources.onboarding_3_body
import pe.cazadores.resources.onboarding_3_title
import pe.cazadores.resources.onboarding_3_unofficial
import pe.cazadores.resources.onboarding_next
import pe.cazadores.resources.onboarding_note_1
import pe.cazadores.resources.onboarding_rank_note
import pe.cazadores.resources.onboarding_note_2
import pe.cazadores.resources.onboarding_skip
import pe.cazadores.resources.onboarding_start
import pe.cazadores.resources.onboarding_step
import pe.cazadores.ui.components.BrandMark
import pe.cazadores.ui.components.RankBadge
import pe.cazadores.ui.components.StatusBarAppearance
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

private data class Step(val title: StringResource, val body: StringResource)

private val STEPS = listOf(
    Step(Res.string.onboarding_1_title, Res.string.onboarding_1_body),
    Step(Res.string.onboarding_2_title, Res.string.onboarding_2_body),
    Step(Res.string.onboarding_3_title, Res.string.onboarding_3_body),
)

/** Tres pasos sobre fondo tinta. El último insiste: no es oficial y nunca hay que enfrentarse. Sin anuncios. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    StatusBarAppearance(lightIcons = true)
    val pager = rememberPagerState { STEPS.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == STEPS.lastIndex

    Column(
        Modifier.fillMaxSize().background(Cz.colors.header).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandMark(32.dp)
            Text(
                stringResource(Res.string.onboarding_step, pager.currentPage + 1, STEPS.size).uppercase(),
                style = Cz.type.monoSmall,
                color = Cz.colors.onHeaderMuted,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            // En el último paso "Omitir" sobra, pero se conserva su hueco: la fila mide lo que el botón (48 dp, más
            // que la marca de 32 dp) y quitarlo subía todo 16 dp. Invisible, deshabilitado (ni clic ni foco) y fuera
            // de TalkBack/VoiceOver.
            TextButton(
                onClick = onFinish,
                enabled = !isLast,
                modifier = Modifier.defaultMinSize(minHeight = CazadoresDimens.touchTarget)
                    .then(if (isLast) Modifier.alpha(0f).clearAndSetSemantics {} else Modifier),
                colors = ButtonDefaults.textButtonColors(contentColor = Cz.colors.onHeader),
            ) { Text(stringResource(Res.string.onboarding_skip), style = Cz.type.bodyStrong) }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            StepPage(STEPS[page], page)
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            repeat(STEPS.size) { index ->
                val width by animateDpAsState(if (index == pager.currentPage) 28.dp else 8.dp)
                Box(
                    Modifier.height(8.dp).width(width).background(
                        if (index == pager.currentPage) Cz.colors.reward else Cz.colors.onHeaderMuted.copy(alpha = 0.5f),
                        CazadoresShapes.chip,
                    ),
                )
            }
        }

        Button(
            onClick = { if (isLast) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().height(CazadoresDimens.buttonHeightLarge),
            shape = CazadoresShapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = Cz.colors.onHeader, contentColor = Cz.colors.header),
        ) {
            Text(
                stringResource(if (isLast) Res.string.onboarding_start else Res.string.onboarding_next),
                style = Cz.type.button,
            )
        }
    }
}

@Composable
private fun StepPage(step: Step, page: Int) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        // Con letra muy grande la ilustración se omite: el texto manda y no debe quedar bajo el pliegue.
        val compact = LocalDensity.current.fontScale > 1.3f
        when {
            compact -> Unit
            page == 0 -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PosterIllustration() }
            page == 1 -> RankIllustration()
            else -> Box(
                Modifier.size(72.dp).background(Cz.colors.action, CazadoresShapes.cardLarge),
                contentAlignment = Alignment.Center,
            ) { Icon(CzIcons.Shield, contentDescription = null, tint = Cz.colors.onHeader, modifier = Modifier.size(36.dp)) }
        }
        Text(
            stringResource(step.title).uppercase(),
            style = Cz.type.headline,
            color = Cz.colors.onHeader,
            modifier = Modifier.semantics { heading() },
        )
        // El título es la línea de seguridad; el aviso de "no oficial" va justo debajo, antes del texto largo.
        if (page == STEPS.lastIndex) {
            Column(
                Modifier.fillMaxWidth().background(Cz.colors.headerField, CazadoresShapes.card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(Res.string.onboarding_3_unofficial), style = Cz.type.bodyStrong, color = Cz.colors.onHeader)
                Note("01", stringResource(Res.string.onboarding_note_1))
                Note("02", stringResource(Res.string.onboarding_note_2))
            }
        }
        Text(stringResource(step.body), style = Cz.type.body, color = Cz.colors.onHeaderMuted)
        Spacer(Modifier.height(8.dp))
    }
}

/** Cartel de muestra, sin datos reales: papel, "SE BUSCA", recuadro de foto y renglones. Solo ilustra. */
@Composable
private fun PosterIllustration() {
    Column(
        Modifier.widthIn(max = 220.dp).fillMaxWidth().clearAndSetSemantics {}
            .background(Cz.colors.paper, CazadoresShapes.cardLarge)
            .border(1.dp, Cz.colors.paperLine, CazadoresShapes.cardLarge)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(Res.string.poster_wanted), style = Cz.type.headline, color = Cz.colors.onPaper)
        Box(
            Modifier.fillMaxWidth(0.55f).aspectRatio(4f / 5f)
                .background(Cz.colors.paperPhoto, CazadoresShapes.photo)
                .border(1.dp, Cz.colors.paperLine, CazadoresShapes.photo),
        )
        Box(Modifier.fillMaxWidth(0.8f).height(8.dp).background(Cz.colors.paperLine, CazadoresShapes.badge))
        Box(Modifier.fillMaxWidth(0.5f).height(8.dp).background(Cz.colors.reward, CazadoresShapes.badge))
    }
}

/** Las cuatro clases con su umbral, en lugar de una ilustración abstracta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankIllustration() {
    Column(
        Modifier.fillMaxWidth().background(Cz.colors.headerField, CazadoresShapes.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RANKS.forEach { (rank, label) ->
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                RankBadge(rank)
                Text(stringResource(label), style = Cz.type.bodySmall, color = Cz.colors.onHeader)
            }
        }
        Text(stringResource(Res.string.onboarding_rank_note), style = Cz.type.caption, color = Cz.colors.onHeaderMuted)
    }
}

private val RANKS = listOf(
    RewardClass.S to Res.string.legend_row_s,
    RewardClass.A to Res.string.legend_row_a,
    RewardClass.B to Res.string.legend_row_b,
    RewardClass.C to Res.string.legend_row_c,
)

@Composable
private fun Note(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(number, style = Cz.type.mono, color = Cz.colors.reward)
        Text(text, style = Cz.type.bodySmall, color = Cz.colors.onHeader)
    }
}
