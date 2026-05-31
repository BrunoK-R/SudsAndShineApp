package com.sudsmobile.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

@Composable
internal actual fun PlatformGoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    val credentialRequest = remember(webClientId) {
        GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build(),
            )
            .build()
    }

    GoogleAuthButton(
        enabled = enabled,
        onClick = {
            coroutineScope.launch {
                try {
                    val credential = credentialManager.getCredential(
                        context = context,
                        request = credentialRequest,
                    ).credential
                    val idToken = credential.googleIdTokenOrNull()
                    if (idToken.isNullOrBlank()) {
                        onError("Não foi possível obter a sessão Google. Tente novamente.")
                    } else {
                        onIdToken(idToken)
                    }
                } catch (cause: GetCredentialCancellationException) {
                    // The user closed the account picker.
                } catch (cause: GetCredentialException) {
                    onError("Não foi possível iniciar sessão com Google. Tente novamente.")
                } catch (cause: GoogleIdTokenParsingException) {
                    onError("Não foi possível validar a sessão Google. Tente novamente.")
                }
            }
        },
    )
}

internal actual fun isGoogleSignInAvailable(): Boolean = true

private fun androidx.credentials.Credential.googleIdTokenOrNull(): String? {
    if (this !is CustomCredential ||
        type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        return null
    }

    return GoogleIdTokenCredential.createFrom(data).idToken
}
