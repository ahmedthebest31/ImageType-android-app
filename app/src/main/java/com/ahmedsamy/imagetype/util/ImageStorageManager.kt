package com.ahmedsamy.imagetype.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageStorageManager(private val context: Context) {
    private val TAG = "ImageStorageManager"

    fun saveToGallery(
        bitmap: Bitmap,
        imageQuality: String,
        bgType: String,
        scope: CoroutineScope,
        onFinished: (Boolean) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            var success = false
            try {
                val quality = ImageGenerator.getExportQualityInt(imageQuality)
                val format = if (bgType == "Transparent") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val extension = if (bgType == "Transparent") "png" else "jpg"
                val mimeType = if (bgType == "Transparent") "image/png" else "image/jpeg"
                val filename = "ImageType_${System.currentTimeMillis()}.$extension"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ImageType")
                    }
                    val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (imageUri != null) {
                        resolver.openOutputStream(imageUri).use { outStream ->
                            if (outStream != null) {
                                success = bitmap.compress(format, quality, outStream)
                            }
                        }
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).resolve("ImageType")
                    if (!imagesDir.exists()) imagesDir.mkdirs()
                    val file = File(imagesDir, filename)
                    FileOutputStream(file).use { outStream ->
                        success = bitmap.compress(format, quality, outStream)
                    }
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(file)
                    context.sendBroadcast(mediaScanIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Save to Gallery Error", e)
            }
            withContext(Dispatchers.Main) { onFinished(success) }
        }
    }

    fun shareImage(
        bitmap: Bitmap,
        imageQuality: String,
        bgType: String,
        scope: CoroutineScope,
        onFinished: (Boolean) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val quality = ImageGenerator.getExportQualityInt(imageQuality)
                val format = if (bgType == "Transparent") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val extension = if (bgType == "Transparent") "png" else "jpg"

                val cachePath = File(context.cacheDir, "shared_images")
                cachePath.mkdirs()
                val file = File(cachePath, "ImageType_share.$extension")
                val stream = FileOutputStream(file)
                bitmap.compress(format, quality, stream)
                stream.close()

                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                if (contentUri != null) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        type = "image/*"
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share generated text image via:")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    withContext(Dispatchers.Main) { onFinished(true) }
                } else {
                    withContext(Dispatchers.Main) { onFinished(false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sharing image error details", e)
                withContext(Dispatchers.Main) { onFinished(false) }
            }
        }
    }
}
