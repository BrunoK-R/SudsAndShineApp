package com.sudsmobile.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val onboardingSlides = listOf(
    OnboardingSlide(
        icon = Icons.Filled.CalendarMonth,
        title = "Marcação Simples",
        description = "Marque o seu serviço de lavagem em poucos passos. Rápido, fácil e conveniente.",
    ),
    OnboardingSlide(
        icon = Icons.Filled.AutoAwesome,
        title = "Cuidado Premium",
        description = "Produtos de qualidade superior e acabamento impecável para o seu veículo.",
    ),
    OnboardingSlide(
        icon = Icons.Filled.CardGiftcard,
        title = "Programa de Fidelização",
        description = "A cada 10 lavagens, ganhe 1 lavagem grátis. Acumule recompensas facilmente.",
    ),
    OnboardingSlide(
        icon = Icons.Filled.Star,
        title = "Histórico e Avaliações",
        description = "Acompanhe o histórico das suas lavagens e ajude-nos a melhorar o serviço.",
    ),
)

@Composable
fun OnboardingScreen(
    actionsEnabled: Boolean,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
) {
    var currentSlide by rememberSaveable { mutableStateOf(0) }
    val slide = onboardingSlides[currentSlide]
    val isLastSlide = currentSlide == onboardingSlides.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 36.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                OnboardingIllustration(slide.icon)
                Spacer(Modifier.height(48.dp))
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SlideIndicators(currentSlide = currentSlide)

            Button(
                onClick = {
                    if (isLastSlide) {
                        onComplete()
                    } else {
                        currentSlide += 1
                    }
                },
                enabled = actionsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (isLastSlide) "Começar" else "Seguinte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!isLastSlide) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Text(
                text = "Saltar",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = actionsEnabled, onClick = onSkip)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun OnboardingIllustration(icon: ImageVector) {
    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(176.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.44f),
            shape = CircleShape,
        ) {}
        Surface(
            modifier = Modifier.size(160.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 10.dp,
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.inverseSurface,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                    tint = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SlideIndicators(currentSlide: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onboardingSlides.indices.forEach { index ->
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (index == currentSlide) 32.dp else 8.dp, height = 8.dp),
                color = if (index == currentSlide) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape,
            ) {}
        }
    }
}
