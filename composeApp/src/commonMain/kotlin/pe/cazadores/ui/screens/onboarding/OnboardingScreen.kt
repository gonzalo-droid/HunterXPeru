package pe.cazadores.ui.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.resources.Res
import pe.cazadores.resources.onboarding_1_body
import pe.cazadores.resources.onboarding_1_title
import pe.cazadores.resources.onboarding_2_body
import pe.cazadores.resources.onboarding_2_title
import pe.cazadores.resources.onboarding_3_body
import pe.cazadores.resources.onboarding_3_title
import pe.cazadores.resources.onboarding_next
import pe.cazadores.resources.onboarding_note_1
import pe.cazadores.resources.onboarding_note_2
import pe.cazadores.resources.onboarding_skip
import pe.cazadores.resources.onboarding_start
import pe.cazadores.resources.onboarding_step
import pe.cazadores.ui.components.StatusBarAppearance
import pe.cazadores.ui.components.TextAction
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

private data class Step(val icon: ImageVector, val title: StringResource, val body: StringResource)

private val STEPS = listOf(
    Step(CzIcons.Info, Res.string.onboarding_1_title, Res.string.onboarding_1_body),
    Step(CzIcons.Search, Res.string.onboarding_2_title, Res.string.onboarding_2_body),
    Step(CzIcons.Shield, Res.string.onboarding_3_title, Res.string.onboarding_3_body),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.onboarding_step, pager.currentPage + 1, STEPS.size).uppercase(),
                style = Cz.type.monoSmall,
                color = Cz.colors.onHeaderMuted,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (!isLast) TextAction(stringResource(Res.string.onboarding_skip), onFinish, color = Cz.colors.onHeader)
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            StepPage(STEPS[page], showNotes = page == STEPS.lastIndex)
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
private fun StepPage(step: Step, showNotes: Boolean) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.size(72.dp).background(if (showNotes) Cz.colors.action else Cz.colors.headerField, CazadoresShapes.cardLarge),
            contentAlignment = Alignment.Center,
        ) { Icon(step.icon, contentDescription = null, tint = Cz.colors.onHeader, modifier = Modifier.size(36.dp)) }
        Text(
            stringResource(step.title).uppercase(),
            style = Cz.type.display,
            color = Cz.colors.onHeader,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(step.body), style = Cz.type.body, color = Cz.colors.onHeaderMuted)
        if (showNotes) {
            Column(
                Modifier.fillMaxWidth().background(Cz.colors.headerField, CazadoresShapes.card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Note("01", stringResource(Res.string.onboarding_note_1))
                Note("02", stringResource(Res.string.onboarding_note_2))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Note(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(number, style = Cz.type.mono, color = Cz.colors.reward)
        Text(text, style = Cz.type.bodySmall, color = Cz.colors.onHeader)
    }
}
