package com.tohn95.internetradio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.unit.IntRect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Хранит только локальные обложки папок; исходные фотографии пользователя не изменяет. */
class FolderCoverStore @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun save(folderId: Long, source: Uri, crop: IntRect): String = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IllegalArgumentException("Cannot read image")
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= crop.width && bounds.outHeight / (sample * 2) >= crop.height) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("Cannot decode image")
        val left = (crop.left / sample).coerceIn(0, decoded.width - 1)
        val top = (crop.top / sample).coerceIn(0, decoded.height - 1)
        val width = (crop.width / sample).coerceAtLeast(1).coerceAtMost(decoded.width - left)
        val height = (crop.height / sample).coerceAtLeast(1).coerceAtMost(decoded.height - top)
        val square = minOf(width, height)
        val cut = Bitmap.createBitmap(decoded, left, top, square, square)
        val output = Bitmap.createScaledBitmap(cut, 512, 512, true)
        val directory = File(context.filesDir, "covers").apply { mkdirs() }
        val file = File(directory, "folder-$folderId-${System.currentTimeMillis()}.jpg")
        file.outputStream().buffered().use { output.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (output !== cut) output.recycle()
        if (cut !== decoded) cut.recycle()
        decoded.recycle()
        file.absolutePath
    }

    fun delete(path: String?) {
        if (!path.isNullOrBlank()) File(path).delete()
    }
}
