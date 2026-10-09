package id.sadarbudget.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.data.AppRepository
import id.sadarbudget.mobile.data.Category
import id.sadarbudget.mobile.data.TransactionSaveRequest
import id.sadarbudget.mobile.data.TransactionTemplate
import id.sadarbudget.mobile.ui.components.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    repository: AppRepository,
    type: String?,
    transactionId: Long?,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val isEdit = transactionId != null
    var resolvedType by remember { mutableStateOf(type ?: "") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var description by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var categoryId by remember { mutableStateOf<Int?>(null) }
    var historicalLabel by remember { mutableStateOf<String?>(null) }
    var templates by remember { mutableStateOf<List<TransactionTemplate>>(emptyList()) }
    var templateName by remember { mutableStateOf("") }
    var templateNotice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(transactionId, type) {
        loading = true; error = null
        runCatching {
            if (transactionId != null) {
                val data = repository.transaction(transactionId)
                resolvedType = data.transaction.type
                amount = formatAmountInput(data.transaction.amount)
                date = data.transaction.transactionDate
                description = data.transaction.description.orEmpty()
                categoryId = data.transaction.categoryId
                historicalLabel = if (data.transaction.categoryId == null) data.transaction.categoryName else null
                categories = data.categories
            } else {
                resolvedType = type ?: "income"
                categories = repository.categories(resolvedType)
                categoryId = categories.firstOrNull()?.id
                templates = repository.transactionTemplates(resolvedType)
            }
        }.onFailure { error = it.message ?: "Gagal menyiapkan formulir." }
        loading = false
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = pageHorizontalPadding(), vertical = pageVerticalPadding()).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing()),
    ) {
        PageTitle(
            eyebrow = if (isEdit) "Perbarui transaksi" else "Catatan baru",
            title = if (isEdit) "Ubah ${typeLabel(resolvedType)}" else "Catat ${typeLabel(resolvedType)}",
            subtitle = if (isEdit) "Perbarui nominal, tanggal, kategori, atau keterangan." else "Tambahkan transaksi ke riwayat SadarBudget.",
        )

        if (loading) LoadingState()
        if (error != null) MessageBanner(error!!, true)

        if (!loading && error == null) {
            if (!isEdit && templates.isNotEmpty()) SbCard {
                Text("Template cepat", style = MaterialTheme.typography.titleMedium)
                Text("Isi formulir dari template yang disimpan. Periksa kembali sebelum mencatat.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                templates.forEach { template ->
                    val available = categories.any { it.id == template.categoryId && it.isActive }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SbSecondaryButton(onClick = {
                            amount = formatDigits(template.amount)
                            categoryId = template.categoryId
                            description = template.description
                            templateNotice = "Template ${template.name} diterapkan."
                        }, enabled = available, modifier = Modifier.weight(1f)) {
                            Text("${template.name} · Rp ${formatDigits(template.amount)}", maxLines = 1)
                        }
                        TextButton(onClick = {
                            repository.deleteTransactionTemplate(template)
                            templates = repository.transactionTemplates(resolvedType)
                            templateNotice = "Template dihapus."
                        }) { Text("Hapus") }
                    }
                }
            }
            SbCard {
                OutlinedTextField(value = typeLabel(resolvedType), onValueChange = {}, readOnly = true, label = { Text("Jenis transaksi") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = formatDigits(it) },
                    label = { Text("Nominal") },
                    prefix = { Text("Rp ") },
                    placeholder = { Text("1.500.000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                DateField(date) { date = it }
                CategoryField(categories, categoryId, historicalLabel, onSelected = { categoryId = it })
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 255) description = it },
                    label = { Text("Keterangan") },
                    placeholder = { Text("Contoh: Gaji Juli, makan siang, bensin…") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isEdit) {
                    HorizontalDivider()
                    Text("Simpan isian sebagai template", style = MaterialTheme.typography.titleSmall)
                    Text("Template hanya mengisi formulir dan tidak membuat transaksi otomatis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = templateName, onValueChange = { templateName = it.take(40) }, label = { Text("Nama template (contoh: Paket data)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    SbSecondaryButton(onClick = {
                        runCatching {
                            repository.saveTransactionTemplate(TransactionTemplate(
                                name = templateName.trim(), type = resolvedType,
                                amount = amount.filter(Char::isDigit),
                                categoryId = categoryId ?: throw IllegalArgumentException("Pilih kategori."),
                                description = description.trim(),
                            ))
                        }.onSuccess { templateNotice = "Template disimpan."; templateName = ""; templates = repository.transactionTemplates(resolvedType) }
                            .onFailure { templateNotice = it.message ?: "Template gagal disimpan." }
                    }, enabled = templateName.trim().length >= 2 && amount.filter(Char::isDigit).isNotBlank() && categoryId != null) { Text("Simpan template") }
                    if (templateNotice != null) Text(templateNotice!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SbSecondaryButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !saving) { Text("Batal") }
                    SbPrimaryButton(
                        onClick = {
                            saving = true; error = null
                            scope.launch {
                                val body = TransactionSaveRequest(
                                    id = transactionId,
                                    type = if (transactionId == null) resolvedType else null,
                                    amount = amount,
                                    transactionDate = date,
                                    categoryId = categoryId,
                                    description = description.trim(),
                                )
                                runCatching {
                                    if (transactionId == null) repository.createTransaction(body) else repository.updateTransaction(body)
                                }.onSuccess { onDone(it) }
                                    .onFailure { error = it.message ?: "Transaksi gagal disimpan." }
                                saving = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !saving && amount.filter(Char::isDigit).isNotBlank() && date.isNotBlank() && (categoryId != null || historicalLabel != null),
                    ) { Text(if (saving) "Menyimpan…" else if (isEdit) "Simpan perubahan" else "Simpan") }
                }
            }
        }
    }
}

private fun formatDigits(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(15)
    if (digits.isBlank()) return ""
    return NumberFormat.getIntegerInstance(Locale("id", "ID")).format(digits.toLongOrNull() ?: 0)
}

private fun formatAmountInput(value: Double): String = NumberFormat.getIntegerInstance(Locale("id", "ID")).format(value.toLong())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(value: String, onValue: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = {}, readOnly = true, label = { Text("Tanggal") }, modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = { show = true }) { Icon(Icons.Outlined.CalendarMonth, null) } },
    )
    if (show) {
        val millis = runCatching { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { onValue(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }; show = false }) { Text("Pilih") } },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Batal") } },
        ) { DatePicker(state = picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(categories: List<Category>, selectedId: Int?, historicalLabel: String?, onSelected: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    val label = selected?.let { it.name + if (!it.isActive) " (nonaktif)" else "" }
        ?: historicalLabel?.let { "Pertahankan kategori historis: $it" }
        ?: "Pilih kategori"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = label, onValueChange = {}, readOnly = true, label = { Text("Kategori") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (historicalLabel != null) DropdownMenuItem(text = { Text("Pertahankan kategori historis: $historicalLabel") }, onClick = { onSelected(null); expanded = false })
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category.name + if (!category.isActive) " (nonaktif)" else "") }, onClick = { onSelected(category.id); expanded = false })
            }
        }
    }
}
