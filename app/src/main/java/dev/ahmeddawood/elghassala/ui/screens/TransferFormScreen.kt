package dev.ahmeddawood.elghassala.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.ahmeddawood.elghassala.AppViewModel
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.data.db.TransferKind
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.AmountField
import dev.ahmeddawood.elghassala.ui.components.DateTimeField
import dev.ahmeddawood.elghassala.ui.components.FormSectionTitle
import dev.ahmeddawood.elghassala.ui.components.PersonSingleSelect
import dev.ahmeddawood.elghassala.ui.components.PressScaleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFormScreen(
    vm: AppViewModel,
    nav: NavHostController,
    dayId: Long?,
    editId: Long?,
    presetSettlement: Boolean,
    presetFrom: Long,
    presetTo: Long,
    presetAmount: Long,
) {
    val records by vm.records.collectAsState()
    val people = records.activePeople

    var isSettlement by remember { mutableStateOf(presetSettlement) }
    var fromId by remember { mutableStateOf(if (presetFrom > 0) presetFrom else null) }
    var toId by remember { mutableStateOf(if (presetTo > 0) presetTo else null) }
    var amount by remember { mutableStateOf(if (presetAmount > 0) Money.format(presetAmount) else "") }
    var notes by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val amountCents = Money.parse(amount) ?: 0L
    val saveEnabled = fromId != null && toId != null && fromId != toId && amountCents > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isSettlement) R.string.record_settlement else R.string.new_transfer)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isSettlement,
                    onClick = { isSettlement = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.type_transfer)) }
                SegmentedButton(
                    selected = isSettlement,
                    onClick = { isSettlement = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.type_settlement)) }
            }

            FormSectionTitle(stringResource(R.string.from_person))
            PersonSingleSelect(people, fromId) { fromId = it }
            FormSectionTitle(stringResource(R.string.to_person))
            PersonSingleSelect(people.filter { it.id != fromId }, toId) { toId = it }

            AmountField(amount, { amount = it }, stringResource(R.string.amount), isError = amount.isNotBlank() && amountCents <= 0)
            DateTimeField(dateMillis, { dateMillis = it }, stringResource(R.string.date))
            OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())

            Text(
                stringResource(R.string.transfers_not_expenses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PressScaleButton(
                onClick = {
                    vm.saveTransfer(
                        editId, dayId, fromId!!, toId!!, amountCents,
                        if (isSettlement) TransferKind.SETTLEMENT else TransferKind.TRANSFER,
                        dateMillis, notes,
                    )
                    nav.popBackStack()
                },
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
