package com.sudsmobile.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
internal actual fun PlatformGoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    val signInOptions = remember(webClientId) {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (webClientId.isNotBlank()) {
            builder.requestIdToken(webClientId)
        }
        builder.build()
    }
    val googleSignInClient = remember(context, signInOptions) {
        GoogleSignIn.getClient(context, signInOptions)
    }
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = accountTask.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                onError("Não foi possível obter a sessão Google. Tente novamente.")
            } else {
                onIdToken(idToken)
            }
        } catch (cause: ApiException) {
            if (cause.statusCode != GoogleSignInStatusSignInCancelled) {
                onError("Não foi possível iniciar sessão com Google. Tente novamente.")
            }
        }
    }

    GoogleAuthButton(
        enabled = enabled,
        onClick = {
            if (webClientId.isBlank()) {
                onError("Google Sign-In não está configurado nesta build.")
            } else {
                signInLauncher.launch(googleSignInClient.signInIntent)
            }
        },
    )
}

private const val GoogleSignInStatusSignInCancelled = 12501
