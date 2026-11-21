package dev.ahmeddawood.elghassala.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import dev.ahmeddawood.elghassala.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(vm: AppViewModel, nav: NavHostController, dayId: Long) {
    val records by vm.records.collectAsState()
    val day = records.days.firstOrNull { it.id == dayId }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }

    if (day == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.nothing_here_yet))
        }
        return
    }

    val dayTransports = records.transports.filter { it.dayId == dayId }
    val dayExpenses = records.expenses.filter { it.dayId == dayId }
    val dayTransfers = records.transfers.filter { it.dayId == dayId }
    val dayLedgers = remember(records, dayId) { vm.ledgerForDay(records, dayId) }
    val dayTotal = dayTransports.sumOf { t -> t.fareCents * records.participants.count { it.transportId == t.id }.coerceAtLeast(1) } +
        dayExpenses.sumOf { it.totalCents }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(day.title) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { confirmClose = true }) {
                        Icon(
                            if (day.isClosed) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            stringResource(if (day.isClosed) R.string.reopen_day else R.string.close_day),
                        )
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_day)) },
                            onClick = { menuOpen = false; confirmDelete = true },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!day.isClosed) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate(Routes.transportForm(dayId = dayId)) },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.start_trip)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LabeledValue(stringResource(R.string.date), formatDate(day.dateMillis))
                        LabeledValue(
                            stringResource(if (day.isClosed) R.string.day_closed else R.string.day_open),
                            Money.formatWithCurrency(dayTotal),
                        )
                    }
                }
            }

            if (dayLedgers.isNotEmpty()) {
                item {
                    SectionCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.day_participants), style = MaterialTheme.typography.titleMedium)
                            dayLedgers.forEach { (pid, ledger) ->
                                RowSpaceBetween {
                                    Text(records.peopleNames[pid] ?: "#$pid")
                                    dev.ahmeddawood.elghassala.ui.components.BalanceText(ledger.netBalanceCents)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.day_transactions), style = MaterialTheme.typography.titleMedium)
            }

            if (dayTransports.isEmpty() && dayExpenses.isEmpty() && dayTransfers.isEmpty()) {
                item { EmptyState(Icons.Filled.DirectionsBus, stringResource(R.string.no_activity_yet)) }
            }

            items(dayTransports, key = { "t-${it.id}" }) { t ->
                SectionCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${t.origin} → ${t.destination}", style = MaterialTheme.typography.titleMedium)
                            Text(formatDateTime(t.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.formatWithCurrency(t.fareCents), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(dayExpenses, key = { "e-${it.id}" }) { e ->
                SectionCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Payments, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.titleMedium)
                            Text(formatDateTime(e.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.formatWithCurrency(e.totalCents), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(dayTransfers, key = { "x-${it.id}" }) { x ->
                SectionCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${records.peopleNames[x.fromPersonId] ?: ""} → ${records.peopleNames[x.toPersonId] ?: ""}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(formatDateTime(x.dateMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Money.formatWithCurrency(x.amountCents), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (confirmClose) {
        ConfirmDialog(
            title = stringResource(if (day.isClosed) R.string.reopen_day else R.string.close_day),
            text = stringResource(R.string.close_day_confirm),
            onConfirm = { vm.setDayClosed(dayId, !day.isClosed) },
            onDismiss = { confirmClose = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_day),
            text = stringResource(R.string.delete_day_confirm),
            onConfirm = {
                vm.deleteDay(dayId)
                nav.popBackStack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
