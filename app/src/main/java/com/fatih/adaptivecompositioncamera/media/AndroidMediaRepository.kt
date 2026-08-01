package com.fatih.adaptivecompositioncamera.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import com.fatih.adaptivecompositioncamera.domain.model.MediaItem
import com.fatih.adaptivecompositioncamera.domain.model.MediaRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidMediaRepository : MediaRepository {
    private val lastTimestamp = AtomicLong(0L)

    override fun createImageName(extension: String): String =
        "IMG_${formattedUniqueTimestamp()}.${extension.trimStart('.')}"

    override fun createVideoName(extension: String): String =
        "VID_${formattedUniqueTimestamp()}.${extension.trimStart('.')}"

    private fun formattedUniqueTimestamp(): String {
        val timestamp = lastTimestamp.updateAndGet { previous -> maxOf(System.currentTimeMillis(), previous + 1L) }
        return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))
    }

    suspend fun latestMedia(context: Context): MediaItem? = withContext(Dispatchers.IO) {
        listOfNotNull(
            latestFromCollection(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/jpeg"),
            latestFromCollection(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/mp4"),
        ).maxByOrNull { it.dateTakenMillis }
    }

    suspend fun mediaItem(context: Context, uri: Uri, fallbackMimeType: String? = null): MediaItem? =
        withContext(Dispatchers.IO) { queryItem(context, uri, fallbackMimeType) }

    suspend fun loadThumbnail(context: Context, item: MediaItem, edgePx: Int = 192): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(item.uri, Size(edgePx, edgePx), null)
                } else {
                    decodeSampled(context, item.uri, edgePx, edgePx)
                }
            }.getOrNull()
        }

    suspend fun loadViewerBitmap(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? =
        withContext(Dispatchers.IO) { decodeSampled(context, uri, maxWidth.coerceAtLeast(1), maxHeight.coerceAtLeast(1)) }

    suspend fun delete(context: Context, item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.delete(item.uri, null, null) > 0 }.getOrDefault(false)
    }

    private fun latestFromCollection(context: Context, collection: Uri, fallbackMime: String): MediaItem? {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
        )
        val selection: String
        val arguments: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            arguments = arrayOf("DCIM/AdaptiveCompositionCamera%")
        } else {
            selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            arguments = arrayOf(if (fallbackMime.startsWith("video")) "VID_%" else "IMG_%")
        }
        return runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                arguments,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC",
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val id = cursor.long(MediaStore.MediaColumns._ID)
                MediaItem(
                    uri = ContentUris.withAppendedId(collection, id),
                    mimeType = cursor.string(MediaStore.MediaColumns.MIME_TYPE).ifBlank { fallbackMime },
                    displayName = cursor.string(MediaStore.MediaColumns.DISPLAY_NAME),
                    dateTakenMillis = cursor.long(MediaStore.MediaColumns.DATE_ADDED) * 1_000L,
                    width = cursor.int(MediaStore.MediaColumns.WIDTH),
                    height = cursor.int(MediaStore.MediaColumns.HEIGHT),
                    sizeBytes = cursor.long(MediaStore.MediaColumns.SIZE),
                    durationMillis = if (fallbackMime.startsWith("video")) videoDuration(context, ContentUris.withAppendedId(collection, id)) else 0L,
                ).withVerifiedImageMetadata(context)
            }
        }.getOrNull()
    }

    private fun queryItem(context: Context, uri: Uri, fallbackMimeType: String?): MediaItem? {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
        )
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val mime = cursor.string(MediaStore.MediaColumns.MIME_TYPE).ifBlank { fallbackMimeType.orEmpty() }
                MediaItem(
                    uri = uri,
                    mimeType = mime,
                    displayName = cursor.string(MediaStore.MediaColumns.DISPLAY_NAME),
                    dateTakenMillis = cursor.long(MediaStore.MediaColumns.DATE_ADDED) * 1_000L,
                    width = cursor.int(MediaStore.MediaColumns.WIDTH),
                    height = cursor.int(MediaStore.MediaColumns.HEIGHT),
                    sizeBytes = cursor.long(MediaStore.MediaColumns.SIZE),
                    durationMillis = if (mime.startsWith("video")) videoDuration(context, uri) else 0L,
                ).withVerifiedImageMetadata(context)
            }
        }.getOrNull()
    }

    private fun videoDuration(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Video.Media.DURATION),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.long(MediaStore.Video.Media.DURATION) else 0L } ?: 0L
    }.getOrDefault(0L)

    private fun MediaItem.withVerifiedImageMetadata(context: Context): MediaItem {
        if (!mimeType.startsWith("image/")) return this
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
        }
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
        return copy(
            width = bounds.outWidth.takeIf { it > 0 } ?: width,
            height = bounds.outHeight.takeIf { it > 0 } ?: height,
            rotationDegrees = rotation,
        )
    }

    private fun decodeSampled(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth && bounds.outHeight / (sample * 2) >= maxHeight) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        return decoded.applyExifOrientation(orientation)
    }

    private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also { transformed ->
            if (transformed !== this) recycle()
        }
    }

    private fun android.database.Cursor.string(column: String): String =
        getColumnIndex(column).takeIf { it >= 0 }?.let(::getString).orEmpty()

    private fun android.database.Cursor.long(column: String): Long =
        getColumnIndex(column).takeIf { it >= 0 }?.let(::getLong) ?: 0L

    private fun android.database.Cursor.int(column: String): Int =
        getColumnIndex(column).takeIf { it >= 0 }?.let(::getInt) ?: 0
}
