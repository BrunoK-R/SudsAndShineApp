package com.sudsmobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import org.koin.compose.viewmodel.koinViewModel

private data class ProfileStat(
    val value: String,
    val label: String,
    val highlighted: Boolean = false,
    val loading: Boolean = false,
)

private data class ProfileMenuItem(
    val icon: ImageVector,
    val label: String,
    val action: ProfileMenuAction = ProfileMenuAction.None,
)

private enum class ProfileMenuAction {
    None,
    Vehicles,
    Loyalty,
    History,
    Contact,
}

private val menuItems = listOf(
    ProfileMenuItem(icon = Icons.Filled.Person, label = "Dados Pessoais"),
    ProfileMenuItem(
        icon = Icons.Filled.DirectionsCar,
        label = "Meus Veículos",
        action = ProfileMenuAction.Vehicles,
    ),
    ProfileMenuItem(
        icon = Icons.Filled.CardGiftcard,
        label = "Programa de Fidelização",
        action = ProfileMenuAction.Loyalty,
    ),
    ProfileMenuItem(
        icon = Icons.Filled.CalendarMonth,
        label = "Histórico de Lavagens",
        action = ProfileMenuAction.History,
    ),
    ProfileMenuItem(icon = Icons.Filled.Notifications, label = "Notificações"),
    ProfileMenuItem(
        icon = Icons.AutoMirrored.Filled.Help,
        label = "Ajuda e Suporte",
        action = ProfileMenuAction.Contact,
    ),
    ProfileMenuItem(icon = Icons.Filled.Security, label = "Privacidade"),
)

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onRequestSignIn: () -> Unit,
    onManageVehicles: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    onOpenRewards: () -> Unit = {},
) {
    val viewModel: ProfileViewModel = koinViewModel()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val statsState by viewModel.statsState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState) {
        viewModel.loadStats()
    }

    ProfileScreenContent(
        contentPadding = contentPadding,
        sessionState = sessionState,
        statsState = statsState,
        onRequestSignIn = onRequestSignIn,
        onSignOut = viewModel::signOut,
        onRetryStats = viewModel::loadStats,
        onManageVehicles = onManageVehicles,
        onOpenHistory = onOpenHistory,
        onOpenContact = onOpenContact,
        onOpenRewards = onOpenRewards,
    )
}

@Composable
private fun ProfileScreenContent(
    contentPadding: PaddingValues,
    sessionState: AuthSessionState,
    statsState: ProfileStatsUiState,
    onRequestSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRetryStats: () -> Unit,
    onManageVehicles: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    onOpenRewards: () -> Unit = {},
) {
    val authenticatedUser = (sessionState as? AuthSessionState.Authenticated)?.session?.user
    val isRestoringSession = sessionState == AuthSessionState.Restoring

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        if (authenticatedUser != null) {
            ProfileHeader(
                user = authenticatedUser,
                statsState = statsState,
                onRetryStats = onRetryStats,
            )
        } else if (isRestoringSession) {
            RestoringProfileHeader()
        } else {
            GuestProfileHeader(onRequestSignIn = onRequestSignIn)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NearestLocationCard()
            if (authenticatedUser != null) {
                ProfileMenuCard(
                    onManageVehicles = onManageVehicles,
                    onOpenHistory = onOpenHistory,
                    onOpenContact = onOpenContact,
                    onOpenRewards = onOpenRewards,
                )
                PreferencesCard()
                LogoutButton(onClick = onSignOut)
            } else if (isRestoringSession) {
                RestoringSessionCard()
            } else {
                GuestProfileCard(onRequestSignIn = onRequestSignIn)
            }
            AppVersionText()
        }
    }
}

@Composable
private fun ProfileHeader(
    user: AuthUser,
    statsState: ProfileStatsUiState,
    onRetryStats: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.inverseSurface,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 40.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.initials(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = user.resolvedDisplayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.62f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            statsState.toProfileStats().forEach { stat ->
                ProfileStatCard(
                    stat = stat,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ProfileStatsStatus(
            statsState = statsState,
            onRetryStats = onRetryStats,
        )
    }
}

@Composable
private fun RestoringProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.inverseSurface,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        strokeWidth = 3.dp,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "A validar sessão",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Estamos a recuperar a sua conta neste dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.66f),
                )
            }
        }
    }
}

@Composable
private fun GuestProfileHeader(onRequestSignIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.inverseSurface,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Área pessoal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Entre para ver os seus dados e marcações.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.66f),
                )
            }
        }

        Button(
            onClick = onRequestSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        ) {
            Text(
                text = "Entrar ou criar conta",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RestoringSessionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "A carregar dados da conta",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Os veículos e histórico aparecem assim que a sessão for validada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GuestProfileCard(onRequestSignIn: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ProfileIconContainer(icon = Icons.Filled.Security)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Sessão necessária",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Os veículos, histórico e preferências ficam associados à sua conta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = onRequestSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                Text(
                    text = "Iniciar sessão",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    stat: ProfileStat,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (stat.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = if (stat.highlighted) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    },
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stat.value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (stat.highlighted) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.64f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProfileStatsStatus(
    statsState: ProfileStatsUiState,
    onRetryStats: () -> Unit,
) {
    val message = when (statsState) {
        is ProfileStatsUiState.Error -> statsState.message
        is ProfileStatsUiState.Loaded -> statsState.warningMessage
        ProfileStatsUiState.Idle,
        ProfileStatsUiState.Loading,
        ProfileStatsUiState.Unauthenticated -> null
    } ?: return
    val retryable = when (statsState) {
        is ProfileStatsUiState.Error -> statsState.retryable
        is ProfileStatsUiState.Loaded -> statsState.warningRetryable
        ProfileStatsUiState.Idle,
        ProfileStatsUiState.Loading,
        ProfileStatsUiState.Unauthenticated -> false
    }

    Spacer(Modifier.height(12.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.78f),
            )
            if (retryable) {
                TextButton(
                    onClick = onRetryStats,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Text(
                        text = "Atualizar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun ProfileStatsUiState.toProfileStats(): List<ProfileStat> {
    return when (this) {
        ProfileStatsUiState.Idle,
        ProfileStatsUiState.Loading -> listOf(
            ProfileStat(value = "", label = "Lavagens", loading = true),
            ProfileStat(value = "", label = "Faltam", highlighted = true, loading = true),
            ProfileStat(value = "", label = "Veículos", loading = true),
        )

        is ProfileStatsUiState.Loaded -> listOf(
            ProfileStat(value = stats.washCount, label = "Lavagens"),
            ProfileStat(value = stats.loyaltyRemaining, label = "Faltam", highlighted = true),
            ProfileStat(value = stats.vehicleCount, label = "Veículos"),
        )

        ProfileStatsUiState.Unauthenticated,
        is ProfileStatsUiState.Error -> listOf(
            ProfileStat(value = "0", label = "Lavagens"),
            ProfileStat(value = "0", label = "Faltam", highlighted = true),
            ProfileStat(value = "0", label = "Veículos"),
        )
    }
}

private fun AuthUser.initials(): String {
    val source = displayName.ifBlank { email.substringBefore("@") }
    return source
        .split(" ", ".", "_", "-", limit = 4)
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercaseChar().toString() }
        .ifBlank { "SS" }
}

@Composable
private fun NearestLocationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Suds & Shine mais próximo:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ProfileIconContainer(icon = Icons.Filled.Place)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Morada",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "R. Virgílio Vieira da Cunha, 2400-447 Leiria",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                    ) {
                        Text(
                            text = "Navegar até",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = "Leiria",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuCard(
    onManageVehicles: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenRewards: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column {
            menuItems.forEachIndexed { index, item ->
                ProfileMenuRow(
                    item = item,
                    onClick = {
                        when (item.action) {
                            ProfileMenuAction.None -> Unit
                            ProfileMenuAction.Vehicles -> onManageVehicles()
                            ProfileMenuAction.Loyalty -> onOpenRewards()
                            ProfileMenuAction.History -> onOpenHistory()
                            ProfileMenuAction.Contact -> onOpenContact()
                        }
                    },
                )
                if (index != menuItems.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    item: ProfileMenuItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileIconContainer(icon = item.icon)
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun PreferencesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Preferências",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            PreferenceRow(
                title = "Notificações Push",
                description = "Receber lembretes de marcações",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PreferenceRow(
                title = "Email Marketing",
                description = "Receber ofertas e promoções",
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = true,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                checkedBorderColor = MaterialTheme.colorScheme.tertiary,
            ),
        )
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Terminar Sessão",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AppVersionText() {
    Text(
        text = "Versão 1.0.0 • Suds & Shine Solutions",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ProfileIconContainer(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f),
        contentColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
