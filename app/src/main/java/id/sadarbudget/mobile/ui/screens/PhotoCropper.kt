package id.sadarbudget.mobile.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import id.sadarbudget.mobile.ui.components.SbPrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    } else {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Gambar tidak dapat dibuka." }
            requireNotNull(BitmapFactory.decodeStream(input)) { "Format gambar tidak dapat dibaca." }
        }
    }
}

@Composable
fun PhotoCropDialog(bitmap: Bitmap, onDismiss: () -> Unit, onApply: (Bitmap, Float, Offset, IntSize) -> Unit) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tentukan area foto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Geser dan cubit untuk mengatur foto di dalam lingkaran. Hasil disimpan tajam dalam ukuran 512 × 512 piksel.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(Color.Black)) {
                    Canvas(
                        Modifier.fillMaxSize()
                            .onSizeChanged { viewport = it }
                            .pointerInput(bitmap, viewport) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    if (viewport.width <= 0) return@detectTransformGestures
                                    val newZoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                    val base = max(viewport.width.toFloat() / bitmap.width, viewport.height.toFloat() / bitmap.height)
                                    val actual = base * newZoom
                                    val maxX = max(0f, (bitmap.width * actual - viewport.width) / 2f)
                                    val maxY = max(0f, (bitmap.height * actual - viewport.height) / 2f)
                                    zoom = newZoom
                                    offset = Offset(
                                        (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        (offset.y + pan.y).coerceIn(-maxY, maxY),
                                    )
                                }
                            }
                    ) {
                        val base = max(size.width / bitmap.width, size.height / bitmap.height)
                        val actual = base * zoom
                        withTransform({
                            translate(size.width / 2f + offset.x, size.height / 2f + offset.y)
                            scale(actual, actual, pivot = Offset.Zero)
                            translate(-bitmap.width / 2f, -bitmap.height / 2f)
                        }) {
                            drawImage(image)
                        }
                        drawCircle(
                            color = Color.White.copy(alpha = .9f),
                            radius = size.minDimension / 2f - 2f,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                        )
                    }
                }
                Text("Zoom ${String.format(java.util.Locale.US, "%.1f×", zoom)}", style = MaterialTheme.typography.labelMedium)
                Slider(value = zoom, onValueChange = { value ->
                    zoom = value
                    if (viewport.width > 0) {
                        val base = max(viewport.width.toFloat() / bitmap.width, viewport.height.toFloat() / bitmap.height)
                        val actual = base * zoom
                        val maxX = max(0f, (bitmap.width * actual - viewport.width) / 2f)
                        val maxY = max(0f, (bitmap.height * actual - viewport.height) / 2f)
                        offset = Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
                    }
                }, valueRange = 1f..5f)
            }
        },
        confirmButton = { SbPrimaryButton(onClick = { onApply(bitmap, zoom, offset, viewport) }, enabled = viewport.width > 0) { Text("Gunakan foto") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

suspend fun createCroppedPhotoFile(context: Context, source: Bitmap, zoom: Float, offset: Offset, viewport: IntSize): File = withContext(Dispatchers.Default) {
    require(viewport.width > 0 && viewport.height > 0)
    val base = max(viewport.width.toFloat() / source.width, viewport.height.toFloat() / source.height)
    val actual = base * zoom
    val sourceCropSize = (viewport.width / actual).coerceAtMost(minOf(source.width, source.height).toFloat())
    val sourceCenterX = source.width / 2f - offset.x / actual
    val sourceCenterY = source.height / 2f - offset.y / actual
    var left = sourceCenterX - sourceCropSize / 2f
    var top = sourceCenterY - sourceCropSize / 2f
    left = left.coerceIn(0f, source.width - sourceCropSize)
    top = top.coerceIn(0f, source.height - sourceCropSize)
    val leftInt = left.toInt().coerceIn(0, source.width - 1)
    val topInt = top.toInt().coerceIn(0, source.height - 1)
    val safeSize = minOf(
        sourceCropSize.toInt().coerceAtLeast(1),
        source.width - leftInt,
        source.height - topInt,
    ).coerceAtLeast(1)
    val crop = Bitmap.createBitmap(source, leftInt, topInt, safeSize, safeSize)
    val scaled = Bitmap.createScaledBitmap(crop, 512, 512, true)
    val file = File(context.cacheDir, "profile-crop-${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { output -> scaled.compress(Bitmap.CompressFormat.JPEG, 90, output) }
    if (crop !== source && !crop.isRecycled) crop.recycle()
    if (scaled !== crop && !scaled.isRecycled) scaled.recycle()
    file
}
