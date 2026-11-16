package pe.cazadores.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.core.text.normalizedForMatch
import pe.cazadores.core.text.sentenceCase
import pe.cazadores.core.text.titleCase
import pe.cazadores.domain.model.Catalogs
import pe.cazadores.domain.model.SearchFilters
import pe.cazadores.resources.Res
import pe.cazadores.resources.filters_all
import pe.cazadores.resources.filters_all_f
import pe.cazadores.resources.filters_apply
import pe.cazadores.resources.filters_apply_count
import pe.cazadores.resources.filters_clear
import pe.cazadores.resources.filters_crime
import pe.cazadores.resources.filters_department
import pe.cazadores.resources.filters_location
import pe.cazadores.resources.filters_other_crime
import pe.cazadores.resources.filters_province
import pe.cazadores.resources.filters_province_needs_department
import pe.cazadores.resources.filters_title
import pe.cazadores.ui.components.PrimaryButton
import pe.cazadores.ui.components.SectionLabel
import pe.cazadores.ui.components.TextAction
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz

/** Delitos frecuentes que se muestran como chips; el resto, en "Otro delito…". */
private val FEATURED_CRIMES = listOf(
    "EXTORSION", "SICARIATO", "HOMICIDIO CALIFICADO", "ROBO AGRAVADO", "SECUESTRO",
    "VIOLACION SEXUAL", "TRAFICO ILICITO DE DROGAS", "TRATA DE PERSONAS", "FEMINICIDIO", "TERRORISMO",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheet(
    draft: SearchFilters,
    count: Int?,
    catalogs: Catalogs?,
    onChange: (SearchFilters) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Cz.colors.surface,
        shape = CazadoresShapes.sheet,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = CazadoresDimens.screenPadding).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.filters_title).uppercase(),
                    style = Cz.type.headline,
                    color = Cz.colors.ink,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextAction(
                    stringResource(Res.string.filters_clear),
                    onClick = { onChange(SearchFilters(query = draft.query, sort = draft.sort)) },
                    color = Cz.colors.action,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(Res.string.filters_location))
                val departamentos = catalogs?.departamentos.orEmpty()
                Picker(
                    label = stringResource(Res.string.filters_department),
                    allLabel = stringResource(Res.string.filters_all),
                    selected = departamentos.firstOrNull { it.id == draft.departamentoId }?.name?.titleCase(),
                    options = departamentos.map { it.id to it.name.titleCase() },
                    enabled = departamentos.isNotEmpty(),
                    onSelect = { id -> onChange(draft.copy(departamentoId = id, provinciaId = null)) },
                )
                val provincias = catalogs?.provinciasOf(draft.departamentoId).orEmpty()
                Picker(
                    label = stringResource(Res.string.filters_province),
                    allLabel = if (draft.departamentoId == null) {
                        stringResource(Res.string.filters_province_needs_department)
                    } else {
                        stringResource(Res.string.filters_all_f)
                    },
                    selected = provincias.firstOrNull { it.id == draft.provinciaId }?.name?.titleCase(),
                    options = provincias.map { it.id to it.name.titleCase() },
                    enabled = provincias.isNotEmpty(),
                    onSelect = { id -> onChange(draft.copy(provinciaId = id)) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(Res.string.filters_crime))
                val delitos = catalogs?.delitos.orEmpty()
                val featured = remember(delitos) {
                    FEATURED_CRIMES.mapNotNull { key -> delitos.firstOrNull { it.name.normalizedForMatch() == key } }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrimeChip(stringResource(Res.string.filters_all), selected = draft.delitoId == null) {
                        onChange(draft.copy(delitoId = null))
                    }
                    featured.forEach { delito ->
                        CrimeChip(delito.name.sentenceCase(), selected = draft.delitoId == delito.id) {
                            onChange(draft.copy(delitoId = if (draft.delitoId == delito.id) null else delito.id))
                        }
                    }
                }
                val other = delitos.firstOrNull { it.id == draft.delitoId && it !in featured }
                Picker(
                    label = stringResource(Res.string.filters_other_crime),
                    allLabel = "—",
                    selected = other?.name?.sentenceCase(),
                    options = delitos.filterNot { it in featured }.map { it.id.toString() to it.name.sentenceCase() },
                    enabled = delitos.isNotEmpty(),
                    onSelect = { id -> onChange(draft.copy(delitoId = id?.toIntOrNull())) },
                )
            }

            PrimaryButton(
                text = count?.let { stringResource(Res.string.filters_apply_count, it.toString()) }
                    ?: stringResource(Res.string.filters_apply),
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CrimeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = Cz.type.bodySmall) },
        modifier = Modifier.heightIn(min = 40.dp),
        shape = CazadoresShapes.chip,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Cz.colors.surface,
            labelColor = Cz.colors.ink,
            selectedContainerColor = Cz.colors.ink,
            selectedLabelColor = Cz.colors.bg,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Cz.colors.lineStrong,
            selectedBorderColor = Cz.colors.ink,
        ),
    )
}

/** Selector desplegable con opción "todos" (null). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Picker(
    label: String,
    allLabel: String,
    selected: String?,
    options: List<Pair<String, String>>,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selected ?: allLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = Cz.type.body,
            shape = CazadoresShapes.field,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cz.colors.ink,
                unfocusedBorderColor = if (selected != null) Cz.colors.ink else Cz.colors.lineStrong,
                focusedLabelColor = Cz.colors.ink,
                unfocusedLabelColor = Cz.colors.inkMuted,
                disabledBorderColor = Cz.colors.line,
                disabledTextColor = Cz.colors.inkMuted,
                disabledLabelColor = Cz.colors.inkMuted,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            containerColor = Cz.colors.surface,
        ) {
            DropdownMenuItem(
                text = { Text(allLabel, style = Cz.type.body) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, style = Cz.type.body) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    },
                )
            }
        }
    }
}
