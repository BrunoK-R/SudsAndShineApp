package org.sudsmobile.app

import com.sudsmobile.feature.auth.IosGoogleSignInBridge

fun configureIosGoogleSignIn(
    signInHandler: (((String) -> Unit, (String) -> Unit) -> Unit)?,
) {
    IosGoogleSignInBridge.configure(signInHandler)
}
