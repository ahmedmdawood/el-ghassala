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
import androidx.compose.material.icons.filled.Edit
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
fun TransportScreen(vm: AppViewModel, nav: NavHostController) {
    val records by vm.records.collectAsState()
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transport_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.transportForm()) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_transport)) },
            )
        },
    ) { padding ->
        if (records.transports.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(Icons.Filled.DirectionsBus, stringResource(R.string.no_transport_yet))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(records.transports, key = { it.id }) { t ->
                    val riders = records.participants.filter { it.transportId == t.id }
                    val refunds = records.refunds.filter { it.transportId == t.id && !it.collected }
                    SectionCard {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DirectionsBus, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${t.origin} → ${t.destination}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${formatDateTime(t.dateMillis)} · ${riders.size} ${stringResource(R.string.participants)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    Money.shown(t.fareCents * riders.size.coerceAtLeast(1)),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            if (riders.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                riders.forEach { p ->
                                    RowSpaceBetween {
                                        Text(records.peopleNames[p.personId] ?: "#${p.personId}", style = MaterialTheme.typography.bodyMedium)
                                        Text(Money.shown(p.paidCents), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            if (refunds.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                refunds.forEach { r ->
                                    Text(
                                        "${stringResource(R.string.driver_owes)} ${records.peopleNames[r.personId] ?: ""}: ${Money.shown(r.amountCents)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = { nav.navigate(Routes.transportForm(editId = t.id)) }) {
                                    Icon(Icons.Filled.Edit, stringResource(R.string.edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { pendingDelete = t.id }) {
                                    Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
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
            text = stringResource(R.string.delete_transport_confirm),
            onConfirm = { vm.deleteTransport(id) },
            onDismiss = { pendingDelete = null },
        )
    }
}
