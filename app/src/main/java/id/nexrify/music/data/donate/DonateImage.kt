package id.nexrify.music.data.donate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Memperkecil foto bukti transfer menjadi JPEG kecil (base64) supaya muat dikirim ke server dan Telegram. */
object DonateImage {

    private const val MAX_SIDE = 1600
    private const val MAX_BYTES = 1_500_000

    fun toJpegBase64(context: Context, uri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val longest = max(decoded.width, decoded.height)
            val bitmap = if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
            } else decoded
            var quality = 85
            var bytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 10
            } while (bytes.size > MAX_BYTES && quality >= 35)
            if (bytes.size < 2000) return null
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }
}
