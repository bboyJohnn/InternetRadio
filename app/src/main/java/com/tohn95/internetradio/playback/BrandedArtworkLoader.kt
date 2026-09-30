package com.tohn95.internetradio.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.tohn95.internetradio.R
import com.tohn95.internetradio.util.accentFrom
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Обложка для уведомления/экрана блокировки/системного плеера в стиле приложения (как «альбом» у Spotify):
 * градиент в цвет логотипа со светлыми бликами + логотип станции на белой плитке. Нет логотипа —
 * наш глобус в наушниках. Android 13+ растягивает её на фон всей карточки плеера.
 * Картинка не зависит от трека — кэшируем по станции, чтобы не перерисовывать на каждую смену песни.
 */
@OptIn(UnstableApi::class)
class BrandedArtworkLoader(private val context: Context) : BitmapLoader {
    private val delegate = DataSourceBitmapLoader(context)
    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    private var cacheKey: String? = null
    private var cached: ListenableFuture<Bitmap>? = null

    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = delegate.decodeBitmap(data)
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = delegate.loadBitmap(uri)

    @Synchronized
    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap> {
        val uri = metadata.artworkUri
        val key = uri?.toString() ?: ("none:" + (metadata.station ?: ""))
        if (key == cacheKey) cached?.let { return it }
        val future = executor.submit(Callable { compose(uri?.let(::loadLogo)) })
        cacheKey = key
        cached = future
        return future
    }

    /** Логотип через Coil: те же форматы и дисковый кэш, что у интерфейса. */
    private fun loadLogo(uri: Uri): Bitmap? = runCatching {
        runBlocking {
            withTimeoutOrNull(8_000) {
                val request = ImageRequest.Builder(context).data(uri.toString()).allowHardware(false).size(384).build()
                (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            }
        }
    }.getOrNull()

    private fun compose(logo: Bitmap?): Bitmap {
        val size = 512
        val s = size.toFloat()
        val out = createBitmap(size, size)
        val c = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Фон: градиент в цвет логотипа (или фирменный синий), как подложка плеера в приложении.
        val base = logo?.let { accentFrom(it) } ?: BRAND_BLUE
        val deep = ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.6f)
        paint.shader = LinearGradient(0f, 0f, s, s, base, deep, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, s, s, paint)
        // Мягкие светлые блики — «переливы», как у фоновых пятен в приложении.
        paint.shader = RadialGradient(s * 0.82f, s * 0.12f, s * 0.6f, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawCircle(s * 0.82f, s * 0.12f, s * 0.6f, paint)
        paint.shader = RadialGradient(s * 0.1f, s * 0.95f, s * 0.55f, 0x26FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawCircle(s * 0.1f, s * 0.95f, s * 0.55f, paint)
        paint.shader = null

        if (logo != null) {
            // Плитка ~38% — Android 13+ растягивает обложку на широкую карточку и режет верх/низ:
            // так логотип целиком в видимой полосе, а вокруг остаётся цветной градиент.
            val tile = RectF(s * 0.31f, s * 0.31f, s * 0.69f, s * 0.69f)
            val radius = s * 0.07f
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                setShadowLayer(s * 0.05f, 0f, s * 0.02f, 0x66000000)
            }
            c.drawRoundRect(tile, radius, radius, shadow)
            // Логотип вписываем в плитку с полями, углы плитки обрезают его края.
            val pad = s * 0.03f
            val box = RectF(tile.left + pad, tile.top + pad, tile.right - pad, tile.bottom - pad)
            val scale = minOf(box.width() / logo.width, box.height() / logo.height)
            val w = logo.width * scale
            val h = logo.height * scale
            val dst = RectF(box.centerX() - w / 2, box.centerY() - h / 2, box.centerX() + w / 2, box.centerY() + h / 2)
            c.save()
            c.clipPath(Path().apply { addRoundRect(tile, radius, radius, Path.Direction.CW) })
            c.drawBitmap(logo, Rect(0, 0, logo.width, logo.height), dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            c.restore()
        } else {
            // Без логотипа — фирменный знак (передний слой иконки: глобус в наушниках).
            ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.apply {
                setBounds(0, 0, size, size)
                draw(c)
            }
        }
        return out
    }

    private companion object {
        const val BRAND_BLUE = 0xFF2F6FD0.toInt()
    }
}
