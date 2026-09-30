package com.sudsmobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.feedback.FeedbackItem
import com.sudsmobile.data.feedback.FeedbackInteractions
import com.sudsmobile.data.feedback.FeedbackRepository
import com.sudsmobile.data.feedback.FeedbackResult
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.ui.SudsBrandBackground
import com.sudsmobile.shared.ui.SudsCompactTopBar
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShakeFeedbackScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
) {
    val access: AdminAccessViewModel = koinViewModel()
    val repository: FeedbackRepository = koinInject()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val sessionState by access.sessionState.collectAsStateWithLifecycle()
    val accessState by access.uiState.collectAsStateWithLifecycle()
    var items by remember { mutableStateOf<List<FeedbackItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<FeedbackItem?>(null) }
    var screenshot by remember { mutableStateOf<ByteArray?>(null) }
    var screenshotError by remember { mutableStateOf(false) }
    var interactions by remember { mutableStateOf<FeedbackInteractions?>(null) }
    var comment by remember { mutableStateOf("") }
    var mutationPending by remember { mutableStateOf(false) }
    var mutationError by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<FeedbackItem?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var sortByLikes by remember { mutableStateOf(false) }

    suspend fun refreshInteractions(id: String) {
        when (val result = repository.loadInteractions(id)) {
            is FeedbackResult.Success -> interactions = result.value
            is FeedbackResult.Failure -> mutationError = result.message
        }
    }

    LaunchedEffect(sessionState) { access.refreshForSession() }
    LaunchedEffect(accessState, retryKey) {
        if (accessState !is AdminAccessUiState.Admin) {
            items = null
            selected = null
            pendingDelete = null
            return@LaunchedEffect
        }
        error = null
        when (val result = repository.listForAdmin()) {
            is FeedbackResult.Success -> items = result.value
            is FeedbackResult.Failure -> error = result.message
        }
    }
    LaunchedEffect(selected) {
        screenshot = null
        screenshotError = false
        interactions = null
        comment = ""
        mutationError = null
        val item = selected ?: return@LaunchedEffect
        refreshInteractions(item.id)
        if (item.screenshotStoragePath.isNotBlank()) {
            when (val result = repository.loadScreenshot(item.id)) {
                is FeedbackResult.Success -> screenshot = result.value
                is FeedbackResult.Failure -> screenshotError = true
            }
        }
    }

    SudsBrandBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            SudsCompactTopBar(
                title = "Feedback recebido",
                eyebrow = "ADMINISTRAÇÃO",
                leadingContent = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = SudsColors.onBrand,
                        )
                    }
                },
            )
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    sessionState == AuthSessionState.Unauthenticated -> {
                        Text("Inicie sessão com uma conta de administrador para ver o feedback.")
                        Button(onClick = onRequestSignIn) { Text("Iniciar sessão") }
                    }
                    accessState is AdminAccessUiState.NotAdmin -> Text("Esta área está reservada a administradores.")
                    accessState is AdminAccessUiState.Error -> {
                        Text("Não foi possível verificar o acesso. Tente novamente.")
                        Button(onClick = { access.refreshForSession(force = true) }) { Text("Tentar novamente") }
                    }
                    error != null -> {
                        Text(error.orEmpty())
                        Button(onClick = { retryKey++ }) { Text("Tentar novamente") }
                    }
                    items == null -> Text("A carregar feedback…")
                    items!!.isEmpty() -> Text("Ainda não há feedback recebido.")
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (sortByLikes) {
                                OutlinedButton(onClick = { sortByLikes = false }) { Text("Data") }
                                Button(onClick = { sortByLikes = true }) { Text("Gostos") }
                            } else {
                                Button(onClick = { sortByLikes = false }) { Text("Data") }
                                OutlinedButton(onClick = { sortByLikes = true }) { Text("Gostos") }
                            }
                        }
                        val sortedItems = if (sortByLikes) {
                            items!!.sortedWith(compareByDescending<FeedbackItem> { it.likeCount }.thenByDescending { it.createdAtIso })
                        } else items!!
                        sortedItems.forEach { item ->
                            key(item.id) {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart && !mutationPending) {
                                            pendingDelete = item
                                            deleteError = null
                                        }
                                        false
                                    },
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier.fillMaxSize()
                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                .padding(end = 20.dp),
                                            contentAlignment = Alignment.CenterEnd,
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Eliminar feedback",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                    },
                                ) {
                                    Card(modifier = Modifier.fillMaxWidth().clickable { selected = item }) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                                            if (item.likeCount > 0) {
                                                Text("${item.likeCount} gostos", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text(item.body, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${item.submitterEmail} · ${item.platform} · ${item.createdAtIso.take(10)}",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = sheetState) {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall)
                Text(item.body.ifBlank { "Sem descrição adicional." })
                Text("Enviado por ${item.submitterEmail} em ${item.createdAtIso.take(10)}")
                if (item.screenshotStoragePath.isNotBlank()) {
                    when {
                        screenshot != null -> AsyncImage(
                            model = screenshot,
                            contentDescription = "Captura de ecrã anexada",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        screenshotError -> Text("Não foi possível carregar a captura de ecrã.")
                        else -> Text("A carregar captura de ecrã…")
                    }
                }
                if (interactions == null) {
                    Text("A carregar comentários…")
                } else {
                    val current = interactions!!
                    TextButton(
                        enabled = !mutationPending,
                        onClick = {
                            focusManager.clearFocus()
                            mutationPending = true
                            mutationError = null
                            scope.launch {
                                when (val result = repository.setLiked(item.id, !current.likedByCurrentAdmin)) {
                                    is FeedbackResult.Success -> {
                                        refreshInteractions(item.id)
                                        retryKey++
                                    }
                                    is FeedbackResult.Failure -> mutationError = result.message
                                }
                                mutationPending = false
                            }
                        },
                    ) {
                        Text(if (current.likedByCurrentAdmin) "Gostei · ${current.likeCount}" else "Gosto · ${current.likeCount}")
                    }
                    current.comments.forEach { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(entry.body)
                                Text(entry.adminEmail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { if (it.length <= 1_000) comment = it },
                        label = { Text("Comentário") },
                        enabled = !mutationPending,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        enabled = comment.trim().isNotEmpty() && !mutationPending,
                        onClick = {
                            focusManager.clearFocus()
                            mutationPending = true
                            mutationError = null
                            scope.launch {
                                when (val result = repository.addComment(item.id, comment)) {
                                    is FeedbackResult.Success -> {
                                        comment = ""
                                        refreshInteractions(item.id)
                                    }
                                    is FeedbackResult.Failure -> mutationError = result.message
                                }
                                mutationPending = false
                            }
                        },
                    ) { Text("Adicionar comentário") }
                }
                mutationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = {
                    focusManager.clearFocus()
                    pendingDelete = item
                    deleteError = null
                }, enabled = !mutationPending) {
                    Text("Eliminar feedback", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!mutationPending) pendingDelete = null },
            title = { Text("Eliminar feedback?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Esta ação elimina a mensagem, os comentários e a captura de ecrã.")
                    deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !mutationPending,
                    onClick = {
                        mutationPending = true
                        deleteError = null
                        scope.launch {
                            when (val result = repository.delete(item.id)) {
                                is FeedbackResult.Success -> {
                                    items = items?.filterNot { it.id == item.id }
                                    if (selected?.id == item.id) selected = null
                                    pendingDelete = null
                                }
                                is FeedbackResult.Failure -> deleteError = result.message
                            }
                            mutationPending = false
                        }
                    },
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }, enabled = !mutationPending) { Text("Cancelar") }
            },
        )
    }
}
