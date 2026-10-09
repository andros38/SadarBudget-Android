package id.sadarbudget.mobile.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.sadarbudget.mobile.data.AppRepository
import id.sadarbudget.mobile.data.User
import id.sadarbudget.mobile.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(repository: AppRepository, initialUser: User?, onUserChanged: (User) -> Unit, onNotify: (String) -> Unit = {}) {
    var user by remember { mutableStateOf(initialUser) }
    var name by remember { mutableStateOf(initialUser?.name.orEmpty()) }
    var email by remember { mutableStateOf(initialUser?.email.orEmpty()) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { repository.me() }.onSuccess { user = it; name = it.name; email = it.email; onUserChanged(it) }
            .onFailure { error = it.message }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true; error = null
                runCatching { loadBitmapFromUri(context, uri) }
                    .onSuccess { cropBitmap = it }
                    .onFailure { error = "Gambar tidak dapat dibuka: ${it.message}" }
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = pageHorizontalPadding(), vertical = pageVerticalPadding()).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing()),
    ) {
        PageTitle("Akun dan keamanan", "Profil", "Kelola identitas akun, foto profil, dan kata sandi.")
        if (message != null) MessageBanner(message!!)
        if (error != null) MessageBanner(error!!, true)

        SbCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!user?.profilePhoto.isNullOrBlank()) {
                    AsyncImage(
                        model = user?.profilePhoto,
                        contentDescription = "Foto profil",
                        modifier = Modifier.size(if (isSmallPhoneUi()) 62.dp else if (isCompactUi()) 72.dp else 80.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Surface(modifier = Modifier.size(if (isSmallPhoneUi()) 62.dp else if (isCompactUi()) 72.dp else 80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text(user?.name?.take(1)?.uppercase() ?: "U", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.name ?: "Pengguna", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user?.createdAt?.let { "Bergabung ${it.take(10)}" } ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SbPrimaryButton(onClick = { pickPhoto.launch("image/*") }, enabled = !busy, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Ganti foto") }
                if (!user?.profilePhoto.isNullOrBlank()) SbSecondaryButton(onClick = {
                    scope.launch {
                        busy = true
                        runCatching { repository.removePhoto() }
                            .onSuccess { user = it.user; onUserChanged(it.user); message = "Foto profil dihapus."; onNotify("Foto profil berhasil dihapus.") }
                            .onFailure { error = it.message }
                        busy = false
                    }
                }, enabled = !busy) { Icon(Icons.Outlined.Delete, "Hapus foto") }
            }
        }

        SbCard {
            Text("Nama dan email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { if (it.length <= 100) name = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(email, { if (it.length <= 190) email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            SbPrimaryButton(onClick = {
                scope.launch {
                    busy = true; error = null; message = null
                    runCatching { repository.updateProfile(name, email) }
                        .onSuccess { user = it.user; onUserChanged(it.user); message = "Profil berhasil diperbarui."; onNotify("Profil berhasil diperbarui.") }
                        .onFailure { error = it.message }
                    busy = false
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !busy && name.trim().length >= 2 && email.isNotBlank()) { Text("Simpan profil") }
        }

        SbCard {
            Text("Kata sandi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Terakhir diganti: ${user?.passwordChangedAt?.takeIf { it.isNotBlank() }?.take(10) ?: "Belum pernah diganti"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(currentPassword, { currentPassword = it }, label = { Text("Kata sandi saat ini") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
            OutlinedTextField(newPassword, { newPassword = it }, label = { Text("Kata sandi baru") }, supportingText = { Text("Minimal 8 karakter") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
            OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("Konfirmasi kata sandi baru") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
            SbPrimaryButton(onClick = {
                scope.launch {
                    busy = true; error = null; message = null
                    runCatching { repository.updatePassword(currentPassword, newPassword, confirmPassword) }
                        .onSuccess { message = it; onNotify(it); currentPassword = ""; newPassword = ""; confirmPassword = ""; user = repository.me(); onUserChanged(user!!) }
                        .onFailure { error = it.message }
                    busy = false
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !busy && currentPassword.isNotBlank() && newPassword.length >= 8 && confirmPassword.isNotBlank()) { Text("Ganti kata sandi") }
        }
    }

    cropBitmap?.let { bitmap ->
        PhotoCropDialog(bitmap, onDismiss = { cropBitmap = null }) { source, zoom, offset, viewport ->
            cropBitmap = null
            scope.launch {
                busy = true; error = null; message = null
                runCatching {
                    val file = createCroppedPhotoFile(context, source, zoom, offset, viewport)
                    repository.uploadPhoto(file)
                }.onSuccess { result -> user = result.user; onUserChanged(result.user); message = "Foto profil 1:1 berhasil diperbarui."; onNotify("Foto profil berhasil diperbarui.") }
                    .onFailure { error = it.message ?: "Foto gagal diunggah." }
                busy = false
            }
        }
    }
}
