package com.sudsmobile.feature.profile

internal data class ProfileAvatarImageDimensions(
    val width: Int,
    val height: Int,
)

internal expect fun decodeProfileAvatarImageDimensions(
    imageBytes: ByteArray,
): ProfileAvatarImageDimensions?

internal expect suspend fun cropProfileAvatarToJpeg(
    imageBytes: ByteArray,
    sourceRect: ProfileAvatarSourceCropRect,
    outputSizePx: Int = 512,
    qualityPercent: Int = 88,
): ByteArray
