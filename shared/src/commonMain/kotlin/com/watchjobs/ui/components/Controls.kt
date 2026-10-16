package com.watchjobs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watchjobs.ui.theme.WatchJobsColors

// Rótulo em caixa alta, pequeno e espaçado ("ACTIVE APPLICATIONS", "FILTERS" nos protótipos).
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = WatchJobsColors.Ink) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier)
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = WatchJobsColors.Red,
            contentColor = WatchJobsColors.White,
            disabledContainerColor = WatchJobsColors.Red.copy(alpha = 0.5f),
            disabledContentColor = WatchJobsColors.White
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        modifier = modifier.heightIn(min = 42.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(color = WatchJobsColors.White, strokeWidth = 2.dp, modifier = Modifier.height(16.dp).width(16.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        border = BorderStroke(1.dp, WatchJobsColors.Ink),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = WatchJobsColors.White, contentColor = WatchJobsColors.Ink),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        modifier = modifier.heightIn(min = 42.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = WatchJobsColors.Ink) {
    TextButton(onClick = onClick, shape = RectangleShape, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = WatchJobsColors.Ink,
    unfocusedBorderColor = WatchJobsColors.Border,
    disabledBorderColor = WatchJobsColors.Divider,
    errorBorderColor = WatchJobsColors.Error,
    focusedContainerColor = WatchJobsColors.White,
    unfocusedContainerColor = WatchJobsColors.White,
    disabledContainerColor = WatchJobsColors.Background,
    errorContainerColor = WatchJobsColors.White,
    cursorColor = WatchJobsColors.Red
)

// Campo com o rótulo em caixa alta acima, como nos protótipos, e o erro do backend embaixo.
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    tag: String = label
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(label, color = WatchJobsColors.Muted)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().testTag(tag),
            enabled = enabled,
            isError = error != null,
            singleLine = singleLine,
            minLines = minLines,
            placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted) } },
            textStyle = MaterialTheme.typography.bodyLarge,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            shape = RectangleShape,
            colors = fieldColors()
        )
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Error)
        }
    }
}

// Lista de seleção (ExposedDropdownMenuBox) mostrando o rótulo em português.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    allowEmpty: Boolean = true,
    emptyLabel: String = "Não informado",
    enabled: Boolean = true,
    tag: String = label
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(label, color = WatchJobsColors.Muted)
        ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
            OutlinedTextField(
                value = selected?.let(optionLabel) ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                isError = error != null,
                singleLine = true,
                placeholder = { Text("Selecione", style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Muted) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RectangleShape,
                colors = fieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                    .testTag(tag)
            )
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                containerColor = WatchJobsColors.White,
                shape = RectangleShape
            ) {
                if (allowEmpty) {
                    DropdownMenuItem(
                        text = { Text(emptyLabel, color = WatchJobsColors.Muted) },
                        onClick = { onSelect(null); expanded = false }
                    )
                }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Error)
        }
    }
}

// Etiqueta com borda vermelha ("Design systems", "Figma" nos protótipos).
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, filled: Boolean = false) {
    Box(
        modifier = modifier
            .background(if (filled) WatchJobsColors.Red else WatchJobsColors.White)
            .border(1.dp, WatchJobsColors.Red)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (filled) WatchJobsColors.White else WatchJobsColors.RedDark
        )
    }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WatchJobsColors.RedSoft)
            .border(1.dp, WatchJobsColors.Red)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("errorBanner")
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.RedDark)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = WatchJobsColors.White,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { PrimaryButton(confirmText, onConfirm, modifier = Modifier.testTag("confirmButton")) },
        dismissButton = { SecondaryButton("Cancelar", onDismiss) }
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = WatchJobsColors.Divider)
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = WatchJobsColors.Red, strokeWidth = 3.dp)
    }
}
