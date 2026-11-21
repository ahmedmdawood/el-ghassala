package dev.ahmeddawood.elghassala.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ahmeddawood.elghassala.AppViewModel
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.data.db.TransferKind
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.BalanceText
import dev.ahmeddawood.elghassala.ui.components.EmptyState
import dev.ahmeddawood.elghassala.ui.components.GradientHeader
import dev.ahmeddawood.elghassala.ui.components.PressScaleButton
import dev.ahmeddawood.elghassala.ui.nav.Routes

@Composable
fun SettlementScreen(vm: AppViewModel, nav: NavHostController) {
    val records by vm.records.collectAsState()
    val ledgers by vm.ledgers.collectAsState()
    val suggestions by vm.suggestedSettlements.collectAsState()
    val includeClosed by vm.includeClosedDays.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        stringResource(R.string.balances_tab),
        stringResource(R.string.suggestions_tab),
        stringResource(R.string.refunds_tab),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            GradientHeader(
                title = stringResource(R.string.settlement_title),
                subtitle = stringResource(R.string.unsettled_balances),
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { i, label ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, maxLines = 1) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (tab == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = includeClosed,
                            onClick = { vm.includeClosedDays.value = !includeClosed },
                            label = { Text(stringResource(R.string.include_closed_days)) },
                        )
                    }
                }
            }
        }

        when (tab) {
            0 -> {
                val unsettled = ledgers.filterValues { it.netBalanceCents != 0L }
                if (unsettled.isEmpty()) {
                    item { EmptyState(Icons.Filled.Handshake, stringResource(R.string.settled_up)) }
                }
                items(unsettled.entries.toList(), key = { it.key }) { (pid, ledger) ->
                    Box2 {
                        SectionCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                RowSpaceBetween {
                                    Text(records.peopleNames[pid] ?: "#$pid", style = MaterialTheme.typography.titleMedium)
                                    BalanceText(ledger.netBalanceCents, fontSize = 17)
                                }
                                Text(
                                    stringResource(if (ledger.netBalanceCents > 0) R.string.you_are_owed else R.string.you_owe),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                LedgerRow(stringResource(R.string.ledger_transport), ledger.transportNetCents)
                                LedgerRow(stringResource(R.string.ledger_expenses), ledger.expenseNetCents)
                                LedgerRow(stringResource(R.string.ledger_transfers_sent), -ledger.transfersSentCents)
                                LedgerRow(stringResource(R.string.ledger_transfers_received), ledger.transfersReceivedCents)
                                LedgerRow(stringResource(R.string.ledger_settlements), ledger.settlementsReceivedCents - ledger.settlementsSentCents)
                                LedgerRow(stringResource(R.string.ledger_refunds_outstanding), ledger.refundsOutstandingCents)
                            }
                        }
                    }
                }
            }
            1 -> {
                if (suggestions.isEmpty()) {
                    item { EmptyState(Icons.Filled.Handshake, stringResource(R.string.no_debts)) }
                }
                items(suggestions, key = { "${it.fromPersonId}-${it.toPersonId}-${it.amountCents}" }) { edge ->
                    Box2 {
                        SectionCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        "${records.peopleNames[edge.fromPersonId] ?: ""} → ${records.peopleNames[edge.toPersonId] ?: ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(Money.formatWithCurrency(edge.amountCents), color = MaterialTheme.colorScheme.primary)
                                }
                                PressScaleButton(
                                    onClick = {
                                        nav.navigate(
                                            Routes.transferForm(
                                                settlement = true,
                                                from = edge.fromPersonId,
                                                to = edge.toPersonId,
                                                amount = edge.amountCents,
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(stringResource(R.string.record_settlement)) }
                            }
                        }
                    }
                }
            }
            else -> {
                val outstanding = records.refunds.filter { !it.collected }
                if (outstanding.isEmpty()) {
                    item { EmptyState(Icons.Filled.Handshake, stringResource(R.string.no_refunds)) }
                }
                items(outstanding, key = { it.id }) { refund ->
                    val transport = records.transports.firstOrNull { it.id == refund.transportId }
                    Box2 {
                        SectionCard {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${stringResource(R.string.driver_owes)} ${records.peopleNames[refund.personId] ?: ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    if (transport != null) {
                                        Text(
                                            "${transport.origin} → ${transport.destination} · ${formatDate(transport.dateMillis)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(Money.formatWithCurrency(refund.amountCents), color = MaterialTheme.colorScheme.tertiary)
                                TextButton(onClick = { vm.setRefundCollected(refund.id, true) }) {
                                    Text(stringResource(R.string.mark_collected))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Box2(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { content() }
}

@Composable
private fun LedgerRow(label: String, value: Long) {
    if (value == 0L) return
    RowSpaceBetween {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Money.format(value), style = MaterialTheme.typography.bodyMedium)
    }
}
