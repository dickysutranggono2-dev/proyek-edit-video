package com.dik.videoedit.videoprocessing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Berkas model Whisper tiny (ONNX, terkuantisasi) yang diunduh sekali ke penyimpanan aplikasi. */
object WhisperModel {
    private const val BASE = "https://huggingface.co/Xenova/whisper-tiny/resolve/main/"

    class FileSpec(val remote: String, val local: String, val minBytes: Long, val approxBytes: Long)

    val files = listOf(
        FileSpec("onnx/encoder_model_quantized.onnx", "encoder.onnx", 8_000_000L, 10_100_000L),
        FileSpec("onnx/decoder_model_merged_quantized.onnx", "decoder.onnx", 25_000_000L, 30_700_000L),
        FileSpec("tokenizer.json", "tokenizer.json", 1_000_000L, 2_480_000L),
    )

    const val TOTAL_MB = 43

    fun dir(context: Context) = File(context.filesDir, "whisper")
    fun file(context: Context, local: String) = File(dir(context), local)

    fun isReady(context: Context): Boolean =
        files.all { f -> file(context, f.local).let { it.exists() && it.length() >= f.minBytes } }

    fun delete(context: Context) {
        dir(context).deleteRecursively()
    }

    /** Mengunduh semua berkas yang belum ada. [onProgress] 0..1. */
    suspend fun download(context: Context, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val total = files.sumOf { it.approxBytes }.toFloat()
        var done = 0L
        dir(context).mkdirs()
        for (spec in files) {
            val target = file(context, spec.local)
            if (target.exists() && target.length() >= spec.minBytes) {
                done += spec.approxBytes
                onProgress((done / total).coerceAtMost(0.99f))
                continue
            }
            val part = File(target.path + ".part")
            part.delete()
            var conn: HttpURLConnection? = null
            try {
                conn = URL(BASE + spec.remote).openConnection() as HttpURLConnection
                conn.connectTimeout = 20_000
                conn.readTimeout = 60_000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "VideoEdit")
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw AutoCaption.CaptionException("Gagal mengunduh model (kode $code).")
                }
                var read = 0L
                conn.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            read += n
                            onProgress(((done + read) / total).coerceAtMost(0.99f))
                        }
                    }
                }
                if (read < spec.minBytes) {
                    part.delete()
                    throw AutoCaption.CaptionException("Unduhan model tidak lengkap. Coba lagi.")
                }
                target.delete()
                if (!part.renameTo(target)) throw AutoCaption.CaptionException("Gagal menyimpan model.")
                done += spec.approxBytes
            } catch (e: IOException) {
                part.delete()
                throw AutoCaption.CaptionException("Gagal mengunduh model. Periksa koneksi internet.")
            } finally {
                conn?.disconnect()
            }
        }
        onProgress(1f)
    }
}
