package pe.cazadores.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pe.cazadores.core.config.AppConfig
import pe.cazadores.platform.ExternalActions
import pe.cazadores.resources.Res
import pe.cazadores.resources.about_community_body
import pe.cazadores.resources.about_community_title
import pe.cazadores.resources.about_future_body
import pe.cazadores.resources.about_future_title
import pe.cazadores.resources.about_motto
import pe.cazadores.resources.about_rate
import pe.cazadores.resources.about_share_app
import pe.cazadores.resources.about_share_text
import pe.cazadores.resources.about_support_body
import pe.cazadores.resources.about_support_title
import pe.cazadores.resources.about_title
import pe.cazadores.resources.about_why_body
import pe.cazadores.resources.about_why_title
import pe.cazadores.resources.back
import pe.cazadores.resources.source_note
import pe.cazadores.ui.components.BrandMark
import pe.cazadores.ui.components.PrimaryButton
import pe.cazadores.ui.components.SecondaryButton
import pe.cazadores.ui.components.StatusBarAppearance
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

private data class Block(val title: StringResource, val body: StringResource)

private val BLOCKS = listOf(
    Block(Res.string.about_why_title, Res.string.about_why_body),
    Block(Res.string.about_community_title, Res.string.about_community_body),
    Block(Res.string.about_future_title, Res.string.about_future_body),
)

@Composable
fun AboutRoute(onBack: () -> Unit, actions: ExternalActions = koinInject()) {
    val shareText = stringResource(Res.string.about_share_text, AppConfig.PLAY_STORE_URL, AppConfig.APP_STORE_URL)
    AboutScreen(
        onBack = onBack,
        onShareApp = { actions.share(shareText) },
        onRate = actions::openStoreReview,
    )
}

/** Propósito del proyecto y cómo apoyarlo. Mismo esquema que Régimen especial: cabecera oscura y tarjetas. */
@Composable
fun AboutScreen(onBack: () -> Unit, onShareApp: () -> Unit, onRate: () -> Unit) {
    StatusBarAppearance(lightIcons = true)
    Column(Modifier.fillMaxSize().background(Cz.colors.bg)) {
        Row(Modifier.fillMaxWidth().background(Cz.colors.header).statusBarsPadding().padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(CzIcons.Back, stringResource(Res.string.back), tint = Cz.colors.onHeader)
            }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().background(Cz.colors.header)
                    .padding(start = CazadoresDimens.screenPadding, end = CazadoresDimens.screenPadding, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BrandMark(56.dp, Modifier.padding(bottom = 8.dp))
                Text(
                    stringResource(Res.string.about_title),
                    style = Cz.type.headline,
                    color = Cz.colors.onHeader,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(Res.string.about_motto), style = Cz.type.body, color = Cz.colors.onHeaderMuted)
            }

            Column(
                Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = CazadoresDimens.screenPadding, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BLOCKS.forEach { block ->
                    Column(
                        Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            stringResource(block.title),
                            style = Cz.type.bodyStrong,
                            color = Cz.colors.ink,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(stringResource(block.body), style = Cz.type.bodySmall, color = Cz.colors.inkMuted)
                    }
                }

                Column(
                    Modifier.fillMaxWidth().background(Cz.colors.surface, CazadoresShapes.card).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(Res.string.about_support_title),
                        style = Cz.type.bodyStrong,
                        color = Cz.colors.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(stringResource(Res.string.about_support_body), style = Cz.type.bodySmall, color = Cz.colors.inkMuted)
                    PrimaryButton(
                        text = stringResource(Res.string.about_share_app),
                        onClick = onShareApp,
                        icon = CzIcons.Share,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryButton(
                        text = stringResource(Res.string.about_rate),
                        onClick = onRate,
                        icon = CzIcons.Star,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Text(stringResource(Res.string.source_note), style = Cz.type.caption, color = Cz.colors.inkMuted)
            }
        }
    }
}
