@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sudsmobile.feature.profile

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Foundation.length
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.drawInRect
import platform.posix.memcpy

internal actual fun decodeProfileAvatarImageDimensions(
    imageBytes: ByteArray,
): ProfileAvatarImageDimensions? {
    if (imageBytes.isEmpty()) return null
    val image = UIImage(data = imageBytes.toNSData()) ?: return null
    val normalizedImage = image.normalizedOrientationImage()
    val cgImage = normalizedImage.CGImage ?: return null
    val width = CGImageGetWidth(cgImage).toInt()
    val height = CGImageGetHeight(cgImage).toInt()
    if (width <= 0 || height <= 0) return null
    return ProfileAvatarImageDimensions(width = width, height = height)
}

internal actual suspend fun cropProfileAvatarToJpeg(
    imageBytes: ByteArray,
    sourceRect: ProfileAvatarSourceCropRect,
    outputSizePx: Int,
    qualityPercent: Int,
): ByteArray {
    val image = UIImage(data = imageBytes.toNSData()) ?: error("Unable to decode selected avatar image")
    val normalizedImage = image.normalizedOrientationImage()
    val cgImage = normalizedImage.CGImage ?: error("Unable to decode selected avatar image")
    val width = CGImageGetWidth(cgImage).toInt()
    val height = CGImageGetHeight(cgImage).toInt()
    val safeSize = sourceRect.size.coerceAtLeast(1).coerceAtMost(minOf(width, height))
    val safeLeft = sourceRect.left.coerceIn(0, width - safeSize)
    val safeTop = sourceRect.top.coerceIn(0, height - safeSize)
    val croppedCgImage = CGImageCreateWithImageInRect(
        cgImage,
        CGRectMake(
            x = safeLeft.toDouble(),
            y = safeTop.toDouble(),
            width = safeSize.toDouble(),
            height = safeSize.toDouble(),
        ),
    ) ?: error("Unable to crop selected avatar image")

    UIGraphicsBeginImageContextWithOptions(
        size = CGSizeMake(outputSizePx.toDouble(), outputSizePx.toDouble()),
        opaque = false,
        scale = 1.0,
    )
    try {
        UIImage.imageWithCGImage(croppedCgImage).drawInRect(
            CGRectMake(0.0, 0.0, outputSizePx.toDouble(), outputSizePx.toDouble()),
        )
        val renderedImage = UIGraphicsGetImageFromCurrentImageContext()
            ?: error("Unable to render cropped avatar image")
        val data = UIImageJPEGRepresentation(
            renderedImage,
            qualityPercent.coerceIn(60, 100).toDouble() / 100.0,
        ) ?: error("Unable to encode avatar image")
        return data.toByteArray()
    } finally {
        UIGraphicsEndImageContext()
    }
}

private fun UIImage.normalizedOrientationImage(): UIImage {
    val renderWidth = size.useContents { width }
    val renderHeight = size.useContents { height }
    if (renderWidth <= 0.0 || renderHeight <= 0.0) return this
    UIGraphicsBeginImageContextWithOptions(
        size = CGSizeMake(renderWidth, renderHeight),
        opaque = false,
        scale = 1.0,
    )
    try {
        drawInRect(CGRectMake(0.0, 0.0, renderWidth, renderHeight))
        return UIGraphicsGetImageFromCurrentImageContext() ?: this
    } finally {
        UIGraphicsEndImageContext()
    }
}

private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.dataWithBytes(
        bytes = pinned.addressOf(0),
        length = size.convert(),
    ) ?: error("Unable to allocate image bytes")
}

private fun NSData.toByteArray(): ByteArray {
    val byteArray = ByteArray(length.toInt())
    byteArray.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return byteArray
}
