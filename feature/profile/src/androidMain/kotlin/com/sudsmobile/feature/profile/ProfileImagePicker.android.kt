package com.sudsmobile.feature.profile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberProfileImagePicker(
    onImagePicked: (PickedProfileImage) -> Unit,
    onImagePickFailed: (String) -> Unit,
): ProfileImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                require(bytes.isNotEmpty()) { "A imagem selecionada está vazia." }
                require(bytes.size <= MAX_SELECTED_PROFILE_IMAGE_BYTES) {
                    "A imagem selecionada é demasiado grande. Escolha uma imagem até 20 MB."
                }
                PickedProfileImage(
                    bytes = bytes,
                    mimeType = context.contentResolver.getType(uri)?.trim().orEmpty().ifBlank { "image/jpeg" },
                    displayName = context.resolveDisplayName(uri),
                )
            } ?: error("Não foi possível abrir a imagem selecionada.")
        }.onSuccess(onImagePicked)
            .onFailure {
                onImagePickFailed(it.message ?: "Não foi possível carregar a imagem selecionada.")
            }
    }

    return remember(launcher) {
        ProfileImagePickerLauncher {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
            true
        }
    }
}

private fun Context.resolveDisplayName(uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex == -1) null else cursor.getString(columnIndex)
    }
}
