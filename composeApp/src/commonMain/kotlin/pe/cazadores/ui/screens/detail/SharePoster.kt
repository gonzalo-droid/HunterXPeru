package pe.cazadores.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.core.DangerAssessment
import pe.cazadores.core.config.AppConfig
import pe.cazadores.domain.model.Criminal
import pe.cazadores.domain.model.RewardClass
import pe.cazadores.domain.model.ThemeMode
import pe.cazadores.resources.Res
import pe.cazadores.resources.share_image_call
import pe.cazadores.resources.share_image_disclaimer
import pe.cazadores.ui.components.CriminalPhoto
import pe.cazadores.ui.components.WantedPoster
import pe.cazadores.ui.components.LocalPhotoRepository
import pe.cazadores.ui.components.ensureBestPhoto
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.CazadoresTheme
import pe.cazadores.ui.theme.Cz

/** Tamaño de la imagen compartida (4:5, el formato vertical que WhatsApp e Instagram muestran sin recortar). */
internal const val SHARE_IMAGE_WIDTH_PX = 1080
internal const val SHARE_IMAGE_HEIGHT_PX = 1350

/** 2 px por dp → lienzo de 540 × 675 dp. Fijo: la imagen no depende de la pantalla ni de la letra del usuario. */
private val ShareDensity = Density(density = 2f, fontScale = 1f)

/**
 * Contenido de la imagen compartida: el cartel, la línea gratuita y el aviso de imagen no oficial.
 * Siempre en papel (tema claro forzado por quien lo llama) para que salga igual desde ambos temas.
 */
@Composable
fun SharePosterContent(criminal: Criminal, assessment: DangerAssessment, rewardClass: RewardClass?) {
    Column(
        Modifier.fillMaxSize().background(Cz.colors.paper).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScaleDownToFit(Modifier.weight(1f).fillMaxWidth()) {
            WantedPoster(criminal = criminal, assessment = assessment, rewardClass = rewardClass, onRankClick = null) {
                CriminalPhoto(
                    criminal = criminal,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    shape = CazadoresShapes.photo,
                )
            }
        }
        Text(
            stringResource(Res.string.share_image_call, AppConfig.REPORT_PHONE),
            style = Cz.type.title,
            color = Cz.colors.onPosterRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
                .background(Cz.colors.posterRed, CazadoresShapes.button)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            stringResource(Res.string.share_image_disclaimer),
            style = Cz.type.bodyStrong,
            color = Cz.colors.onPaper,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Mide el hijo sin límite de alto; si no cabe, lo reduce (centrado) en vez de cortarlo. */
@Composable
private fun ScaleDownToFit(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val scale = if (placeable.height > height) height.toFloat() / placeable.height else 1f
        layout(width, height) {
            // La escala se aplica alrededor del centro del hijo: centrarlo sin escalar basta para que quede centrado.
            placeable.placeWithLayer((width - placeable.width) / 2, (height - placeable.height) / 2) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
}

/**
 * Dibuja [SharePosterContent] fuera de pantalla a 1080 × 1350 px y entrega el bitmap (o `null` si falla).
 *
 * Mide 0 × 0 para su padre (no mueve nada), no expone semántica y graba su contenido en un `GraphicsLayer`
 * que nunca se dibuja en pantalla. Solo se compone mientras se comparte.
 */
@Composable
fun SharePosterCapture(
    criminal: Criminal,
    assessment: DangerAssessment,
    rewardClass: RewardClass?,
    onCaptured: (ImageBitmap?) -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val photos = LocalPhotoRepository.current
    val callback by rememberUpdatedState(onCaptured)
    // El cartel solo se compone cuando la foto ya está en la caché: así CriminalPhoto la lee al componerse y la
    // captura nunca sale con la silueta por un toque rápido en Compartir.
    var photoReady by remember { mutableStateOf(false) }
    LaunchedEffect(layer) {
        val bitmap = try {
            withTimeoutOrNull(CAPTURE_TIMEOUT_MS) {
                ensureBestPhoto(criminal, photos)
                photoReady = true
                // Un cuadro para componer con la foto lista; luego dos: el primero mide y graba la capa; en el segundo la grabación ya está completa.
                withFrameNanos { }
                withFrameNanos { }
                layer.toImageBitmap()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        callback(bitmap)
    }
    Box(
        Modifier
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixed(SHARE_IMAGE_WIDTH_PX, SHARE_IMAGE_HEIGHT_PX))
                layout(0, 0) { placeable.place(0, 0) }
            }
            .clearAndSetSemantics { }
            .drawWithContent {
                // Solo se graba; no se llama a drawLayer(layer), así que nada aparece en pantalla.
                layer.record { this@drawWithContent.drawContent() }
            },
    ) {
        CompositionLocalProvider(LocalDensity provides ShareDensity) {
            CazadoresTheme(themeMode = ThemeMode.LIGHT) {
                if (photoReady) SharePosterContent(criminal, assessment, rewardClass)
            }
        }
    }
}

private const val CAPTURE_TIMEOUT_MS = 5_000L
