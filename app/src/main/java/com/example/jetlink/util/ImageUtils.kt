package com.example.jetlink.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    private const val MAX_IMAGE_SIZE = 1024
    private const val COMPRESSION_QUALITY = 80

    /**
     * 将给定的 Uri (可以是 content:// 或 file://) 指向的图片，
     * 复制到应用的内部缓存目录，并返回一个新的 file:// Uri。
     * 这是确保应用能够持续访问图片的关键步骤。
     */
    suspend fun copyImageToInternalStorage(context: Context, uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val imagesFolder = File(context.cacheDir, "images")
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs()
            }
            val file = File(imagesFolder, "IMG_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use {
                inputStream.copyTo(it)
            }
            inputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun compressImageToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height && width > MAX_IMAGE_SIZE) {
                MAX_IMAGE_SIZE.toFloat() / width
            } else if (height > MAX_IMAGE_SIZE) {
                MAX_IMAGE_SIZE.toFloat() / height
            } else {
                1.0f
            }

            val scaledBitmap = if (scale < 1.0) {
                Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                originalBitmap
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveBase64Image(context: Context, base64String: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val imagesFolder = File(context.cacheDir, "images")
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs()
            }
            val file = File(imagesFolder, "IMG_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use {
                it.write(imageBytes)
            }
            Uri.fromFile(file)
        } catch(e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
