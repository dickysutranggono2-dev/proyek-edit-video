package com.dik.videoedit.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/** Cadangan untuk HP tanpa pemilih foto bawaan: ACTION_GET_CONTENT menampilkan aplikasi Galeri di daftar pilihan. */
private class GetMediaContents : ActivityResultContract<Unit, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "image/*"))
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (intent == null || resultCode != android.app.Activity.RESULT_OK) return emptyList()
        val out = LinkedHashSet<Uri>()
        intent.data?.let { out += it }
        intent.clipData?.let { c -> for (i in 0 until c.itemCount) c.getItemAt(i).uri?.let { out += it } }
        return out.toList()
    }
}

/**
 * Pemilih video dan foto dari galeri. Memakai pemilih foto bawaan Android (langsung menampilkan galeri,
 * ada tombol untuk menjelajah file); bila tidak tersedia, memakai ACTION_GET_CONTENT. Mengembalikan fungsi
 * pembuka pemilih.
 */
@Composable
fun rememberMediaPicker(onPicked: (List<Uri>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onPicked)
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { callback.value(it) }
    val content = rememberLauncherForActivityResult(GetMediaContents()) { callback.value(it) }
    return {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        } else {
            content.launch(Unit)
        }
    }
}
