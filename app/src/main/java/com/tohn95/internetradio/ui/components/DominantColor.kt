package com.tohn95.internetradio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.tohn95.internetradio.util.accentFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Акцентный цвет из картинки (логотип станции по ссылке или фото жанра из ресурсов) (для фона и акцентов плеера, как у Spotify).
 * null — логотипа нет, он не загрузился или он почти бесцветный (белый/серый/чёрный):
 * тогда вызывающий берёт цвет темы.
 */
@Composable
fun rememberDominantColor(data: Any?): Color? {
    val context = LocalContext.current
    var color by remember(data) { mutableStateOf<Color?>(null) }
    LaunchedEffect(data) {
        if (data == null || (data is String && data.isBlank())) return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(data).allowHardware(false).size(128).build()
        val bitmap = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            ?: return@LaunchedEffect
        color = withContext(Dispatchers.Default) { accentFrom(bitmap)?.let { Color(it) } }
    }
    return color
}

