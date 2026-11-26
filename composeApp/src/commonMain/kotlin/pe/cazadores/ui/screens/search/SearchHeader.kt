package pe.cazadores.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.core.text.titleCase
import pe.cazadores.domain.model.SortOrder
import pe.cazadores.resources.Res
import pe.cazadores.resources.filters_button_active_cd
import pe.cazadores.resources.filters_button_cd
import pe.cazadores.resources.remove_filter_cd
import pe.cazadores.resources.results_count
import pe.cazadores.resources.results_count_one
import pe.cazadores.resources.search_clear_cd
import pe.cazadores.resources.search_field_label
import pe.cazadores.resources.search_field_placeholder
import pe.cazadores.resources.search_title
import pe.cazadores.resources.sort_cd
import pe.cazadores.resources.sort_name
import pe.cazadores.resources.sort_reward
import pe.cazadores.resources.sort_reward_asc
import pe.cazadores.ui.components.OfflineBanner
import pe.cazadores.ui.components.ScreenTitle
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

@Composable
internal fun SearchHeader(
    state: SearchUiState,
    focusSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onOpenFilters: () -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onRemoveDepartment: () -> Unit,
    onRemoveProvince: () -> Unit,
    onRemoveCrime: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Solo la primera vez (atajo desde Inicio): al volver de una ficha no se reabre el teclado.
    var focusHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(focusSearch) {
        if (focusSearch && !focusHandled) {
            focusRequester.requestFocus()
            focusHandled = true
        }
    }

    Column(
        Modifier.padding(start = CazadoresDimens.screenPadding, end = CazadoresDimens.screenPadding, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(stringResource(Res.string.search_title))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.queryInput,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                label = { Text(stringResource(Res.string.search_field_label)) },
                placeholder = { Text(stringResource(Res.string.search_field_placeholder)) },
                leadingIcon = { Icon(CzIcons.Search, contentDescription = null) },
                trailingIcon = if (state.queryInput.isNotEmpty()) {
                    {
                        IconButton(onClick = onClearQuery) {
                            Icon(CzIcons.Close, contentDescription = stringResource(Res.string.search_clear_cd))
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = CazadoresShapes.field,
                textStyle = Cz.type.body,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Cz.colors.surface,
                    unfocusedContainerColor = Cz.colors.surface,
                    focusedBorderColor = Cz.colors.ink,
                    unfocusedBorderColor = Cz.colors.lineStrong,
                    focusedLabelColor = Cz.colors.ink,
                    unfocusedLabelColor = Cz.colors.inkMuted,
                    cursorColor = Cz.colors.ink,
                ),
            )
            FiltersButton(state.filters.activeCount, onOpenFilters)
        }
        ActiveFilterChips(state, onRemoveDepartment, onRemoveProvince, onRemoveCrime)
        if (!state.isLoading && state.error == null) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                ResultsCount(state.totalElements, Modifier)
                SortMenu(state.filters.sort, onSortChange)
            }
        }
        state.cachedAtMillis?.let { OfflineBanner(it) }
    }
}

@Composable
internal fun FiltersButton(activeCount: Int, onClick: () -> Unit) {
    val description = if (activeCount > 0) {
        stringResource(Res.string.filters_button_active_cd, activeCount)
    } else {
        stringResource(Res.string.filters_button_cd)
    }
    Box {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp).background(Cz.colors.ink, CazadoresShapes.field)
                .semantics { contentDescription = description },
        ) { Icon(CzIcons.Filter, contentDescription = null, tint = Cz.colors.bg) }
        if (activeCount > 0) {
            Text(
                activeCount.toString(),
                style = Cz.type.monoSmall,
                fontWeight = FontWeight.Bold,
                color = Cz.colors.onReward,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp).background(Cz.colors.reward, CazadoresShapes.chip)
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
internal fun ActiveFilterChips(
    state: SearchUiState,
    onRemoveDepartment: () -> Unit,
    onRemoveProvince: () -> Unit,
    onRemoveCrime: () -> Unit,
) {
    val f = state.filters
    val catalogs = state.catalogs
    val chips = buildList {
        f.departamentoId?.let { id -> add((catalogs?.departamentos?.firstOrNull { it.id == id }?.name ?: id) to onRemoveDepartment) }
        f.provinciaId?.let { id ->
            add((catalogs?.provinciasOf(f.departamentoId)?.firstOrNull { it.id == id }?.name ?: id) to onRemoveProvince)
        }
        f.delitoId?.let { id -> add((catalogs?.delitos?.firstOrNull { it.id == id }?.name ?: "#$id") to onRemoveCrime) }
    }
    if (chips.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { (label, onRemove) ->
            val cd = stringResource(Res.string.remove_filter_cd, label)
            InputChip(
                selected = true,
                onClick = onRemove,
                label = { Text(label.titleCase(), style = Cz.type.bodySmall) },
                trailingIcon = { Icon(CzIcons.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = cd },
                shape = CazadoresShapes.chip,
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = Cz.colors.ink,
                    selectedLabelColor = Cz.colors.bg,
                    selectedTrailingIconColor = Cz.colors.bg,
                ),
                border = null,
            )
        }
    }
}

@Composable
internal fun ResultsCount(total: Int?, modifier: Modifier) {
    val text = when (total) {
        null -> ""
        1 -> stringResource(Res.string.results_count_one)
        else -> stringResource(Res.string.results_count, total.toString())
    }
    Text(text, style = Cz.type.bodySmall, color = Cz.colors.inkMuted, modifier = modifier)
}

@Composable
internal fun sortLabel(sort: SortOrder) = stringResource(
    when (sort) {
        SortOrder.NAME -> Res.string.sort_name
        SortOrder.REWARD -> Res.string.sort_reward
        SortOrder.REWARD_ASC -> Res.string.sort_reward_asc
    },
)

@Composable
internal fun SortMenu(current: SortOrder, onSortChange: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = sortLabel(current)
    val cd = stringResource(Res.string.sort_cd, label)
    Box {
        TextButton(
            onClick = { open = true },
            modifier = Modifier.heightIn(min = CazadoresDimens.touchTarget).semantics { contentDescription = cd },
        ) {
            Text(label, style = Cz.type.bodyStrong, color = Cz.colors.ink)
            Icon(CzIcons.ChevronDown, contentDescription = null, tint = Cz.colors.ink, modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Cz.colors.surface) {
            SortOrder.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sortLabel(sort), style = Cz.type.body, fontWeight = if (sort == current) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        open = false
                        onSortChange(sort)
                    },
                )
            }
        }
    }
}
