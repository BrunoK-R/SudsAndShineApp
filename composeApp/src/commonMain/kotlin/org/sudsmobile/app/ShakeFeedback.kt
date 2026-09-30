package org.sudsmobile.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.feedback.FEEDBACK_BODY_MAX_LENGTH
import com.sudsmobile.data.feedback.FEEDBACK_SCREENSHOT_MAX_BYTES
import com.sudsmobile.data.feedback.FEEDBACK_TITLE_MAX_LENGTH
import com.sudsmobile.data.feedback.FeedbackRepository
import com.sudsmobile.data.feedback.FeedbackResult
import com.sudsmobile.data.feedback.FeedbackScreenshot
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.sqrt

internal data class ShakeConfig(
    val thresholdG: Double = 2.7,
    val requiredImpulses: Int = 2,
    val impulseWindowMillis: Long = 500,
    val cooldownMillis: Long = 1_500,
)

internal class ShakeClassifier(private val config: ShakeConfig = ShakeConfig()) {
    private val impulses = mutableListOf<Long>()
    private var lastShakeAt: Long? = null
    private var aboveThreshold = false

    fun onSample(xG: Double, yG: Double, zG: Double, atMillis: Long): Boolean {
        if (sqrt(xG * xG + yG * yG + zG * zG) < config.thresholdG) {
            aboveThreshold = false
            return false
        }
        if (aboveThreshold) return false
        aboveThreshold = true
        if (lastShakeAt?.let { atMillis - it < config.cooldownMillis } == true) return false
        impulses.removeAll { atMillis - it > config.impulseWindowMillis }
        impulses += atMillis
        if (impulses.size < config.requiredImpulses) return false
        impulses.clear()
        lastShakeAt = atMillis
        return true
    }
}

private val iosShakeEvents = MutableSharedFlow<FeedbackScreenshot?>(extraBufferCapacity = 1)

@OptIn(ExperimentalEncodingApi::class)
fun requestShakeFeedbackFromIos(screenshotBase64: String?, widthPx: Int, heightPx: Int) {
    val screenshot = if (screenshotBase64 != null && widthPx > 0 && heightPx > 0) {
        val bytes = Base64.Default.decode(screenshotBase64)
        if (bytes.isNotEmpty() && bytes.size <= FEEDBACK_SCREENSHOT_MAX_BYTES) {
            FeedbackScreenshot(bytes, "image/jpeg", widthPx, heightPx)
        } else null
    } else null
    iosShakeEvents.tryEmit(screenshot)
}

@Composable
internal expect fun PlatformShakeEffect(enabled: Boolean, onShake: () -> Unit)

@Composable
internal expect fun rememberFeedbackScreenshotCapture(): suspend () -> FeedbackScreenshot?

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShakeFeedbackHost(content: @Composable () -> Unit) {
    val auth: AuthRepository = koinInject()
    val feedback: FeedbackRepository = koinInject()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val capture = rememberFeedbackScreenshotCapture()
    var visible by remember { mutableStateOf(false) }
    var screenshot by remember { mutableStateOf<FeedbackScreenshot?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun open(attached: FeedbackScreenshot?) {
        if (visible || submitting) return
        screenshot = attached
        title = ""
        body = ""
        message = null
        visible = true
    }

    PlatformShakeEffect(enabled = !visible) {
        scope.launch { open(capture()) }
    }
    LaunchedEffect(Unit) {
        iosShakeEvents.collect { open(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (visible) {
        val signedIn = auth.sessionState.value is AuthSessionState.Authenticated
        ModalBottomSheet(onDismissRequest = { if (!submitting) visible = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .imePadding().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Enviar feedback", style = MaterialTheme.typography.headlineSmall)
                if (!signedIn) {
                    Text("Inicie sessão para enviar feedback.")
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= FEEDBACK_TITLE_MAX_LENGTH) title = it },
                        label = { Text("Título") },
                        singleLine = true,
                        enabled = !submitting,
                        supportingText = { Text("${title.length}/$FEEDBACK_TITLE_MAX_LENGTH") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { if (it.length <= FEEDBACK_BODY_MAX_LENGTH) body = it },
                        label = { Text("Descrição") },
                        minLines = 4,
                        enabled = !submitting,
                        supportingText = { Text("${body.length}/$FEEDBACK_BODY_MAX_LENGTH") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    screenshot?.let { attached ->
                        Text("Captura de ecrã anexada")
                        AsyncImage(
                            model = attached.bytes,
                            contentDescription = "Pré visualização da captura de ecrã",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        )
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (submitting) CircularProgressIndicator()
                    Button(
                        onClick = {
                            submitting = true
                            message = null
                            scope.launch {
                                when (val result = feedback.submit(title, body, platformName(), screenshot)) {
                                    is FeedbackResult.Success -> {
                                        visible = false
                                        screenshot = null
                                        scope.launch { snackbar.showSnackbar("Feedback enviado. Obrigado!") }
                                    }
                                    is FeedbackResult.Failure -> message = result.message
                                }
                                submitting = false
                            }
                        },
                        enabled = title.trim().isNotEmpty() && !submitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Enviar") }
                }
            }
        }
    }
}

internal expect fun platformName(): String
