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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
internal fun AdminRemoteLookupField(
    label: String, valueLabel: String, onSearch: suspend (String) -> List<AdminLookupOption>,
    onSelect: (AdminLookupOption) -> Unit, enabled: Boolean = true,
    searchLabel: String = "Pesquisar", minQueryLength: Int = 2,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(valueLabel.ifBlank { label })
    }
    if (open) {
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf(emptyList<AdminLookupOption>()) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var retry by remember { mutableStateOf(0) }
        LaunchedEffect(query, retry) {
            results = emptyList(); error = null; loading = false
            if (query.trim().length >= minQueryLength) {
                loading = true
                delay(400)
                try { results = onSearch(query.trim()) }
                catch (cause: CancellationException) { throw cause }
                catch (cause: Exception) { error = cause.message ?: "Não foi possível pesquisar. Tente novamente." }
                finally { loading = false }
            }
        }
        AlertDialog(onDismissRequest = { open = false }, title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(searchLabel) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    when {
                        loading -> CircularProgressIndicator()
                        error != null -> {
                            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { retry++ }) { Text("Tentar novamente") }
                        }
                        query.trim().length < minQueryLength -> Text("Escreva pelo menos $minQueryLength caracteres.")
                        results.isEmpty() -> Text("Sem resultados")
                        else -> LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                            items(results, key = { it.id }) { option ->
                                Column(modifier = Modifier.fillMaxWidth().clickable {
                                    onSelect(option); open = false
                                }.padding(vertical = 12.dp)) {
                                    Text(option.label)
                                    if (option.detail.isNotBlank()) Text(option.detail, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }, confirmButton = {}, dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } })
    }
}

internal data class AdminAddressOption(val addressLine1: String, val addressLine2: String, val mapsUri: String)

@Composable
internal expect fun rememberAdminAddressSearch(): suspend (String) -> List<AdminAddressOption>

@Composable
internal fun AdminAddressLookup(enabled: Boolean, onSelect: (AdminAddressOption) -> Unit) {
    val search = rememberAdminAddressSearch()
    var addresses by remember { mutableStateOf(emptyList<AdminAddressOption>()) }
    AdminRemoteLookupField("Procurar morada", "Procurar morada ou estabelecimento", enabled = enabled,
        searchLabel = "Morada, localidade ou estabelecimento", minQueryLength = 3,
        onSearch = { query ->
            addresses = search(query).distinctBy { it.mapsUri }
            addresses.map { AdminLookupOption(it.mapsUri, it.addressLine1, it.addressLine2) }
        }, onSelect = { option -> addresses.firstOrNull { it.mapsUri == option.id }?.let(onSelect) })
}

internal data class AdminOpeningHoursRow(val day: String, val hours: String, val closed: Boolean)
internal fun parseAdminHoursRows(value: String): List<AdminOpeningHoursRow> = value.lineSequence()
    .filter { it.isNotBlank() }.map { line ->
        val parts = line.split('|').map { it.trim() }
        AdminOpeningHoursRow(parts.firstOrNull().orEmpty(), parts.getOrNull(1).orEmpty(),
            parts.drop(2).any { it.equals("fechado", true) || it.equals("closed", true) })
    }.toList()
internal fun serializeAdminHoursRows(rows: List<AdminOpeningHoursRow>): String = rows.joinToString("\n") {
    "${it.day} | ${it.hours}" + if (it.closed) " | fechado" else ""
}

private val adminWeekdayOptions = listOf("Segunda a Sexta", "Todos os dias", "Fim de semana", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")
    .map { AdminLookupOption(it, it) }

@Composable
internal fun AdminOpeningHoursEditor(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    val rows = parseAdminHoursRows(value)
    fun update(index: Int, row: AdminOpeningHoursRow) = onValueChange(serializeAdminHoursRows(rows.toMutableList().also { it[index] = row }))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Horários", style = MaterialTheme.typography.titleSmall)
        rows.forEachIndexed { index, row ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminPresetField("Dia / dias", row.day, adminWeekdayOptions, { update(index, row.copy(day = it)) },
                    enabled = enabled, customLabel = "Outro dia / grupo de dias")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Encerrado")
                    Switch(checked = row.closed, enabled = enabled, onCheckedChange = {
                        update(index, row.copy(closed = it, hours = if (it) "Encerrado" else "09:00 - 19:00"))
                    })
                }
                if (!row.closed) {
                    val times = Regex("([0-2][0-9]:[0-5][0-9])").findAll(row.hours).map { it.value }.toList()
                    if (times.size == 2 && row.hours.count { it == ':' } == 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminTimeLookup("Abre", times[0], { update(index, row.copy(hours = "$it - ${times[1]}")) }, Modifier.weight(1f), enabled)
                            AdminTimeLookup("Fecha", times[1], { update(index, row.copy(hours = "${times[0]} - $it")) }, Modifier.weight(1f), enabled)
                        }
                    } else {
                        // Preserve split shifts or custom public text until the administrator chooses a standard range.
                        OutlinedTextField(row.hours, { update(index, row.copy(hours = it)) }, enabled = enabled,
                            label = { Text("Horário especial") }, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = { update(index, row.copy(hours = "09:00 - 19:00")) }, enabled = enabled) { Text("Usar seleção de horas") }
                    }
                }
                TextButton(onClick = { onValueChange(serializeAdminHoursRows(rows.filterIndexed { i, _ -> i != index })) },
                    enabled = enabled && rows.size > 1) { Text("Remover horário") }
            }
        }
        OutlinedButton(onClick = { onValueChange(serializeAdminHoursRows(rows + AdminOpeningHoursRow("Segunda", "09:00 - 19:00", false))) },
            enabled = enabled && rows.size < 10) { Text("Adicionar horário") }
    }
}

internal data class AdminSocialRow(val platform: String, val uri: String)
internal fun parseAdminSocialRows(value: String): List<AdminSocialRow> = value.lineSequence().filter { it.isNotBlank() }.map {
    val parts = it.split('|', limit = 2).map(String::trim)
    AdminSocialRow(parts.firstOrNull().orEmpty(), parts.getOrNull(1).orEmpty())
}.toList()
internal fun serializeAdminSocialRows(rows: List<AdminSocialRow>): String = rows.joinToString("\n") { "${it.platform} | ${it.uri}" }

@Composable
internal fun AdminSocialLinksEditor(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    val rows = parseAdminSocialRows(value)
    fun update(index: Int, row: AdminSocialRow) = onValueChange(serializeAdminSocialRows(rows.toMutableList().also { it[index] = row }))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Redes sociais", style = MaterialTheme.typography.titleSmall)
        rows.forEachIndexed { index, row ->
            AdminPresetField("Plataforma", row.platform,
                listOf("Instagram", "Facebook", "TikTok", "YouTube", "LinkedIn", "Website").map { AdminLookupOption(it, it) },
                { update(index, row.copy(platform = it)) }, enabled = enabled, customLabel = "Outra plataforma")
            OutlinedTextField(row.uri, { update(index, row.copy(uri = it)) }, label = { Text("Link") },
                enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { onValueChange(serializeAdminSocialRows(rows.filterIndexed { i, _ -> i != index })) },
                enabled = enabled) { Text("Remover rede social") }
        }
        OutlinedButton(onClick = { onValueChange(serializeAdminSocialRows(rows + AdminSocialRow("Instagram", ""))) },
            enabled = enabled && rows.size < 8) { Text("Adicionar rede social") }
    }
}
