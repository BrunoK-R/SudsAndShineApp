package com.sudsmobile.feature.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual fun decodeProfileAvatarImageDimensions(
    imageBytes: ByteArray,
): ProfileAvatarImageDimensions? {
    if (imageBytes.isEmpty()) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    val orientation = readExifOrientation(imageBytes)
    val swapsAxes = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
        orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
        orientation == ExifInterface.ORIENTATION_TRANSVERSE
    return if (swapsAxes) {
        ProfileAvatarImageDimensions(width = options.outHeight, height = options.outWidth)
    } else {
        ProfileAvatarImageDimensions(width = options.outWidth, height = options.outHeight)
    }
}

internal actual suspend fun cropProfileAvatarToJpeg(
    imageBytes: ByteArray,
    sourceRect: ProfileAvatarSourceCropRect,
    outputSizePx: Int,
    qualityPercent: Int,
): ByteArray = withContext(Dispatchers.Default) {
    val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        ?: error("Unable to decode selected avatar image")
    val bitmap = applyExifOrientation(decodedBitmap, readExifOrientation(imageBytes))
    val safeSize = sourceRect.size.coerceAtLeast(1).coerceAtMost(minOf(bitmap.width, bitmap.height))
    val safeLeft = sourceRect.left.coerceIn(0, bitmap.width - safeSize)
    val safeTop = sourceRect.top.coerceIn(0, bitmap.height - safeSize)
    val cropped = Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeSize, safeSize)
    val resized = if (safeSize == outputSizePx) {
        cropped
    } else {
        Bitmap.createScaledBitmap(cropped, outputSizePx, outputSizePx, true)
    }
    val stream = ByteArrayOutputStream()
    val compressed = resized.compress(
        Bitmap.CompressFormat.JPEG,
        qualityPercent.coerceIn(60, 100),
        stream,
    )
    if (!compressed) error("Unable to encode avatar image")
    if (cropped !== resized) cropped.recycle()
    resized.recycle()
    bitmap.recycle()
    stream.toByteArray()
}

private fun readExifOrientation(imageBytes: ByteArray): Int {
    if (imageBytes.isEmpty()) return ExifInterface.ORIENTATION_NORMAL
    return runCatching {
        ByteArrayInputStream(imageBytes).use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
}

private fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        else -> return source
    }
    val transformed = Bitmap.createBitmap(
        source,
        0,
        0,
        source.width,
        source.height,
        matrix,
        true,
    )
    if (transformed !== source) source.recycle()
    return transformed
}
