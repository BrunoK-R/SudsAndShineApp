package com.sudsmobile.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

internal data class AdminLookupOption(
    val id: String,
    val label: String,
    val detail: String = "",
    val icon: ImageVector? = null,
)

/** Uses a local draft so cancelling a multi-selection never changes the form. */
@Composable
internal fun AdminLookupField(
    label: String,
    selectedIds: Set<String>,
    options: List<AdminLookupOption>,
    onSelect: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    multiple: Boolean = false,
    emptyLabel: String = "Selecionar",
    allowEmpty: Boolean = true,
    emptyChoice: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    val byId = options.associateBy { it.id }
    val selected = selectedIds.map { byId[it] ?: AdminLookupOption(it, it, "Valor guardado") }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            selected.singleOrNull()?.icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp)) }
            Text(selected.joinToString(", ") { it.label }.ifBlank { emptyLabel }, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ExpandMore, contentDescription = "Escolher $label")
        }
    }
    if (open) {
        var query by remember { mutableStateOf("") }
        var draft by remember { mutableStateOf(selectedIds) }
        // Preserve previously saved IDs even when a catalogue entry is no longer available.
        val choices = options + selected.filter { it.id !in byId }
        val filtered = choices.filter { (it.label + " " + it.detail + " " + it.id).contains(query.trim(), ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Pesquisar") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        if (emptyChoice != null && query.isBlank()) {
                            item {
                                AdminLookupRow(AdminLookupOption("", emptyChoice), draft.isEmpty(), multiple) {
                                    if (multiple) draft = emptySet() else { onSelect(emptySet()); open = false }
                                }
                            }
                        }
                        items(filtered, key = { it.id }) { option ->
                            AdminLookupRow(option, option.id in draft, multiple) {
                                if (multiple) {
                                    draft = if (option.id in draft) draft - option.id else draft + option.id
                                } else {
                                    onSelect(setOf(option.id)); open = false
                                }
                            }
                        }
                        if (filtered.isEmpty()) item { Text("Sem resultados", modifier = Modifier.padding(12.dp)) }
                    }
                }
            },
            confirmButton = {
                if (multiple) TextButton(
                    enabled = allowEmpty || draft.isNotEmpty(),
                    onClick = { onSelect(draft); open = false },
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun AdminLookupRow(option: AdminLookupOption, selected: Boolean, multiple: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (multiple) Checkbox(checked = selected, onCheckedChange = null)
        option.icon?.let { Icon(it, contentDescription = null) }
        Column(modifier = Modifier.weight(1f)) {
            Text(option.label, color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
            if (option.detail.isNotBlank()) Text(option.detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal val adminIconOptions: List<AdminLookupOption> = listOf(
    AdminLookupOption("car", "Automóvel", icon = Icons.Filled.DirectionsCar),
    AdminLookupOption("air", "Ar / aspiração", icon = Icons.Filled.Air),
    AdminLookupOption("circle", "Pneus", icon = Icons.Filled.Circle),
    AdminLookupOption("shield", "Proteção", icon = Icons.Filled.Shield),
    AdminLookupOption("auto_awesome", "Brilho", icon = Icons.Filled.AutoAwesome),
    AdminLookupOption("water_drop", "Lavagem exterior", icon = Icons.Filled.WaterDrop),
    AdminLookupOption("weekend", "Interior / estofos", icon = Icons.Filled.Weekend),
)

internal fun canonicalAdminIconKey(value: String): String = when (value.lowercase()) {
    "vacuum", "odor" -> "air"
    "tires", "tyre" -> "circle"
    "wax" -> "shield"
    "sparkles", "premium" -> "auto_awesome"
    "water", "droplets", "exterior" -> "water_drop"
    "sofa", "interior" -> "weekend"
    else -> value.lowercase()
}

@Composable
internal fun AdminIconLookup(value: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    AdminLookupField("Ícone", setOf(canonicalAdminIconKey(value)), adminIconOptions,
        onSelect = { it.firstOrNull()?.let(onValueChange) }, enabled = enabled)
}

/** Presets keep uncommon existing values editable rather than silently rounding them. */
@Composable
internal fun AdminPresetField(
    label: String, value: String, options: List<AdminLookupOption>, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, numeric: Boolean = false,
    customLabel: String = "Outro valor", minLines: Int = 1,
) {
    val known = options.any { it.id == value }
    var custom by remember { mutableStateOf(!known) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AdminLookupField(label, setOf(if (custom || !known) "__custom__" else value),
            options + AdminLookupOption("__custom__", customLabel),
            onSelect = { ids ->
                val chosen = ids.firstOrNull() ?: return@AdminLookupField
                custom = chosen == "__custom__"
                if (!custom) onValueChange(chosen)
            }, enabled = enabled)
        if (custom || !known) OutlinedTextField(
            value = value, onValueChange = onValueChange, enabled = enabled,
            label = { Text(customLabel) }, minLines = minLines, singleLine = numeric,
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun adminDurationOptions(minutes: List<Int>): List<AdminLookupOption> = minutes.map {
    AdminLookupOption(it.toString(), when {
        it == 0 -> "0 min"
        it == 1440 -> "1 dia"
        it % 1440 == 0 -> "${it / 1440} dias"
        it % 60 == 0 -> "${it / 60} h"
        else -> "$it min"
    })
}

@Composable
internal fun AdminDurationLookup(
    label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, minutes: List<Int> = listOf(0, 15, 30, 45, 60, 90, 120, 180, 240),
) {
    AdminPresetField(label, value, adminDurationOptions(minutes), onValueChange, modifier, enabled,
        numeric = true, customLabel = "Outro valor (minutos)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminTimeLookup(label: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(value.ifBlank { "Escolher hora" })
        }
    }
    if (open) {
        val parts = value.split(":")
        val state = rememberTimePickerState(
            initialHour = (parts.getOrNull(0)?.toIntOrNull() ?: 0).coerceIn(0, 23),
            initialMinute = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59), is24Hour = true,
        )
        AlertDialog(onDismissRequest = { open = false }, title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = {
                onValueChange("${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}")
                open = false
            }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } })
    }
}

internal expect fun adminTimeZoneIds(): List<String>

@Composable
internal fun AdminTimeZoneLookup(value: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    val options = remember {
        adminTimeZoneIds().sortedWith(compareBy<String> { it != "Europe/Lisbon" }.thenBy { it })
            .map { AdminLookupOption(it, it.replace('_', ' ')) }
    }
    AdminLookupField("Fuso horário", setOf(value), options, { it.firstOrNull()?.let(onValueChange) }, enabled = enabled)
}

internal val adminRejectionReasons = listOf("Sem disponibilidade", "Serviço indisponível", "Pedido duplicado", "A pedido do cliente")
    .map { AdminLookupOption(it, it) }
internal val adminBlockingReasons = listOf("Pausa da equipa", "Manutenção", "Ausência de pessoal", "Evento privado")
    .map { AdminLookupOption(it, it) }
