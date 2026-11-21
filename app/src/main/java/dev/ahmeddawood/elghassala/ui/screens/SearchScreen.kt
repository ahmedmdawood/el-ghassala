package dev.ahmeddawood.elghassala.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ahmeddawood.elghassala.AppViewModel
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.EmptyState

private enum class Filter { ALL, DAYS, TRANSPORT, EXPENSES, TRANSFERS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: AppViewModel, nav: NavHostController) {
    val records by vm.records.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.ALL) }

    val q = query.trim().lowercase()
    fun matches(vararg fields: String) = q.isEmpty() || fields.any { it.lowercase().contains(q) }

    val dayHits = if (filter in setOf(Filter.ALL, Filter.DAYS))
        records.days.filter { matches(it.title) } else emptyList()
    val transportHits = if (filter in setOf(Filter.ALL, Filter.TRANSPORT))
        records.transports.filter { matches(it.origin, it.destination, it.notes) } else emptyList()
    val expenseHits = if (filter in setOf(Filter.ALL, Filter.EXPENSES))
        records.expenses.filter { matches(it.title, it.notes) } else emptyList()
    val transferHits = if (filter in setOf(Filter.ALL, Filter.TRANSFERS))
        records.transfers.filter {
            matches(records.peopleNames[it.fromPersonId] ?: "", records.peopleNames[it.toPersonId] ?: "", it.notes)
        } else emptyList()

    val filterLabels = listOf(
        Filter.ALL to stringResource(R.string.filter_all),
        Filter.DAYS to stringResource(R.string.filter_days),
        Filter.TRANSPORT to stringResource(R.string.filter_transport),
        Filter.EXPENSES to stringResource(R.string.filter_expenses),
        Filter.TRANSFERS to stringResource(R.string.filter_transfers),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterLabels.size) { i ->
                        val (f, label) = filterLabels[i]
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(label) })
                    }
                }
            }

            if (q.isNotEmpty() && dayHits.isEmpty() && transportHits.isEmpty() && expenseHits.isEmpty() && transferHits.isEmpty()) {
                item { EmptyState(Icons.Filled.Search, stringResource(R.string.no_results)) }
            }

            items(dayHits, key = { "d-${it.id}" }) { day ->
                SectionCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text(day.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${stringResource(R.string.type_day)} · ${formatDate(day.dateMillis)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(transportHits, key = { "t-${it.id}" }) { t ->
                SectionCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${t.origin} → ${t.destination}", style = MaterialTheme.typography.bodyLarge)
                            Text(formatDate(t.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.formatWithCurrency(t.fareCents), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(expenseHits, key = { "e-${it.id}" }) { e ->
                SectionCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Payments, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.bodyLarge)
                            Text(formatDate(e.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.formatWithCurrency(e.totalCents), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(transferHits, key = { "x-${it.id}" }) { x ->
                SectionCard {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${records.peopleNames[x.fromPersonId] ?: ""} → ${records.peopleNames[x.toPersonId] ?: ""}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(formatDate(x.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.formatWithCurrency(x.amountCents), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
