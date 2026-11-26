package pe.cazadores.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pe.cazadores.domain.model.Criminal
import pe.cazadores.resources.Res
import pe.cazadores.resources.report_call
import pe.cazadores.resources.report_call_sub
import pe.cazadores.resources.report_call_alt
import pe.cazadores.resources.report_safety
import pe.cazadores.core.config.AppConfig
import pe.cazadores.ui.components.TextAction
import pe.cazadores.resources.report_details
import pe.cazadores.resources.report_details_hint
import pe.cazadores.resources.report_disclaimer
import pe.cazadores.resources.report_email_body
import pe.cazadores.resources.report_email_subject
import pe.cazadores.resources.report_or_write
import pe.cazadores.resources.report_send
import pe.cazadores.resources.report_subtitle
import pe.cazadores.resources.report_title
import pe.cazadores.resources.report_when
import pe.cazadores.resources.report_when_hint
import pe.cazadores.resources.report_where
import pe.cazadores.resources.report_where_hint
import pe.cazadores.ui.components.SecondaryButton
import pe.cazadores.ui.components.sheetContentInsets
import pe.cazadores.ui.components.sheetModifier
import pe.cazadores.ui.theme.CazadoresDimens
import pe.cazadores.ui.theme.CazadoresShapes
import pe.cazadores.ui.theme.Cz
import pe.cazadores.ui.theme.CzIcons

/** Llamar (primario) o armar un correo. La app no guarda ni envía nada. Sin anuncios. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(
    criminal: Criminal,
    onCall: () -> Unit,
    onCallAlt: () -> Unit,
    onSendEmail: (subject: String, body: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var where by rememberSaveable { mutableStateOf("") }
    var whenText by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    val id = criminal.id.toString()
    val subject = stringResource(Res.string.report_email_subject, criminal.displayName, id)
    val body = stringResource(Res.string.report_email_body, criminal.displayName, id, where.trim(), whenText.trim(), details.trim())

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = sheetModifier,
        contentWindowInsets = { sheetContentInsets },
        containerColor = Cz.colors.surface,
        shape = CazadoresShapes.sheet,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = CazadoresDimens.screenPadding).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.report_title).uppercase(),
                    style = Cz.type.headline,
                    color = Cz.colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(Res.string.report_subtitle, criminal.displayName, id), style = Cz.type.bodySmall, color = Cz.colors.inkMuted)
                Text(stringResource(Res.string.report_safety), style = Cz.type.bodyStrong, color = Cz.colors.ink)
            }

            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .background(Cz.colors.action, CazadoresShapes.button)
                    .clickable(role = Role.Button, onClick = onCall)
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(CzIcons.Phone, contentDescription = null, tint = Cz.colors.onAction, modifier = Modifier.size(22.dp))
                Column {
                    Text(stringResource(Res.string.report_call), style = Cz.type.button, color = Cz.colors.onAction)
                    Text(stringResource(Res.string.report_call_sub), style = Cz.type.caption, color = Cz.colors.onAction)
                }
            }

            TextAction(
                text = stringResource(Res.string.report_call_alt, AppConfig.REPORT_PHONE_ALT),
                onClick = onCallAlt,
                modifier = Modifier.fillMaxWidth().heightIn(min = CazadoresDimens.touchTarget),
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = Cz.colors.line)
                Text(stringResource(Res.string.report_or_write), style = Cz.type.caption, color = Cz.colors.inkMuted)
                HorizontalDivider(Modifier.weight(1f), color = Cz.colors.line)
            }

            ReportField(stringResource(Res.string.report_where), stringResource(Res.string.report_where_hint), where) { where = it }
            ReportField(stringResource(Res.string.report_when), stringResource(Res.string.report_when_hint), whenText) { whenText = it }
            ReportField(stringResource(Res.string.report_details), stringResource(Res.string.report_details_hint), details, singleLine = false) { details = it }

            SecondaryButton(
                text = stringResource(Res.string.report_send),
                onClick = { onSendEmail(subject, body) },
                icon = CzIcons.Mail,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(Res.string.report_disclaimer), style = Cz.type.caption, color = Cz.colors.inkMuted)
        }
    }
}

@Composable
private fun ReportField(label: String, hint: String, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(hint) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        textStyle = Cz.type.body,
        shape = CazadoresShapes.field,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cz.colors.ink,
            unfocusedBorderColor = Cz.colors.lineStrong,
            focusedLabelColor = Cz.colors.ink,
            unfocusedLabelColor = Cz.colors.inkMuted,
            cursorColor = Cz.colors.ink,
        ),
    )
}
