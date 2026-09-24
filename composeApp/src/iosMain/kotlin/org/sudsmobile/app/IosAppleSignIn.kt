package org.sudsmobile.app

import com.sudsmobile.feature.auth.IosAppleSignInBridge

fun configureIosAppleSignIn(
    signInHandler: (((String, String, String?) -> Unit, (String) -> Unit, () -> Unit) -> Unit)?,
) {
    IosAppleSignInBridge.configure(signInHandler)
}
