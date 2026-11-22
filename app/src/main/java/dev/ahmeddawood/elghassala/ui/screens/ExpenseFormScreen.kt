package dev.ahmeddawood.elghassala.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.ahmeddawood.elghassala.data.repo.ExpenseInput
import dev.ahmeddawood.elghassala.domain.FinanceEngine
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.components.AmountField
import dev.ahmeddawood.elghassala.ui.components.DateTimeField
import dev.ahmeddawood.elghassala.ui.components.FormSectionTitle
import dev.ahmeddawood.elghassala.ui.components.PersonMultiSelect
import dev.ahmeddawood.elghassala.ui.components.PressScaleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(vm: AppViewModel, nav: NavHostController, dayId: Long?, editId: Long?) {
    val records by vm.records.collectAsState()
    val people = records.activePeople

    var title by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var payers by remember { mutableStateOf(setOf<Long>()) }
    var payerAmounts by remember { mutableStateOf(mapOf<Long, String>()) }
    var beneficiaries by remember { mutableStateOf(setOf<Long>()) }
    var equalSplit by remember { mutableStateOf(true) }
    var shareAmounts by remember { mutableStateOf(mapOf<Long, String>()) }
    var loaded by remember { mutableStateOf(editId == null) }

    LaunchedEffect(editId, records.expenses) {
        if (!loaded && editId != null) {
            val e = records.expenses.firstOrNull { it.id == editId } ?: return@LaunchedEffect
            title = e.title
            total = Money.format(e.totalCents)
            notes = e.notes
            dateMillis = e.dateMillis
            val pay = vm.expensePayers(editId)
            payers = pay.map { it.personId }.toSet()
            payerAmounts = pay.associate { it.personId to Money.format(it.paidCents) }
            val ben = vm.expenseBeneficiaries(editId)
            beneficiaries = ben.map { it.personId }.toSet()
            shareAmounts = ben.associate { it.personId to Money.format(it.shareCents) }
            equalSplit = false
            loaded = true
        }
    }

    val totalCents = Money.parse(total) ?: 0L
    val payerSum = payers.sumOf { Money.parse(payerAmounts[it]) ?: 0L }
    val shares: Map<Long, Long> = if (equalSplit) FinanceEngine.equalShares(totalCents, beneficiaries)
    else beneficiaries.associateWith { Money.parse(shareAmounts[it]) ?: 0L }
    val shareSum = shares.values.sum()
    val saveEnabled = title.isNotBlank() && totalCents > 0 && payers.isNotEmpty() && beneficiaries.isNotEmpty() &&
        payerSum == totalCents && shareSum == totalCents

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (editId != null) R.string.edit_expense else R.string.new_expense)) },
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
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.expense_title_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            AmountField(total, { total = it }, stringResource(R.string.total_amount), isError = total.isNotBlank() && totalCents <= 0)
            DateTimeField(dateMillis, { dateMillis = it }, stringResource(R.string.date))
            OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth())

            FormSectionTitle(stringResource(R.string.who_paid))
            PersonMultiSelect(people, payers) { pid ->
                payers = if (pid in payers) payers - pid else payers + pid
                payerAmounts = if (pid in payers) payerAmounts - pid
                else payerAmounts + (pid to if (payers.size == 1) total else "")
            }
            payers.forEach { pid ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(records.peopleNames[pid] ?: "#$pid", modifier = Modifier.weight(1f), maxLines = 1)
                    OutlinedTextField(
                        value = payerAmounts[pid] ?: "",
                        onValueChange = { v -> payerAmounts = payerAmounts + (pid to v) },
                        modifier = Modifier.padding(start = 8.dp).fillMaxWidth(0.45f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        label = { Text(stringResource(R.string.pays)) },
                    )
                }
            }
            if (payers.size > 1 && totalCents > 0) {
                Text(
                    "${stringResource(R.string.actual_paid)}: ${Money.shown(payerSum)} / ${Money.shown(totalCents)}",
                    color = if (payerSum == totalCents) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }

            FormSectionTitle(stringResource(R.string.beneficiaries))
            PersonMultiSelect(people, beneficiaries) { pid ->
                beneficiaries = if (pid in beneficiaries) beneficiaries - pid else beneficiaries + pid
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Switch(checked = equalSplit, onCheckedChange = { equalSplit = it })
                Spacer(Modifier.padding(4.dp))
                Text(stringResource(R.string.equal_split))
            }
            if (!equalSplit) {
                beneficiaries.forEach { pid ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(records.peopleNames[pid] ?: "#$pid", modifier = Modifier.weight(1f), maxLines = 1)
                        OutlinedTextField(
                            value = shareAmounts[pid] ?: "",
                            onValueChange = { v -> shareAmounts = shareAmounts + (pid to v) },
                            modifier = Modifier.padding(start = 8.dp).fillMaxWidth(0.45f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            label = { Text(stringResource(R.string.share)) },
                        )
                    }
                }
            }
            if (beneficiaries.isNotEmpty() && totalCents > 0 && shareSum != totalCents) {
                Text(
                    stringResource(R.string.shares_must_equal_total),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PressScaleButton(
                onClick = {
                    vm.saveExpense(
                        editId,
                        ExpenseInput(
                            dayId = dayId,
                            title = title,
                            totalCents = totalCents,
                            payers = payers.associateWith { Money.parse(payerAmounts[it]) ?: 0L },
                            shares = shares,
                            dateMillis = dateMillis,
                            notes = notes,
                        ),
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
