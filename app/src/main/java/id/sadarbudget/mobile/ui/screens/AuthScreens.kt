package id.sadarbudget.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sadarbudget.mobile.AppState
import id.sadarbudget.mobile.ui.components.*
import id.sadarbudget.mobile.ui.theme.sbDesignSystem
import kotlinx.coroutines.launch

private enum class AuthPage { Welcome, Login, Register }

@Composable
fun AuthFlow(appState: AppState) {
    var page by remember { mutableStateOf(AuthPage.Welcome) }
    when (page) {
        AuthPage.Welcome -> WelcomeScreen(
            darkTheme = appState.darkTheme,
            onThemeChange = appState::updateDarkTheme,
            onLogin = { page = AuthPage.Login },
            onRegister = { page = AuthPage.Register },
        )
        AuthPage.Login -> LoginScreen(appState, onBack = { page = AuthPage.Welcome })
        AuthPage.Register -> RegisterScreen(appState, onBack = { page = AuthPage.Welcome })
    }
}

@Composable
private fun WelcomeScreen(
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val small = isSmallPhoneUi()
    val wide = isWideUi()
    val design = sbDesignSystem()
    val pagePadding = design.spacing.pageHorizontal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding, vertical = if (small) 14.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Surface(
                    shape = RoundedCornerShape(design.radius.pill),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = .55f)),
                ) {
                    Row(
                        modifier = Modifier.padding(design.spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(design.spacing.xs),
                    ) {
                        ThemeChoicePill(!darkTheme, Icons.Outlined.LightMode, "Terang") { onThemeChange(false) }
                        ThemeChoicePill(darkTheme, Icons.Outlined.DarkMode, "Gelap") { onThemeChange(true) }
                    }
                }
            }

            Spacer(Modifier.height(if (small) 18.dp else 26.dp))

            Surface(
                modifier = Modifier.size(if (small) 104.dp else if (wide) 132.dp else 122.dp),
                shape = CircleShape,
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outline.copy(alpha = .50f)),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    SadarBudgetLogo(
                        contentDescription = "Logo SadarBudget",
                        modifier = Modifier.size(if (small) 68.dp else 82.dp),
                    )
                }
            }

            Spacer(Modifier.height(if (small) 14.dp else 20.dp))

            Text(
                "SadarBudget",
                style = if (small) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Catat, pantau, dan evaluasi keuangan pribadi dalam satu aplikasi.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(Modifier.height(if (small) 16.dp else 22.dp))

            SbCard {
                WelcomeBenefit(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "Catat transaksi",
                    subtitle = "Simpan pemasukan dan pengeluaran dalam satu riwayat.",
                )
                HorizontalDivider(color = colors.outline.copy(alpha = .35f))
                WelcomeBenefit(
                    icon = Icons.Outlined.BarChart,
                    title = "Laporan keuangan",
                    subtitle = "Lihat ringkasan, tren, serta laporan bulanan dan tahunan.",
                )
                HorizontalDivider(color = colors.outline.copy(alpha = .35f))
                WelcomeBenefit(
                    icon = Icons.Outlined.Security,
                    title = "Data di perangkat",
                    subtitle = "Backup dan ekspor tersedia saat Anda membutuhkannya.",
                )
            }

            Spacer(Modifier.height(if (small) 16.dp else 22.dp))

            SbPrimaryButton(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Masuk", fontSize = if (small) 15.sp else 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            SbSecondaryButton(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Buat akun", fontSize = if (small) 15.sp else 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(if (small) 12.dp else 18.dp))
        }
    }
}

@Composable
private fun ThemeChoicePill(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    SbFilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
    )
}

@Composable
private fun WelcomeBenefit(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    val small = isSmallPhoneUi()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (small) 10.dp else 14.dp),
    ) {
        Surface(
            modifier = Modifier.size(if (small) 38.dp else 44.dp),
            shape = RoundedCornerShape(if (small) 12.dp else 14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(if (small) 19.dp else 22.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoginScreen(appState: AppState, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AuthFormShell("Masuk", "Masukkan email dan kata sandi akun Anda.", onBack) {
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, placeholder = { Text("nama@gmail.com") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(password, { password = it }, label = { Text("Kata sandi") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        if (error != null) MessageBanner(error!!, true)
        SbPrimaryButton(
            onClick = {
                loading = true; error = null
                scope.launch {
                    runCatching { appState.login(email, password) }
                        .onFailure { error = it.message ?: "Login gagal." }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth(), enabled = email.isNotBlank() && password.isNotBlank() && !loading,
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp)); Text(if (loading) "Memproses…" else "Masuk")
        }
    }
}

@Composable
private fun RegisterScreen(appState: AppState, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AuthFormShell("Buat akun", "Isi data dasar berikut untuk mulai mencatat keuangan.", onBack) {
        OutlinedTextField(name, { name = it }, label = { Text("Nama") }, supportingText = { Text("Minimal 2 karakter.") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, placeholder = { Text("nama@gmail.com") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(password, { password = it }, label = { Text("Kata sandi") }, supportingText = { Text("Minimal 8 karakter.") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        OutlinedTextField(confirm, { confirm = it }, label = { Text("Konfirmasi kata sandi") }, supportingText = { Text(if (confirm.isNotEmpty() && confirm != password) "Belum sama dengan kata sandi di atas." else "Ketik ulang kata sandi yang sama.") }, isError = confirm.isNotEmpty() && confirm != password, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        if (error != null) MessageBanner(error!!, true)
        SbPrimaryButton(
            onClick = {
                loading = true; error = null
                scope.launch {
                    runCatching { appState.register(name, email, password, confirm) }
                        .onFailure { error = it.message ?: "Pendaftaran gagal." }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth(), enabled = name.trim().length >= 2 && email.isNotBlank() && password.length >= 8 && confirm == password && !loading,
        ) { Text(if (loading) "Membuat akun…" else "Buat akun") }
    }
}

@Composable
private fun AuthFormShell(title: String, subtitle: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val small = isSmallPhoneUi()
    Column(
        Modifier.fillMaxSize().padding(if (small) 14.dp else 20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(if (small) 40.dp else 48.dp)) { Icon(Icons.Outlined.ArrowBack, "Kembali") }
        Spacer(Modifier.height(if (small) 4.dp else 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SadarBudgetLogo(contentDescription = null, modifier = Modifier.size(if (small) 42.dp else 52.dp))
            Spacer(Modifier.width(if (small) 9.dp else 12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(if (small) 14.dp else 22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(if (small) 8.dp else 12.dp), content = content)
    }
}
