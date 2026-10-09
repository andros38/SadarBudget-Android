package id.sadarbudget.mobile.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.AppState
import id.sadarbudget.mobile.ui.components.*

/** BiometricPrompt platform API 28+; pada perangkat lain PIN selalu tersedia. */
fun biometricSupported(context: Context): Boolean = Build.VERSION.SDK_INT >= 28 &&
    (context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
        (Build.VERSION.SDK_INT >= 29 && context.packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)))

fun showBiometricPrompt(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    if (Build.VERSION.SDK_INT < 28) { onError("Biometrik memerlukan Android 9 atau lebih baru. Gunakan PIN."); return }
    val prompt = BiometricPrompt.Builder(activity)
        .setTitle("Buka SadarBudget")
        .setSubtitle("Konfirmasi identitas dengan biometrik perangkat")
        .setNegativeButton("Gunakan PIN", activity.mainExecutor) { _, _ -> }
        .build()
    prompt.authenticate(CancellationSignal(), activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) { onSuccess() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            // Platform BiometricPrompt tidak menyediakan BIOMETRIC_ERROR_NEGATIVE_BUTTON.
            // Tombol "Gunakan PIN" ditangani oleh setNegativeButton di atas.
            if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                onError(errString?.toString() ?: "Biometrik tidak tersedia. Gunakan PIN.")
            }
        }
    })
}

@Composable
fun AppLockScreen(appState: AppState) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    var recoveryPassword by remember { mutableStateOf("") }
    var recoveryBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        PageTitle("Keamanan", "SadarBudget terkunci", "Masukkan PIN untuk melanjutkan.")
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6); error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PIN 6 angka") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        if (error != null) { Spacer(Modifier.height(8.dp)); Text(error!!, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
        SbPrimaryButton(onClick = {
            val remaining = appState.remainingPinWaitMs()
            if (remaining > 0) error = "Terlalu banyak percobaan. Coba lagi dalam ${(remaining + 999)/1000} detik."
            else if (appState.unlockWithPin(pin)) { pin = ""; error = null }
            else {
                pin = ""
                val wait = appState.remainingPinWaitMs()
                error = if (wait > 0) "Terlalu banyak percobaan. Coba lagi dalam ${(wait + 999)/1000} detik." else "PIN salah."
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = pin.length == 6) { Icon(Icons.Outlined.Lock, null); Spacer(Modifier.width(8.dp)); Text("Buka aplikasi") }
        TextButton(onClick = { showRecovery = true }) { Text("Lupa PIN? Reset dengan kata sandi akun") }
        if (showRecovery) AlertDialog(
            onDismissRequest = { if (!recoveryBusy) showRecovery = false },
            title = { Text("Reset PIN aplikasi") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Masukkan kata sandi akun untuk menonaktifkan PIN. Data tidak dihapus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(recoveryPassword, { recoveryPassword = it }, label = { Text("Kata sandi akun") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
            } },
            confirmButton = { SbPrimaryButton(onClick = {
                scope.launch {
                    recoveryBusy = true
                    runCatching { appState.resetPinWithAccountPassword(recoveryPassword) }
                        .onSuccess { showRecovery = false; error = null; recoveryPassword = "" }
                        .onFailure { error = it.message ?: "Kata sandi salah."; recoveryPassword = ""; showRecovery = false }
                    recoveryBusy = false
                }
            }, enabled = recoveryPassword.isNotBlank() && !recoveryBusy) { Text("Reset PIN") } },
            dismissButton = { TextButton(onClick = { showRecovery = false }, enabled = !recoveryBusy) { Text("Batal") } },
        )
        if (appState.biometricEnabled && activity != null && biometricSupported(context)) {
            Spacer(Modifier.height(10.dp))
            SbSecondaryButton(onClick = {
                showBiometricPrompt(activity, onSuccess = { appState.unlock(); pin = "" }, onError = { error = it })
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Gunakan biometrik") }
        }
    }
}

@Composable
fun AppSecurityScreen(appState: AppState) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var pinToRemove by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity
    Column(
        Modifier.fillMaxSize().padding(horizontal = pageHorizontalPadding(), vertical = pageVerticalPadding()).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing()),
    ) {
        PageTitle("Data dan privasi", "Keamanan aplikasi", "PIN lokal, autentikasi biometrik, dan perlindungan tampilan.")
        if (message != null) MessageBanner(message!!)
        if (error != null) MessageBanner(error!!, true)
        SbCard {
            Text(if (appState.pinEnabled) "Ubah PIN aplikasi" else "Buat PIN aplikasi", style = MaterialTheme.typography.titleMedium)
            Text("PIN 6 digit diperlukan saat aplikasi dibuka kembali dari latar belakang. PIN bukan pengganti kata sandi akun.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (appState.pinEnabled) SecurityPinField(oldPin, { oldPin = it }, "PIN lama")
            SecurityPinField(newPin, { newPin = it }, "PIN baru (6 angka)")
            SbPrimaryButton(onClick = {
                runCatching { appState.updatePin(newPin, oldPin) }
                    .onSuccess { message = "PIN aplikasi tersimpan."; error = null; newPin = ""; oldPin = "" }
                    .onFailure { error = it.message; message = null }
            }, modifier = Modifier.fillMaxWidth(), enabled = newPin.length == 6 && (!appState.pinEnabled || oldPin.length == 6)) { Text("Simpan PIN") }
            if (appState.pinEnabled) {
                SecurityPinField(pinToRemove, { pinToRemove = it }, "PIN saat ini untuk menonaktifkan")
                SbSecondaryButton(onClick = {
                    runCatching { appState.removePin(pinToRemove) }
                        .onSuccess { message = "Kunci aplikasi dinonaktifkan."; error = null; pinToRemove = "" }
                        .onFailure { error = it.message; message = null }
                }, modifier = Modifier.fillMaxWidth(), enabled = pinToRemove.length == 6) { Text("Nonaktifkan PIN") }
            }
        }
        SbCard {
            Text("Biometrik", style = MaterialTheme.typography.titleMedium)
            Text("Sidik jari atau pengenalan wajah bila didukung perangkat. PIN tetap dapat digunakan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Buka dengan biometrik", modifier = Modifier.weight(1f))
                Switch(checked = appState.biometricEnabled, onCheckedChange = { enable ->
                    if (enable && !biometricSupported(context)) { error = "Perangkat belum mendukung biometrik yang tersedia untuk aplikasi."; return@Switch }
                    if (enable && activity != null) {
                        showBiometricPrompt(activity, onSuccess = { appState.setBiometric(true); error = null }, onError = { error = it })
                    } else appState.setBiometric(false)
                }, enabled = appState.pinEnabled)
            }
        }
        SbCard {
            Text("Privasi layar", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sembunyikan nominal di Ringkasan", modifier = Modifier.weight(1f))
                Switch(checked = appState.hideAmounts, onCheckedChange = appState::updateHideAmounts)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Blokir screenshot dan pratinjau aplikasi", modifier = Modifier.weight(1f))
                Switch(checked = appState.secureWindow, onCheckedChange = appState::updateSecureWindow)
            }
        }
    }
}

@Composable
private fun SecurityPinField(value: String, update: (String) -> Unit, label: String) {
    OutlinedTextField(value, { update(it.filter(Char::isDigit).take(6)) }, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
}
