package com.tohn95.internetradio.ui.screens.favorites

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.tohn95.internetradio.R
import kotlin.math.min
import kotlin.math.roundToInt

fun clampOffset(imageW: Int, imageH: Int, frame: Float, scale: Float, offsetX: Float, offsetY: Float): Offset {
    if (imageW <= 0 || imageH <= 0 || frame <= 0f) return Offset.Zero
    val factor = frame / min(imageW, imageH) * scale.coerceAtLeast(1f)
    val maxX = (imageW * factor - frame).coerceAtLeast(0f) / 2f
    val maxY = (imageH * factor - frame).coerceAtLeast(0f) / 2f
    return Offset(offsetX.coerceIn(-maxX, maxX), offsetY.coerceIn(-maxY, maxY))
}

/** Преобразует видимую квадратную рамку в квадрат пикселей исходного изображения. */
fun coverCrop(imageW: Int, imageH: Int, frame: Float, scale: Float, offsetX: Float, offsetY: Float): IntRect {
    if (imageW <= 0 || imageH <= 0 || frame <= 0f) return IntRect.Zero
    val safeScale = scale.coerceAtLeast(1f)
    val offset = clampOffset(imageW, imageH, frame, safeScale, offsetX, offsetY)
    val factor = frame / min(imageW, imageH) * safeScale
    val side = (frame / factor).roundToInt().coerceIn(1, min(imageW, imageH))
    val centerX = imageW / 2f - offset.x / factor
    val centerY = imageH / 2f - offset.y / factor
    val left = (centerX - side / 2f).roundToInt().coerceIn(0, imageW - side)
    val top = (centerY - side / 2f).roundToInt().coerceIn(0, imageH - side)
    return IntRect(left, top, left + side, top + side)
}

@Composable
fun rememberCoverPicker(onPicked: (Uri, IntRect) -> Unit): () -> Unit {
    var source by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { source = it }
    source?.let { uri ->
        CoverCropDialog(uri = uri, onDismiss = { source = null }, onDone = { crop ->
            onPicked(uri, crop)
            source = null
        })
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
private fun CoverCropDialog(uri: Uri, onDismiss: () -> Unit, onDone: (IntRect) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val frame = minOf(maxWidth, maxHeight) * 0.82f
            val framePx = with(LocalDensity.current) { frame.toPx() }
            var imageW by remember(uri) { mutableStateOf(0) }
            var imageH by remember(uri) { mutableStateOf(0) }
            var scale by remember(uri) { mutableStateOf(1f) }
            var offset by remember(uri) { mutableStateOf(Offset.Zero) }
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel), color = Color.White) }
                    TextButton(
                        onClick = { onDone(coverCrop(imageW, imageH, framePx, scale, offset.x, offset.y)) },
                        enabled = imageW > 0 && imageH > 0,
                    ) { Text(stringResource(R.string.cover_crop_done), color = Color.White) }
                }
                Text(stringResource(R.string.cover_crop_title), color = Color.White)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(frame).pointerInput(uri, imageW, imageH) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val next = (scale * zoom).coerceIn(1f, 5f)
                                scale = next
                                offset = clampOffset(imageW, imageH, framePx, next, offset.x + pan.x, offset.y + pan.y)
                            }
                        },
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = stringResource(R.string.cover_crop_title),
                            contentScale = ContentScale.Crop,
                            onSuccess = {
                                imageW = it.result.image.width
                                imageH = it.result.image.height
                            },
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        )
                    }
                }
                Text(
                    stringResource(R.string.cover_crop_hint), color = Color.White,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}
