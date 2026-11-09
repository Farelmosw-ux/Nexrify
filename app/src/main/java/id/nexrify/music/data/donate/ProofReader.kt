package id.nexrify.music.data.donate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** File bukti yang siap dikirim. Isinya bisa foto, video, PDF, atau file apa saja. */
class NexProofFile(val name: String, val mime: String, val bytes: ByteArray)

sealed class NexProofResult {
    class Ok(val file: NexProofFile) : NexProofResult()
    object TooBig : NexProofResult()
    object Unreadable : NexProofResult()
}

/**
 * Menyiapkan bukti transfer. Foto yang bisa dibaca diperkecil jadi JPEG, sisanya (video, PDF, GIF,
 * HEIC yang tidak bisa dibaca, dll) dikirim apa adanya selama tidak lebih dari 15 MB.
 */
object ProofReader {

    const val MAX_BYTES = 15 * 1024 * 1024
    /** Kelipatan 3 supaya potongan base64 tidak kena padding di tengah. */
    const val CHUNK = 384 * 1024
    private const val MAX_SIDE = 1600
    private const val JPEG_TARGET = 1_500_000

    class Info(val name: String, val mime: String, val size: Long)

    fun info(context: Context, uri: Uri): Info {
        var name = ""
        var size = -1L
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it).orEmpty() }
                    c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { size = c.getLong(it) }
                }
            }
        } catch (_: Throwable) {}
        if (name.isBlank()) name = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "bukti" }
        var mime = try { context.contentResolver.getType(uri).orEmpty() } catch (_: Throwable) { "" }
        if (mime.isBlank() || mime == "application/octet-stream") {
            val ext = name.substringAfterLast('.', "").lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let { mime = it }
        }
        if (mime.isBlank()) mime = "application/octet-stream"
        return Info(name, mime, size)
    }

    fun prepare(context: Context, uri: Uri): NexProofResult {
        return try {
            val info = info(context, uri)
            if (info.size > MAX_BYTES) return NexProofResult.TooBig
            if (info.mime.startsWith("image/") && info.mime != "image/gif") {
                compressImage(context, uri)?.let {
                    val base = info.name.substringBeforeLast('.', info.name).ifBlank { "bukti" }
                    return NexProofResult.Ok(NexProofFile("$base.jpg", "image/jpeg", it))
                }
            }
            val raw = readLimited(context, uri) ?: return NexProofResult.TooBig
            if (raw.isEmpty()) NexProofResult.Unreadable else NexProofResult.Ok(NexProofFile(info.name, info.mime, raw))
        } catch (_: OutOfMemoryError) {
            NexProofResult.TooBig
        } catch (_: Throwable) {
            NexProofResult.Unreadable
        }
    }

    /** Membaca isi file. Mengembalikan null kalau lebih besar dari batas. */
    private fun readLimited(context: Context, uri: Uri): ByteArray? {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > MAX_BYTES) return null
            }
            return out.toByteArray()
        }
        throw IllegalStateException("stream")
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray? {
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
            } while (bytes.size > JPEG_TARGET && quality >= 35)
            if (bytes.size < 2000) null else bytes
        } catch (_: Throwable) {
            null
        }
    }
}
