@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.sudsmobile.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ObjCAction
import platform.AuthenticationServices.ASAuthorizationAppleIDButton
import platform.AuthenticationServices.ASAuthorizationAppleIDButtonStyle.ASAuthorizationAppleIDButtonStyleWhiteOutline
import platform.AuthenticationServices.ASAuthorizationAppleIDButtonTypeContinue
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIColor
import platform.darwin.NSObject

internal actual fun isAppleSignInAvailable(): Boolean = IosAppleSignInBridge.isAvailable()

@Composable
internal actual fun PlatformAppleSignInButton(
    enabled: Boolean,
    onStart: () -> Boolean,
    onCredential: (String, String, String?) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
) {
    val currentOnClick = rememberUpdatedState {
        if (enabled && onStart()) {
            IosAppleSignInBridge.signIn(onCredential, onError, onCancelled)
        }
    }
    val target = remember { AppleButtonTarget { currentOnClick.value() } }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        UIKitView(
            factory = {
                ASAuthorizationAppleIDButton(
                    authorizationButtonType = ASAuthorizationAppleIDButtonTypeContinue,
                    authorizationButtonStyle = ASAuthorizationAppleIDButtonStyleWhiteOutline,
                ).apply {
                    cornerRadius = 12.0
                    backgroundColor = UIColor.clearColor
                    layer.cornerRadius = 12.0
                    clipsToBounds = true
                    addTarget(target, NSSelectorFromString("onTap"), UIControlEventTouchUpInside)
                }
            },
            modifier = Modifier
                .widthIn(max = 375.dp)
                .fillMaxWidth()
                .height(AppleSignInButtonHeight)
                .clip(RoundedCornerShape(12.dp))
                .semantics {
                    contentDescription = "Continuar com Apple"
                    role = Role.Button
                    if (!enabled) disabled()
                    onClick {
                        if (enabled) currentOnClick.value()
                        enabled
                    }
                },
            update = { it.enabled = enabled },
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                // Compose exposes one localized accessibility action for the embedded control.
                isNativeAccessibilityEnabled = false,
            ),
        )
    }
}

private class AppleButtonTarget(private val onClick: () -> Unit) : NSObject() {
    @ObjCAction
    fun onTap() = onClick()
}

object IosAppleSignInBridge {
    private var signInHandler: (((String, String, String?) -> Unit, (String) -> Unit, () -> Unit) -> Unit)? = null

    fun configure(
        signInHandler: (((String, String, String?) -> Unit, (String) -> Unit, () -> Unit) -> Unit)?,
    ) {
        this.signInHandler = signInHandler
    }

    fun isAvailable(): Boolean = signInHandler != null

    fun signIn(
        onCredential: (String, String, String?) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit,
    ) {
        val handler = signInHandler
        if (handler == null) {
            onError("O início de sessão com Apple ainda não está disponível.")
            return
        }
        handler(onCredential, onError, onCancelled)
    }
}
