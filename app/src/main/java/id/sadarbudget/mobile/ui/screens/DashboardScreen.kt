package id.sadarbudget.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.data.AppRepository
import id.sadarbudget.mobile.data.DashboardData
import id.sadarbudget.mobile.ui.components.*
import id.sadarbudget.mobile.ui.theme.SbDanger
import id.sadarbudget.mobile.ui.theme.SbSuccess
import id.sadarbudget.mobile.ui.theme.sbDesignSystem

@Composable
fun DashboardScreen(repository: AppRepository, userName: String, onSeeAll: () -> Unit, hideAmounts: Boolean = false, onToggleHideAmounts: () -> Unit = {}) {
    var data by remember { mutableStateOf<DashboardData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val compact = isCompactUi()
    val wide = isWideUi()

    LaunchedEffect(refresh) {
        error = null
        runCatching { repository.dashboard() }
            .onSuccess { data = it }
            .onFailure { error = it.message ?: "Gagal memuat ringkasan." }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = pageHorizontalPadding(),
            vertical = pageVerticalPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing()),
    ) {
        if (data == null && error == null) item { LoadingState() }
        if (error != null) item { ErrorState(error!!) { refresh++ } }

        data?.let { d ->
            item {
                DashboardIntro(
                    name = userName.ifBlank { "Pengguna" },
                )
            }
            item {
                BalanceSummaryCard(
                    balance = if (hideAmounts) "Rp •••••••" else formatRupiah(d.balance),
                    monthLabel = d.monthLabel,
                    hidden = hideAmounts,
                    onToggleHidden = onToggleHideAmounts,
                )
            }

            if (wide) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("Pemasukan bulan ini", (if (hideAmounts) "Rp •••••••" else formatRupiah(d.monthIncome)), d.monthLabel, SbSuccess, Modifier.weight(1f))
                        MetricCard("Pengeluaran bulan ini", (if (hideAmounts) "Rp •••••••" else formatRupiah(d.monthExpense)), d.monthLabel, SbDanger, Modifier.weight(1f))
                        MetricCard("Saldo bersih", (if (hideAmounts) "Rp •••••••" else formatRupiah(d.balance)), d.monthLabel, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                }
                if (!hideAmounts) item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        ExpenseControlCard(d, modifier = Modifier.weight(.82f))
                        TrendCard(d, modifier = Modifier.weight(1.18f))
                    }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
                        MetricCard("Pemasukan", (if (hideAmounts) "Rp •••••••" else formatRupiah(d.monthIncome)), d.monthLabel, SbSuccess, Modifier.weight(1f))
                        MetricCard("Pengeluaran", (if (hideAmounts) "Rp •••••••" else formatRupiah(d.monthExpense)), d.monthLabel, SbDanger, Modifier.weight(1f))
                    }
                }
                if (!hideAmounts) { item { ExpenseControlCard(d) }; item { TrendCard(d) } }
            }

            item {
                SbCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("AKTIVITAS TERBARU", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Transaksi terakhir", style = if (wide) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onSeeAll, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text("Lihat semua") }
                    }
                    if (hideAmounts) Text("Aktivitas disembunyikan untuk privasi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else if (d.recent.isEmpty()) Text("Belum ada transaksi untuk ditampilkan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(if (hideAmounts) emptyList() else d.recent.take(if (wide) 4 else d.recent.size)) { transaction ->
                SbCard { TransactionItem(transaction) }
            }
            item { Spacer(Modifier.height(if (wide) 4.dp else 10.dp)) }
        }
    }
}

@Composable
private fun DashboardIntro(name: String) {
    val colors = MaterialTheme.colorScheme
    val small = isSmallPhoneUi()
    val design = sbDesignSystem()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(design.spacing.xs),
    ) {
        Text(
            "RINGKASAN",
            color = colors.primary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Halo, $name",
            style = if (small) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            "Berikut kondisi keuangan Anda saat ini.",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun BalanceSummaryCard(balance: String, monthLabel: String, hidden: Boolean, onToggleHidden: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val small = isSmallPhoneUi()
    val wide = isWideUi()
    val design = sbDesignSystem()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(design.radius.card),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(design.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(design.spacing.sm),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SALDO SAAT INI",
                    modifier = Modifier.weight(1f),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onToggleHidden, modifier = Modifier.size(38.dp)) {
                    Icon(if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        if (hidden) "Tampilkan saldo" else "Sembunyikan saldo")
                }
                Spacer(Modifier.width(design.spacing.xs))
                Surface(
                    shape = RoundedCornerShape(design.radius.pill),
                    color = colors.surfaceVariant,
                    border = CardDefaults.outlinedCardBorder(),
                ) {
                    Text(monthLabel,
                        modifier = Modifier.padding(horizontal = design.spacing.sm, vertical = design.spacing.xs),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant)
                }
            }
            Text(
                balance,
                color = colors.primary,
                style = if (wide) MaterialTheme.typography.displaySmall else if (small) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ExpenseControlCard(d: DashboardData, modifier: Modifier = Modifier) {
    val wide = isWideUi()
    SbCard(modifier) {
        Text("KONTROL PENGELUARAN", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text("Rasio pengeluaran bulanan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(d.expenseRatio?.let { String.format(java.util.Locale("id", "ID"), "%.1f%%", it) } ?: "—", style = if (wide) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            if (wide) Text("dari pemasukan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(d.expenseRatioNote, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = if (wide) 1 else 2)
        LinearProgressIndicator(progress = { (d.expenseRatioProgress / 100.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrendCard(d: DashboardData, modifier: Modifier = Modifier) {
    SbCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TREN ENAM BULAN", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Saldo transaksi kumulatif", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        BalanceLineChart(d.chart)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            d.chart.forEach { Text(it.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
