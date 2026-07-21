package com.sudsmobile.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.posix.memcpy

private val activeProfileImagePickerDelegates = mutableSetOf<ProfileImagePickerDelegate>()

@Composable
internal actual fun rememberProfileImagePicker(
    onImagePicked: (PickedProfileImage) -> Unit,
    onImagePickFailed: (String) -> Unit,
): ProfileImagePickerLauncher {
    val hostViewController = LocalUIViewController.current
    return remember(hostViewController, onImagePicked, onImagePickFailed) {
        ProfileImagePickerLauncher {
            presentProfileImagePicker(
                hostViewController = hostViewController,
                onImagePicked = onImagePicked,
                onImagePickFailed = onImagePickFailed,
            )
        }
    }
}

private fun presentProfileImagePicker(
    hostViewController: UIViewController,
    onImagePicked: (PickedProfileImage) -> Unit,
    onImagePickFailed: (String) -> Unit,
): Boolean {
    return runCatching {
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = 1
            filter = PHPickerFilter.imagesFilter()
        }
        val picker = PHPickerViewController(configuration = configuration)
        val delegate = ProfileImagePickerDelegate(
            onImagePicked = onImagePicked,
            onImagePickFailed = onImagePickFailed,
        )
        activeProfileImagePickerDelegates += delegate
        picker.delegate = delegate
        hostViewController.topMostPresentedController().presentViewController(
            viewControllerToPresent = picker,
            animated = true,
            completion = null,
        )
    }.isSuccess
}

private class ProfileImagePickerDelegate(
    private val onImagePicked: (PickedProfileImage) -> Unit,
    private val onImagePickFailed: (String) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        if (didFinishPicking.isEmpty()) {
            activeProfileImagePickerDelegates.remove(this)
            return
        }
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            activeProfileImagePickerDelegates.remove(this)
            onImagePickFailed("Não foi possível carregar a imagem selecionada.")
            return
        }
        val provider = result.itemProvider
        provider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data: NSData?, error: NSError? ->
            when {
                error != null -> {
                    activeProfileImagePickerDelegates.remove(this)
                    onImagePickFailed(error.localizedDescription ?: "Não foi possível carregar a imagem selecionada.")
                }

                data == null -> {
                    activeProfileImagePickerDelegates.remove(this)
                    onImagePickFailed("Não foi possível carregar a imagem selecionada.")
                }

                data.length.toLong() == 0L -> {
                    activeProfileImagePickerDelegates.remove(this)
                    onImagePickFailed("A imagem selecionada está vazia.")
                }

                data.length.toLong() > MAX_SELECTED_PROFILE_IMAGE_BYTES -> {
                    activeProfileImagePickerDelegates.remove(this)
                    onImagePickFailed("A imagem selecionada é demasiado grande. Escolha uma imagem até 20 MB.")
                }

                else -> {
                    activeProfileImagePickerDelegates.remove(this)
                    onImagePicked(
                        PickedProfileImage(
                            bytes = data.toByteArray(),
                            mimeType = provider.registeredTypeIdentifiers.firstOrNull()?.toString() ?: "image/jpeg",
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val byteArray = ByteArray(length.toInt())
    byteArray.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return byteArray
}

private fun UIViewController.topMostPresentedController(): UIViewController {
    var current = this
    while (true) {
        val next = current.presentedViewController ?: return current
        current = next
    }
}
