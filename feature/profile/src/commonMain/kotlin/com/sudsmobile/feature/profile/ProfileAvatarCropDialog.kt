package com.sudsmobile.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import kotlinx.coroutines.launch

internal const val PROFILE_AVATAR_OUTPUT_SIZE_PX = 512
internal const val PROFILE_AVATAR_QUALITY_PERCENT = 88

@Composable
internal fun ProfileAvatarCropDialog(
    sourceImage: PickedProfileImage,
    onDismissRequest: () -> Unit,
    onCropApplied: (ByteArray) -> Unit,
    onCropFailed: (String) -> Unit,
) {
    val platformContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val sourceDimensions = remember(sourceImage.bytes) {
        decodeProfileAvatarImageDimensions(sourceImage.bytes)
    }
    var viewportSizePx by remember(sourceImage.bytes) { mutableIntStateOf(0) }
    var applying by remember(sourceImage.bytes) { mutableStateOf(false) }
    var transform by remember(sourceImage.bytes) { mutableStateOf(ProfileAvatarCropTransform()) }

    LaunchedEffect(sourceDimensions, viewportSizePx) {
        val dimensions = sourceDimensions ?: return@LaunchedEffect
        if (viewportSizePx <= 0) return@LaunchedEffect
        transform = clampProfileAvatarCropTransform(
            imageWidthPx = dimensions.width,
            imageHeightPx = dimensions.height,
            viewportSizePx = viewportSizePx,
            transform = transform,
        )
    }

    Dialog(onDismissRequest = { if (!applying) onDismissRequest() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Ajustar foto",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Arrasta e usa dois dedos para enquadrar a foto no círculo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .onSizeChanged { viewportSizePx = it.width }
                        .pointerInput(sourceDimensions, viewportSizePx, applying) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val dimensions = sourceDimensions ?: return@detectTransformGestures
                                if (viewportSizePx <= 0 || applying) return@detectTransformGestures
                                transform = clampProfileAvatarCropTransform(
                                    imageWidthPx = dimensions.width,
                                    imageHeightPx = dimensions.height,
                                    viewportSizePx = viewportSizePx,
                                    transform = ProfileAvatarCropTransform(
                                        scale = transform.scale * zoom,
                                        offsetX = transform.offsetX + pan.x,
                                        offsetY = transform.offsetY + pan.y,
                                    ),
                                )
                            }
                        },
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(platformContext)
                            .data(sourceImage.bytes)
                            .build(),
                        contentDescription = "Pré-visualização da foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = transform.scale
                                scaleY = transform.scale
                                translationX = transform.offsetX
                                translationY = transform.offsetY
                            },
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            }
                        },
                        success = { SubcomposeAsyncImageContent() },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(color = Color.Black.copy(alpha = 0.48f))
                            val radius = (size.minDimension / 2f) - 2.dp.toPx()
                            drawCircle(
                                color = Color.Transparent,
                                radius = radius,
                                center = Offset(size.width / 2f, size.height / 2f),
                                blendMode = BlendMode.Clear,
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.8f),
                                radius = radius,
                                center = Offset(size.width / 2f, size.height / 2f),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                    if (applying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 3.dp,
                            )
                        }
                    }
                }
                Text(
                    text = "Zoom",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = transform.scale,
                    onValueChange = { scale ->
                        val dimensions = sourceDimensions ?: return@Slider
                        transform = clampProfileAvatarCropTransform(
                            imageWidthPx = dimensions.width,
                            imageHeightPx = dimensions.height,
                            viewportSizePx = viewportSizePx,
                            transform = transform.copy(scale = scale),
                        )
                    },
                    enabled = !applying && sourceDimensions != null && viewportSizePx > 0,
                    valueRange = PROFILE_AVATAR_MIN_SCALE..PROFILE_AVATAR_MAX_SCALE,
                    modifier = Modifier.semantics {
                        contentDescription = "Zoom da foto de perfil"
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(enabled = !applying, onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                    TextButton(
                        enabled = !applying && sourceDimensions != null && viewportSizePx > 0,
                        onClick = {
                            val dimensions = sourceDimensions
                            if (dimensions == null || viewportSizePx <= 0) {
                                onCropFailed("Não foi possível processar esta imagem.")
                                return@TextButton
                            }
                            applying = true
                            scope.launch {
                                runCatching {
                                    cropProfileAvatarToJpeg(
                                        imageBytes = sourceImage.bytes,
                                        sourceRect = resolveProfileAvatarSourceCropRect(
                                            imageWidthPx = dimensions.width,
                                            imageHeightPx = dimensions.height,
                                            viewportSizePx = viewportSizePx,
                                            transform = transform,
                                        ),
                                        outputSizePx = PROFILE_AVATAR_OUTPUT_SIZE_PX,
                                        qualityPercent = PROFILE_AVATAR_QUALITY_PERCENT,
                                    )
                                }.onSuccess(onCropApplied)
                                    .onFailure {
                                        applying = false
                                        onCropFailed("Não foi possível processar esta imagem.")
                                    }
                            }
                        },
                    ) {
                        Text("Usar foto")
                    }
                }
            }
        }
    }
}
