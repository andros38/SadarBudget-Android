package id.sadarbudget.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.data.AppRepository
import id.sadarbudget.mobile.data.Transaction
import id.sadarbudget.mobile.data.TransactionsData
import id.sadarbudget.mobile.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(repository: AppRepository, onEdit: (Long) -> Unit, onNotify: (String) -> Unit = {}, snackbarHostState: SnackbarHostState? = null) {
    var month by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var appliedSearch by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var data by remember { mutableStateOf<TransactionsData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var deleteTarget by remember { mutableStateOf<Transaction?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var exportBusy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wide = isWideUi()
    val compact = isCompactUi()

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val bytes = pendingBytes
        if (uri != null && bytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
                .onSuccess { message = "Berkas berhasil disimpan."; onNotify("Berkas berhasil disimpan.") }
                .onFailure { message = "Gagal menyimpan berkas: ${it.message}" }
        }
        pendingBytes = null
    }

    LaunchedEffect(search) { delay(250); appliedSearch = search }
    LaunchedEffect(month, type, page, appliedSearch, refresh) {
        error = null
        runCatching { repository.transactions(month, type, page, appliedSearch) }
            .onSuccess { data = it; page = it.page }
            .onFailure { error = it.message ?: "Gagal memuat riwayat transaksi." }
    }

    fun export(kind: String) {
        exportBusy = true
        scope.launch {
            runCatching {
                if (kind == "pdf") repository.reportPdf(month, type) else repository.reportExcel(month, type)
            }.onSuccess { bytes ->
                pendingBytes = bytes
                val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())
                createDocument.launch("sadarbudget-laporan-$stamp.${if (kind == "pdf") "pdf" else "xlsx"}")
            }.onFailure { message = it.message ?: "Ekspor gagal." }
            exportBusy = false
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = pageHorizontalPadding(),
            vertical = pageVerticalPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing()),
    ) {
        item {
            PageTitle(
                eyebrow = "Catatan transaksi",
                title = when (type) { "income" -> "Riwayat Pemasukan"; "expense" -> "Riwayat Pengeluaran"; else -> "Riwayat Keuangan" },
            )
        }
        if (message != null) item { MessageBanner(message!!, message!!.startsWith("Gagal")) }
        item {
            SbCard {
                if (!wide) Text("Filter transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(80); page = 1 },
                    label = { Text("Cari transaksi") },
                    placeholder = { Text("Kategori, keterangan, nominal, tanggal") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                if (wide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MonthFilter(month, { month = it; page = 1 }, Modifier.weight(1.15f))
                        TypeFilter(type, { type = it; page = 1 }, Modifier.weight(.85f))
                        SbSecondaryButton(onClick = { month = ""; type = ""; search = ""; page = 1 }, modifier = Modifier.height(controlHeight())) { Text("Reset") }
                        SbPrimaryButton(onClick = { refresh++ }, modifier = Modifier.height(controlHeight())) { Text("Terapkan") }
                        SbSecondaryButton(onClick = { export("excel") }, enabled = !exportBusy, modifier = Modifier.height(controlHeight())) {
                            Icon(Icons.Outlined.TableChart, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Excel")
                        }
                        SbSecondaryButton(onClick = { export("pdf") }, enabled = !exportBusy, modifier = Modifier.height(controlHeight())) {
                            Icon(Icons.Outlined.PictureAsPdf, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("PDF")
                        }
                    }
                } else {
                    MonthFilter(month, { month = it; page = 1 })
                    TypeFilter(type, { type = it; page = 1 })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SbSecondaryButton(onClick = { month = ""; type = ""; search = ""; page = 1 }, modifier = Modifier.weight(1f)) { Text("Reset") }
                        SbPrimaryButton(onClick = { refresh++ }, modifier = Modifier.weight(1f)) { Text("Terapkan") }
                    }
                }
            }
        }
        if (!wide) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SbSecondaryButton(onClick = { export("excel") }, enabled = !exportBusy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.TableChart, null); Spacer(Modifier.width(6.dp)); Text("Excel")
                    }
                    SbSecondaryButton(onClick = { export("pdf") }, enabled = !exportBusy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("PDF")
                    }
                }
            }
        }
        if (data == null && error == null) item { LoadingState() }
        if (error != null) item { ErrorState(error!!) { refresh++ } }
        data?.let { d ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${d.total} transaksi", fontWeight = FontWeight.SemiBold, style = if (wide) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
                    Text("Halaman ${d.page} dari ${d.totalPages}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (d.rows.isEmpty()) item {
                SbCard(Modifier.fillMaxWidth()) {
                    Text("Tidak ada transaksi untuk filter ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(d.rows, key = { it.id }) { transaction ->
                SbCard {
                    TransactionItem(transaction) {
                        Row {
                            IconButton(onClick = { onEdit(transaction.id) }, modifier = Modifier.size(if (wide) 30.dp else 34.dp)) { Icon(Icons.Outlined.Edit, "Ubah", modifier = Modifier.size(16.dp)) }
                            IconButton(onClick = { deleteTarget = transaction }, modifier = Modifier.size(if (wide) 30.dp else 34.dp)) { Icon(Icons.Outlined.Delete, "Hapus", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (wide) Arrangement.Start else Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SbSecondaryButton(
                        onClick = { page-- },
                        enabled = page > 1,
                        modifier = if (wide) Modifier else Modifier.weight(1f),
                        contentPadding = if (wide) PaddingValues(horizontal = 12.dp, vertical = 6.dp) else null,
                    ) { Text("Sebelumnya") }
                    if (wide) Spacer(Modifier.width(8.dp))
                    SbSecondaryButton(
                        onClick = { page++ },
                        enabled = page < d.totalPages,
                        modifier = if (wide) Modifier else Modifier.weight(1f),
                        contentPadding = if (wide) PaddingValues(horizontal = 12.dp, vertical = 6.dp) else null,
                    ) { Text("Berikutnya") }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus transaksi?") },
            text = { Text("${typeLabel(target.type)} ${formatRupiah(target.amount)} pada ${formatDateId(target.transactionDate)} akan dihapus. Riwayat backup tetap mengikuti aturan SadarBudget.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        runCatching { repository.deleteTransaction(target.id) }
                            .onSuccess {
                                message = null; refresh++
                                if (snackbarHostState != null) {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Transaksi dihapus.", actionLabel = "Urungkan",
                                            withDismissAction = true, duration = SnackbarDuration.Long,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            runCatching { repository.restoreDeletedTransaction(target) }
                                                .onSuccess { onNotify(it); refresh++ }
                                                .onFailure { message = "Gagal memulihkan: ${it.message}" }
                                        }
                                    }
                                } else onNotify(it)
                            }
                            .onFailure { message = it.message ?: "Gagal menghapus transaksi." }
                    }
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthFilter(value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = if (value.isBlank()) "Semua periode" else value,
        onValueChange = {},
        readOnly = true,
        label = { Text("Bulan") },
        trailingIcon = { IconButton(onClick = { show = true }) { Icon(Icons.Outlined.CalendarMonth, null) } },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
    if (show) {
        val initialMillis = runCatching {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            java.time.LocalDate.parse((if (value.isBlank()) java.time.LocalDate.now().toString().substring(0, 7) else value) + "-01", fmt)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        val selected = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        onValue(selected.toString().substring(0, 7))
                    }
                    show = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { onValue(""); show = false }) { Text("Semua") } },
        ) { DatePicker(state = picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeFilter(value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = when (value) { "income" -> "Pemasukan"; "expense" -> "Pengeluaran"; else -> "Semua jenis" },
            onValueChange = {}, readOnly = true, label = { Text("Jenis") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("" to "Semua jenis", "income" to "Pemasukan", "expense" to "Pengeluaran").forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onValue(key); expanded = false })
            }
        }
    }
}
