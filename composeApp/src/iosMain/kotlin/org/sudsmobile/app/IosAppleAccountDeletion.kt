package org.sudsmobile.app

import com.sudsmobile.feature.profile.IosAppleAccountDeletionBridge

fun configureIosAppleAccountDeletion(
    authorizationHandler: (((String) -> Unit, (String) -> Unit, () -> Unit) -> Unit)?,
) {
    IosAppleAccountDeletionBridge.configure(authorizationHandler)
}
