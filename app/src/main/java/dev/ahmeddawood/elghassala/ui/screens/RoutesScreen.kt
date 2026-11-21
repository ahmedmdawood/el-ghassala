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
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import dev.ahmeddawood.elghassala.ui.components.AmountField
import dev.ahmeddawood.elghassala.ui.components.EmptyState
import dev.ahmeddawood.elghassala.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(vm: AppViewModel, nav: NavHostController) {
    val records by vm.records.collectAsState()
    var editing by remember { mutableStateOf<Long?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.routes_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_route)) },
            )
        },
    ) { padding ->
        if (records.routes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(Icons.Filled.Route, stringResource(R.string.no_routes_yet))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(records.routes, key = { it.id }) { route ->
                    SectionCard {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Route, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(route.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    "${route.origin} → ${route.destination} · ${Money.formatWithCurrency(route.defaultFareCents)} · ${route.usageCount} ${stringResource(R.string.used_count)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                nav.navigate(
                                    Routes.transportForm()
                                )
                            }) {
                                Icon(Icons.Filled.DirectionsBus, stringResource(R.string.start_trip), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { editing = route.id; showDialog = true }) {
                                Icon(Icons.Filled.Edit, stringResource(R.string.edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { pendingDelete = route.id }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val existing = records.routes.firstOrNull { it.id == editing }
        var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
        var origin by remember(existing?.id) { mutableStateOf(existing?.origin ?: "") }
        var destination by remember(existing?.id) { mutableStateOf(existing?.destination ?: "") }
        var fare by remember(existing?.id) { mutableStateOf(existing?.let { Money.format(it.defaultFareCents) } ?: "") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(if (existing != null) R.string.edit_route else R.string.new_route)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.route_name_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(origin, { origin = it }, label = { Text(stringResource(R.string.origin)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(destination, { destination = it }, label = { Text(stringResource(R.string.destination)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    AmountField(fare, { fare = it }, stringResource(R.string.default_fare))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val fareCents = Money.parse(fare) ?: 0L
                    if (name.isNotBlank() && origin.isNotBlank() && destination.isNotBlank() && fareCents > 0) {
                        vm.saveRoute(existing?.id, name, origin, destination, fareCents)
                        showDialog = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            text = stringResource(R.string.delete_route_confirm),
            onConfirm = { vm.deleteRoute(id) },
            onDismiss = { pendingDelete = null },
        )
    }
}
