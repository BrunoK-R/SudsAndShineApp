package com.sudsmobile.feature.profile

import androidx.compose.runtime.Composable

internal const val MAX_SELECTED_PROFILE_IMAGE_BYTES = 20 * 1024 * 1024

internal data class PickedProfileImage(
    val bytes: ByteArray,
    val mimeType: String,
    val displayName: String? = null,
)

internal fun interface ProfileImagePickerLauncher {
    fun launch(): Boolean
}

@Composable
internal expect fun rememberProfileImagePicker(
    onImagePicked: (PickedProfileImage) -> Unit,
    onImagePickFailed: (String) -> Unit,
): ProfileImagePickerLauncher
