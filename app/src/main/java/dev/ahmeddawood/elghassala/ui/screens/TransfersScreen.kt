package dev.ahmeddawood.elghassala.ui.screens

import dev.ahmeddawood.elghassala.ui.arrow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
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
import dev.ahmeddawood.elghassala.data.db.TransferKind
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.EmptyState
import dev.ahmeddawood.elghassala.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(vm: AppViewModel, nav: NavHostController) {
    val records by vm.records.collectAsState()
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transfers_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.transferForm()) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_transfer)) },
            )
        },
    ) { padding ->
        if (records.transfers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(Icons.Filled.SwapHoriz, stringResource(R.string.no_transfers_yet))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.transfers_not_expenses),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(records.transfers, key = { it.id }) { t ->
                    SectionCard {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.SwapHoriz, null,
                                tint = if (t.kind == TransferKind.SETTLEMENT) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${records.peopleNames[t.fromPersonId] ?: ""} ${arrow()} ${records.peopleNames[t.toPersonId] ?: ""}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "${formatDateTime(t.dateMillis)} · ${stringResource(if (t.kind == TransferKind.SETTLEMENT) R.string.type_settlement else R.string.type_transfer)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(Money.shown(t.amountCents), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { pendingDelete = t.id }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            text = stringResource(R.string.delete_transfer_confirm),
            onConfirm = { vm.deleteTransfer(id) },
            onDismiss = { pendingDelete = null },
        )
    }
}
