@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package id.sadarbudget.mobile

import androidx.activity.compose.BackHandler
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import id.sadarbudget.mobile.ui.components.*
import id.sadarbudget.mobile.ui.screens.*
import id.sadarbudget.mobile.ui.theme.SadarBudgetTheme
import id.sadarbudget.mobile.ui.theme.sbDesignSystem
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Dashboard : AppScreen
    data object Transactions : AppScreen
    data object Annual : AppScreen
    data object Categories : AppScreen
    data object Profile : AppScreen
    data object DataBackup : AppScreen
    data object About : AppScreen
    data object Security : AppScreen
    data class AddTransaction(val type: String) : AppScreen
    data class EditTransaction(val id: Long) : AppScreen
}

@Composable
fun SadarBudgetApp() {
    val context = LocalContext.current.applicationContext
    val state = remember { AppState(context) }
    LaunchedEffect(Unit) { state.verifySession() }
    val lifecycleOwner = LocalContext.current as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) state.lockIfEnabled()
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity, state.secureWindow, state.locked) {
        val window = activity?.window
        if (state.secureWindow || state.locked) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }

    SadarBudgetTheme(darkTheme = state.darkTheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                state.checkingSession -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                !state.loggedIn -> AuthFlow(appState = state)
                state.locked -> AppLockScreen(state)
                else -> MainShell(state)
            }
        }
    }
}

@Composable
private fun MainShell(appState: AppState) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
    var showCreate by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wide = isWideUi()
    val small = isSmallPhoneUi()
    val notify: (String) -> Unit = { text ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message = text, duration = SnackbarDuration.Short)
        }
    }

    BackHandler(screen !is AppScreen.Dashboard) { screen = AppScreen.Dashboard }

    val body: @Composable (Modifier) -> Unit = { modifier ->
        Box(modifier) {
            when (val current = screen) {
                AppScreen.Dashboard -> DashboardScreen(appState.repository, appState.user?.name.orEmpty(), onSeeAll = { screen = AppScreen.Transactions }, hideAmounts = appState.hideAmounts, onToggleHideAmounts = { appState.updateHideAmounts(!appState.hideAmounts) })
                AppScreen.Transactions -> TransactionsScreen(appState.repository, onEdit = { screen = AppScreen.EditTransaction(it) }, onNotify = notify, snackbarHostState = snackbar)
                AppScreen.Annual -> AnnualScreen(appState.repository)
                AppScreen.Categories -> CategoriesScreen(appState.repository, onNotify = notify)
                AppScreen.Profile -> ProfileScreen(appState.repository, initialUser = appState.user, onUserChanged = { appState.user = it }, onNotify = notify)
                AppScreen.DataBackup -> DataBackupScreen(appState.repository, onNotify = notify)
                AppScreen.About -> AboutScreen()
                AppScreen.Security -> AppSecurityScreen(appState)
                is AppScreen.AddTransaction -> TransactionFormScreen(appState.repository, type = current.type, transactionId = null, onDone = { message -> notify(message); screen = AppScreen.Transactions }, onCancel = { screen = AppScreen.Transactions })
                is AppScreen.EditTransaction -> TransactionFormScreen(appState.repository, type = null, transactionId = current.id, onDone = { message -> notify(message); screen = AppScreen.Transactions }, onCancel = { screen = AppScreen.Transactions })
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SadarTopBar(
                userName = appState.user?.name.orEmpty(),
                profilePhoto = appState.user?.profilePhoto,
                darkTheme = appState.darkTheme,
                compact = wide || small,
                onToggleTheme = { appState.updateDarkTheme(!appState.darkTheme) },
                onProfile = { screen = AppScreen.Profile },
            )
        },
        bottomBar = {
            if (!wide) {
                CompactBottomBar(
                    screen = screen,
                    onDashboard = { screen = AppScreen.Dashboard },
                    onTransactions = { screen = AppScreen.Transactions },
                    onCreate = { showCreate = true },
                    onAnnual = { screen = AppScreen.Annual },
                    onAccount = { showAccount = true },
                    small = small,
                )
            }
        },
    ) { padding ->
        if (wide) {
            Row(Modifier.fillMaxSize().padding(padding)) {
                CompactNavigationRail(
                    screen = screen,
                    onDashboard = { screen = AppScreen.Dashboard },
                    onTransactions = { screen = AppScreen.Transactions },
                    onCreate = { showCreate = true },
                    onAnnual = { screen = AppScreen.Annual },
                    onAccount = { showAccount = true },
                )
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.fillMaxSize().widthIn(max = 1180.dp).align(Alignment.TopCenter)) {
                        body(Modifier.fillMaxSize())
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding)) {
                body(Modifier.fillMaxSize())
            }
        }
    }

    if (showCreate) {
        TransactionQuickPopup(
            wide = wide,
            small = small,
            onDismiss = { showCreate = false },
            onIncome = {
                showCreate = false
                screen = AppScreen.AddTransaction("income")
            },
            onExpense = {
                showCreate = false
                screen = AppScreen.AddTransaction("expense")
            },
        )
    }

    if (showAccount) {
        if (wide) {
            AlertDialog(
                onDismissRequest = { showAccount = false },
                title = { Text("Menu akun") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        AccountMenuItems(
                            onProfile = { showAccount = false; screen = AppScreen.Profile },
                            onCategories = { showAccount = false; screen = AppScreen.Categories },
                            onBackup = { showAccount = false; screen = AppScreen.DataBackup },
                            onSecurity = { showAccount = false; screen = AppScreen.Security },
                            onAbout = { showAccount = false; screen = AppScreen.About },
                            onLogout = { showAccount = false; scope.launch { appState.logout() } },
                            compact = true,
                        )
                    }
                },
                confirmButton = {},
            )
        } else {
            ModalBottomSheet(onDismissRequest = { showAccount = false }) {
                Column(Modifier.fillMaxWidth().padding(horizontal = if (small) 14.dp else 20.dp).padding(bottom = if (small) 18.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(if (small) 4.dp else 6.dp)) {
                    Text("Menu akun", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    AccountMenuItems(
                        onProfile = { showAccount = false; screen = AppScreen.Profile },
                        onCategories = { showAccount = false; screen = AppScreen.Categories },
                        onBackup = { showAccount = false; screen = AppScreen.DataBackup },
                        onSecurity = { showAccount = false; screen = AppScreen.Security },
                        onAbout = { showAccount = false; screen = AppScreen.About },
                        onLogout = { showAccount = false; scope.launch { appState.logout() } },
                        compact = small,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionQuickPopup(
    wide: Boolean,
    small: Boolean,
    onDismiss: () -> Unit,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val design = sbDesignSystem()
    val popupWidthDp = if (wide) 360 else (configuration.screenWidthDp - 24).coerceIn(260, 360)
    val bottomOffsetPx = with(density) {
        (if (small) 70.dp else 78.dp).roundToPx()
    }
    val railOffsetPx = with(density) { (design.railWidth + 16.dp).roundToPx() }

    Popup(
        alignment = if (wide) Alignment.CenterStart else Alignment.BottomCenter,
        offset = if (wide) IntOffset(railOffsetPx, 0) else IntOffset(0, -bottomOffsetPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = true,
        ),
    ) {
        Column(
            modifier = Modifier.width(popupWidthDp.dp),
            horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(if (small) 18.dp else 20.dp),
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant),
                shadowElevation = if (colors.background == colors.surface) 0.dp else 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(if (small) 14.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (small) 10.dp else 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Catat transaksi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Pilih jenis transaksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Tutup",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    QuickTransactionAction(
                        icon = Icons.Outlined.TrendingUp,
                        title = "Catat pemasukan",
                        subtitle = "Tambahkan uang yang masuk",
                        highlighted = true,
                        onClick = onIncome,
                    )
                    QuickTransactionAction(
                        icon = Icons.Outlined.TrendingDown,
                        title = "Catat pengeluaran",
                        subtitle = "Tambahkan uang yang keluar",
                        highlighted = false,
                        onClick = onExpense,
                    )
                }
            }

            if (!wide) {
                Box(
                    modifier = Modifier
                        .offset(y = (-7).dp)
                        .size(15.dp)
                        .rotate(45f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surfaceContainerHigh),
                )
            }
        }
    }
}

@Composable
private fun QuickTransactionAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val small = isSmallPhoneUi()
    val container = if (highlighted) colors.primaryContainer else colors.surface
    val contentColor = if (highlighted) colors.onPrimaryContainer else colors.onSurface
    val outline = if (highlighted) colors.primary.copy(alpha = .72f) else colors.outlineVariant

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (small) 14.dp else 16.dp),
        color = container,
        contentColor = contentColor,
        border = BorderStroke(1.dp, outline),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (small) 12.dp else 14.dp,
                vertical = if (small) 10.dp else 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(if (small) 38.dp else 42.dp),
                shape = CircleShape,
                color = if (highlighted) colors.primary.copy(alpha = .14f) else colors.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (small) 20.dp else 22.dp),
                        tint = if (highlighted) colors.primary else colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(if (small) 10.dp else 12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlighted) colors.onPrimaryContainer.copy(alpha = .76f) else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (highlighted) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SadarTopBar(userName: String, profilePhoto: String?, darkTheme: Boolean, compact: Boolean, onToggleTheme: () -> Unit, onProfile: () -> Unit) {
    val design = sbDesignSystem()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(if (compact) design.navHeight - 6.dp else design.navHeight).padding(horizontal = if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(design.radius.control),
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SadarBudgetLogo(contentDescription = null, modifier = Modifier.size(if (compact) 24.dp else 26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sadar", fontWeight = FontWeight.Bold, fontSize = if (compact) 15.sp else 16.sp)
                    Text("Budget", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = if (compact) 15.sp else 16.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleTheme, modifier = Modifier.size(if (compact) 40.dp else 46.dp)) {
                Icon(if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Ganti tema", modifier = Modifier.size(if (compact) 22.dp else 24.dp))
            }
            IconButton(onClick = onProfile, modifier = Modifier.size(if (compact) 40.dp else 46.dp)) {
                if (!profilePhoto.isNullOrBlank()) {
                    AsyncImage(
                        model = profilePhoto,
                        contentDescription = userName.ifBlank { "Profil" },
                        modifier = Modifier.size(if (compact) 30.dp else 34.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Outlined.AccountCircle, userName.ifBlank { "Profil" }, modifier = Modifier.size(if (compact) 26.dp else 28.dp))
                }
            }
        }
    }
}

@Composable
private fun CompactNavigationRail(
    screen: AppScreen,
    onDashboard: () -> Unit,
    onTransactions: () -> Unit,
    onCreate: () -> Unit,
    onAnnual: () -> Unit,
    onAccount: () -> Unit,
) {
    val design = sbDesignSystem()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        Column(
            Modifier.fillMaxHeight().width(design.railWidth).padding(vertical = design.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            RailItem(Icons.Outlined.SpaceDashboard, "Ringkasan", screen is AppScreen.Dashboard, onDashboard)
            RailItem(Icons.Outlined.ReceiptLong, "Riwayat", screen is AppScreen.Transactions, onTransactions)
            RailItem(Icons.Outlined.AddCircle, "Catat", screen is AppScreen.AddTransaction, onCreate)
            RailItem(Icons.Outlined.BarChart, "Laporan", screen is AppScreen.Annual, onAnnual)
            RailItem(Icons.Outlined.AccountCircle, "Akun", screen is AppScreen.Categories || screen is AppScreen.Profile || screen is AppScreen.DataBackup || screen is AppScreen.Security || screen is AppScreen.About, onAccount)
        }
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val design = sbDesignSystem()
    Surface(
        onClick = onClick,
        modifier = Modifier.width(58.dp).height(52.dp),
        shape = RoundedCornerShape(design.radius.control),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, label, modifier = Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            if (selected) Text(label, fontSize = 8.sp, lineHeight = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
        }
    }
}

@Composable
private fun CompactBottomBar(
    screen: AppScreen,
    onDashboard: () -> Unit,
    onTransactions: () -> Unit,
    onCreate: () -> Unit,
    onAnnual: () -> Unit,
    onAccount: () -> Unit,
    small: Boolean = false,
) {
    val design = sbDesignSystem()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        Column {
            Row(Modifier.fillMaxWidth().height(if (small) design.navHeight - 4.dp else design.navHeight)) {
                CompactBottomItem(Icons.Outlined.SpaceDashboard, "Ringkasan", screen is AppScreen.Dashboard, onDashboard, small)
                CompactBottomItem(Icons.Outlined.ReceiptLong, "Riwayat", screen is AppScreen.Transactions, onTransactions, small)
                CompactBottomItem(Icons.Outlined.AddCircle, "Catat", screen is AppScreen.AddTransaction, onCreate, small)
                CompactBottomItem(Icons.Outlined.BarChart, "Laporan", screen is AppScreen.Annual, onAnnual, small)
                CompactBottomItem(Icons.Outlined.AccountCircle, "Akun", screen is AppScreen.Categories || screen is AppScreen.Profile || screen is AppScreen.DataBackup || screen is AppScreen.Security || screen is AppScreen.About, onAccount, small)
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun RowScope.CompactBottomItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, small: Boolean = false) {
    val design = sbDesignSystem()
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier.height(if (small) 26.dp else 28.dp).width(if (small) 44.dp else 48.dp).clip(RoundedCornerShape(design.radius.pill)),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(design.radius.pill), color = MaterialTheme.colorScheme.secondaryContainer, border = CardDefaults.outlinedCardBorder()) {}
                Icon(icon, label, modifier = Modifier.size(21.dp), tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(label, fontSize = if (small) 8.sp else 9.sp, lineHeight = if (small) 9.sp else 10.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun AccountMenuItems(
    onProfile: () -> Unit,
    onCategories: () -> Unit,
    onBackup: () -> Unit,
    onSecurity: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit,
    compact: Boolean = false,
) {
    AccountAction(Icons.Outlined.Person, "Profil", "Nama, email, foto, dan kata sandi", onProfile, compact)
    AccountAction(Icons.Outlined.Category, "Kelola kategori", "Atur kategori pemasukan dan pengeluaran", onCategories, compact)
    AccountAction(Icons.Outlined.Storage, "Data & backup", "Backup lokal, ekspor, impor, dan pemulihan", onBackup, compact)
    AccountAction(Icons.Outlined.Security, "Keamanan", "PIN, biometrik, dan privasi layar", onSecurity, compact)
    AccountAction(Icons.Outlined.Info, "Tentang", "Versi, pembuat, dan informasi aplikasi", onAbout, compact)
    HorizontalDivider()
    AccountAction(Icons.Outlined.Logout, "Keluar dari akun", "Akhiri sesi pada perangkat ini", onLogout, compact)
}

@Composable
private fun AccountAction(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, compact: Boolean = false) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 44.dp else 50.dp),
        contentPadding = PaddingValues(horizontal = if (compact) 4.dp else 8.dp, vertical = if (compact) 3.dp else 6.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(if (compact) 20.dp else 24.dp))
        Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = if (compact) 13.sp else 14.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (compact) 10.sp else 12.sp, maxLines = 2)
        }
        Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(if (compact) 18.dp else 22.dp))
    }
}
