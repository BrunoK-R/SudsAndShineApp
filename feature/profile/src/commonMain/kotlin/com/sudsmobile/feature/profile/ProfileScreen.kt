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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsCustomerTheme
import com.sudsmobile.shared.ui.SudsBrandBackground
import com.sudsmobile.shared.ui.SudsCompactTopBar
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
    AdminBookings,
    AdminAvailability,
    AdminBookingPolicy,
    AdminLoyaltySettings,
    AdminNotificationSettings,
    AdminNotificationCampaignDrafts,
    AdminBusinessInfo,
    AdminServiceCatalog,
    AdminServiceExtras,
    PersonalData,
    NotificationPreferences,
    Vehicles,
    Loyalty,
    History,
    Contact,
}

private val adminMenuItem = ProfileMenuItem(
    icon = Icons.Filled.Security,
    label = "Operação de hoje",
    action = ProfileMenuAction.AdminBookings,
)

private val adminAvailabilityMenuItem = ProfileMenuItem(
    icon = Icons.Filled.CalendarMonth,
    label = "Disponibilidade",
    action = ProfileMenuAction.AdminAvailability,
)

private val adminBookingPolicyMenuItem = ProfileMenuItem(
    icon = Icons.Filled.CalendarMonth,
    label = "Política de marcações",
    action = ProfileMenuAction.AdminBookingPolicy,
)

private val adminLoyaltySettingsMenuItem = ProfileMenuItem(
    icon = Icons.Filled.CardGiftcard,
    label = "Fidelização",
    action = ProfileMenuAction.AdminLoyaltySettings,
)

private val adminNotificationSettingsMenuItem = ProfileMenuItem(
    icon = Icons.Filled.Notifications,
    label = "Notificações",
    action = ProfileMenuAction.AdminNotificationSettings,
)

private val adminNotificationCampaignDraftsMenuItem = ProfileMenuItem(
    icon = Icons.Filled.Notifications,
    label = "Campanhas push",
    action = ProfileMenuAction.AdminNotificationCampaignDrafts,
)

private val adminBusinessInfoMenuItem = ProfileMenuItem(
    icon = Icons.Filled.Business,
    label = "Configuração do negócio",
    action = ProfileMenuAction.AdminBusinessInfo,
)

private val adminServiceCatalogMenuItem = ProfileMenuItem(
    icon = Icons.Filled.Build,
    label = "Catálogo de serviços",
    action = ProfileMenuAction.AdminServiceCatalog,
)

private val adminServiceExtrasMenuItem = ProfileMenuItem(
    icon = Icons.Filled.Build,
    label = "Extras de serviço",
    action = ProfileMenuAction.AdminServiceExtras,
)

private val adminMenuItems = listOf(
    adminMenuItem,
    adminAvailabilityMenuItem,
    adminNotificationSettingsMenuItem,
    adminNotificationCampaignDraftsMenuItem,
    adminServiceCatalogMenuItem,
    adminServiceExtrasMenuItem,
)

private val menuItems = listOf(
    ProfileMenuItem(
        icon = Icons.Filled.Person,
        label = "Dados Pessoais",
        action = ProfileMenuAction.PersonalData,
    ),
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
    ProfileMenuItem(
        icon = Icons.Filled.Notifications,
        label = "Notificações",
        action = ProfileMenuAction.NotificationPreferences,
    ),
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
    onOpenPersonalData: () -> Unit = {},
    onOpenNotificationPreferences: () -> Unit = {},
    onManageVehicles: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    onOpenRewards: () -> Unit = {},
    onOpenAdminBookings: () -> Unit = {},
    onOpenAdminAvailability: () -> Unit = {},
    onOpenAdminBookingPolicy: () -> Unit = {},
    onOpenAdminLoyaltySettings: () -> Unit = {},
    onOpenAdminNotificationSettings: () -> Unit = {},
    onOpenAdminNotificationCampaignDrafts: () -> Unit = {},
    onOpenAdminBusinessInfo: () -> Unit = {},
    onOpenAdminServiceCatalog: () -> Unit = {},
    onOpenAdminServiceExtras: () -> Unit = {},
) {
    val viewModel: ProfileViewModel = koinViewModel()
    val adminAccessViewModel: AdminAccessViewModel = koinViewModel()
    val contactViewModel: ContactViewModel = koinViewModel()
    val notificationPreferencesViewModel: NotificationPreferencesViewModel = koinViewModel()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val adminAccessState by adminAccessViewModel.uiState.collectAsStateWithLifecycle()
    val bookingRevision by viewModel.bookingRevision.collectAsStateWithLifecycle()
    val vehicleRevision by viewModel.vehicleRevision.collectAsStateWithLifecycle()
    val profileRevision by viewModel.profileRevision.collectAsStateWithLifecycle()
    val statsState by viewModel.statsState.collectAsStateWithLifecycle()
    val preferencesState by viewModel.preferencesState.collectAsStateWithLifecycle()
    val profilePhotoState by viewModel.profilePhotoState.collectAsStateWithLifecycle()
    val businessInfoState by contactViewModel.businessInfoState.collectAsStateWithLifecycle()
    val notificationDeviceState by notificationPreferencesViewModel.deviceState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionState, bookingRevision, vehicleRevision, profileRevision) {
        viewModel.refreshForSession()
    }

    LaunchedEffect(sessionState) {
        adminAccessViewModel.refreshForSession()
        notificationPreferencesViewModel.refreshDeviceForSession()
    }

    LaunchedEffect(Unit) {
        contactViewModel.loadBusinessInfo()
    }

    ProfileScreenContent(
        contentPadding = contentPadding,
        sessionState = sessionState,
        adminAccessState = adminAccessState,
        statsState = statsState,
        preferencesState = preferencesState,
        profilePhotoState = profilePhotoState,
        notificationDeviceState = notificationDeviceState,
        businessInfoState = businessInfoState,
        onRequestSignIn = onRequestSignIn,
        onSignOut = viewModel::signOut,
        onRetryStats = viewModel::loadStats,
        onRetryPreferences = viewModel::loadPreferences,
        onRetryPreferenceSave = viewModel::retryPreferenceSave,
        onRetryAdminAccess = { adminAccessViewModel.refreshForSession(force = true) },
        onRetryBusinessInfo = { contactViewModel.loadBusinessInfo(force = true) },
        onProfilePhotoChange = viewModel::updateProfilePhoto,
        onRemoveProfilePhoto = viewModel::removeProfilePhoto,
        onRetryProfilePhoto = viewModel::retryProfilePhotoMutation,
        onDismissProfilePhotoError = viewModel::dismissProfilePhotoError,
        onMarketingOptInChange = viewModel::updateMarketingOptIn,
        onAppointmentReminderOptInChange = viewModel::updateAppointmentReminderOptIn,
        onOpenPersonalData = onOpenPersonalData,
        onOpenNotificationPreferences = onOpenNotificationPreferences,
        onManageVehicles = onManageVehicles,
        onOpenHistory = onOpenHistory,
        onOpenContact = onOpenContact,
        onOpenRewards = onOpenRewards,
        onOpenAdminBookings = onOpenAdminBookings,
        onOpenAdminAvailability = onOpenAdminAvailability,
        onOpenAdminBookingPolicy = onOpenAdminBookingPolicy,
        onOpenAdminLoyaltySettings = onOpenAdminLoyaltySettings,
        onOpenAdminNotificationSettings = onOpenAdminNotificationSettings,
        onOpenAdminNotificationCampaignDrafts = onOpenAdminNotificationCampaignDrafts,
        onOpenAdminBusinessInfo = onOpenAdminBusinessInfo,
        onOpenAdminServiceCatalog = onOpenAdminServiceCatalog,
        onOpenAdminServiceExtras = onOpenAdminServiceExtras,
    )
}

@Composable
private fun ProfileScreenContent(
    contentPadding: PaddingValues,
    sessionState: AuthSessionState,
    adminAccessState: AdminAccessUiState,
    statsState: ProfileStatsUiState,
    preferencesState: ProfilePreferencesUiState,
    profilePhotoState: ProfilePhotoUiState,
    notificationDeviceState: NotificationDeviceUiState,
    businessInfoState: ContactBusinessInfoUiState,
    onRequestSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRetryStats: () -> Unit,
    onRetryPreferences: () -> Unit,
    onRetryPreferenceSave: () -> Unit,
    onRetryAdminAccess: () -> Unit,
    onRetryBusinessInfo: () -> Unit,
    onProfilePhotoChange: (ByteArray, String) -> Unit,
    onRemoveProfilePhoto: () -> Unit,
    onRetryProfilePhoto: () -> Unit,
    onDismissProfilePhotoError: () -> Unit,
    onMarketingOptInChange: (Boolean) -> Unit,
    onAppointmentReminderOptInChange: (Boolean) -> Unit,
    onOpenPersonalData: () -> Unit = {},
    onOpenNotificationPreferences: () -> Unit = {},
    onManageVehicles: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    onOpenRewards: () -> Unit = {},
    onOpenAdminBookings: () -> Unit = {},
    onOpenAdminAvailability: () -> Unit = {},
    onOpenAdminBookingPolicy: () -> Unit = {},
    onOpenAdminLoyaltySettings: () -> Unit = {},
    onOpenAdminNotificationSettings: () -> Unit = {},
    onOpenAdminNotificationCampaignDrafts: () -> Unit = {},
    onOpenAdminBusinessInfo: () -> Unit = {},
    onOpenAdminServiceCatalog: () -> Unit = {},
    onOpenAdminServiceExtras: () -> Unit = {},
) {
    val authenticatedUser = (sessionState as? AuthSessionState.Authenticated)?.session?.user
    val isRestoringSession = sessionState == AuthSessionState.Restoring
    val restoreFailedMessage = (sessionState as? AuthSessionState.RestoreFailed)?.error?.message
    val uriHandler = LocalUriHandler.current
    var pendingCropImage by remember { mutableStateOf<PickedProfileImage?>(null) }
    var showProfilePhotoActions by remember { mutableStateOf(false) }
    var localProfilePhotoError by remember { mutableStateOf<String?>(null) }
    val profilePhotoSaving = profilePhotoState is ProfilePhotoUiState.Saving
    val profileImagePicker = rememberProfileImagePicker(
        onImagePicked = { pickedImage ->
            showProfilePhotoActions = false
            pendingCropImage = pickedImage
        },
        onImagePickFailed = { message ->
            showProfilePhotoActions = false
            localProfilePhotoError = message
        },
    )
    val launchProfileImagePicker = {
        if (!profileImagePicker.launch()) {
            localProfilePhotoError = "Não foi possível abrir a galeria."
        }
    }

    SudsCustomerTheme {
        SudsBrandBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
            ) {
                SudsCompactTopBar(
                    title = "Perfil",
                    eyebrow = "A sua conta",
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 8.dp, bottom = 4.dp),
                )
                if (authenticatedUser != null) {
                    ProfileHeader(
                user = authenticatedUser,
                statsState = statsState,
                preferencesState = preferencesState,
                profilePhotoState = profilePhotoState,
                onRetryStats = onRetryStats,
                onOpenRewards = onOpenRewards,
                onEditPhoto = { showProfilePhotoActions = true },
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
                    NearestLocationCard(
                businessInfoState = businessInfoState,
                onRetryBusinessInfo = onRetryBusinessInfo,
                onOpenMaps = { mapsUri -> uriHandler.openUri(mapsUri) },
            )
            if (authenticatedUser != null) {
                NotificationDevicePromptCard(
                    deviceState = notificationDeviceState,
                    isAdmin = adminAccessState is AdminAccessUiState.Admin,
                    onOpenNotificationPreferences = onOpenNotificationPreferences,
                )
                ProfileMenuCard(
                    onOpenPersonalData = onOpenPersonalData,
                    onOpenNotificationPreferences = onOpenNotificationPreferences,
                    onManageVehicles = onManageVehicles,
                    onOpenHistory = onOpenHistory,
                    onOpenContact = onOpenContact,
                    onOpenRewards = onOpenRewards,
                )
                AdminOperationsCard(
                    adminAccessState = adminAccessState,
                    onRetryAdminAccess = onRetryAdminAccess,
                    onOpenAdminBookings = onOpenAdminBookings,
                    onOpenAdminAvailability = onOpenAdminAvailability,
                    onOpenAdminBookingPolicy = onOpenAdminBookingPolicy,
                    onOpenAdminLoyaltySettings = onOpenAdminLoyaltySettings,
                    onOpenAdminNotificationSettings = onOpenAdminNotificationSettings,
                    onOpenAdminNotificationCampaignDrafts = onOpenAdminNotificationCampaignDrafts,
                    onOpenAdminBusinessInfo = onOpenAdminBusinessInfo,
                    onOpenAdminServiceCatalog = onOpenAdminServiceCatalog,
                    onOpenAdminServiceExtras = onOpenAdminServiceExtras,
                )
                PreferencesCard(
                    preferencesState = preferencesState,
                    onAppointmentReminderOptInChange = onAppointmentReminderOptInChange,
                    onMarketingOptInChange = onMarketingOptInChange,
                    onRetryPreferences = onRetryPreferences,
                    onRetryPreferenceSave = onRetryPreferenceSave,
                )
                LogoutButton(onClick = onSignOut)
            } else if (isRestoringSession) {
                RestoringSessionCard()
            } else if (restoreFailedMessage != null) {
                RestoreFailedSessionCard(
                    message = restoreFailedMessage,
                    onRequestSignIn = onRequestSignIn,
                )
            } else {
                GuestProfileCard(onRequestSignIn = onRequestSignIn)
            }
                    AppVersionText()
                }
            }
        }

        if (showProfilePhotoActions) {
            ProfilePhotoActionsDialog(
            hasPhoto = profilePhotoState.hasPhotoOverride() ?: preferencesState.photoUrlOrBlank().isNotBlank(),
            saving = profilePhotoSaving,
            onChooseFromGallery = launchProfileImagePicker,
            onRemove = {
                showProfilePhotoActions = false
                onRemoveProfilePhoto()
            },
            onDismiss = { if (!profilePhotoSaving) showProfilePhotoActions = false },
            )
        }

        pendingCropImage?.let { sourceImage ->
            ProfileAvatarCropDialog(
            sourceImage = sourceImage,
            onDismissRequest = { pendingCropImage = null },
            onCropApplied = { croppedBytes ->
                pendingCropImage = null
                onProfilePhotoChange(croppedBytes, "image/jpeg")
            },
            onCropFailed = { message ->
                pendingCropImage = null
                localProfilePhotoError = message
            },
            )
        }

        val remotePhotoError = profilePhotoState as? ProfilePhotoUiState.Error
        val photoErrorMessage = localProfilePhotoError ?: remotePhotoError?.message
        if (photoErrorMessage != null) {
            ProfilePhotoErrorDialog(
            message = photoErrorMessage,
            retryable = localProfilePhotoError == null && remotePhotoError?.retryable == true,
            onRetry = onRetryProfilePhoto,
            onDismiss = {
                localProfilePhotoError = null
                onDismissProfilePhotoError()
            },
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    user: AuthUser,
    statsState: ProfileStatsUiState,
    preferencesState: ProfilePreferencesUiState,
    profilePhotoState: ProfilePhotoUiState,
    onRetryStats: () -> Unit,
    onOpenRewards: () -> Unit,
    onEditPhoto: () -> Unit,
) {
    val displayName = preferencesState.displayNameOrNull() ?: user.resolvedDisplayName
    val photoUrl = preferencesState.photoUrlOrBlank()
    val previewImageBytes = profilePhotoState.previewImageBytesOrNull()
    val hidesStoredPhoto = profilePhotoState.hidesStoredPhoto()
    val photoModel: Any? = previewImageBytes ?: photoUrl.takeIf { !hidesStoredPhoto && it.isNotBlank() }
    val photoSaving = profilePhotoState is ProfilePhotoUiState.Saving
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(96.dp)) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (photoModel != null) {
                            AsyncImage(
                                model = photoModel,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = user.initials(displayName),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shadowElevation = 4.dp,
                ) {
                    IconButton(
                        onClick = onEditPhoto,
                        enabled = !photoSaving,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (photoSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Alterar foto de perfil",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SudsColors.onBrand,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SudsColors.onBrandMuted,
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

        ProfileRewardBanner(
            statsState = statsState,
            onOpenRewards = onOpenRewards,
        )
    }
}

@Composable
private fun RestoringProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 20.dp),
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
                color = SudsColors.glass,
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
                    color = SudsColors.onBrand,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Estamos a recuperar a sua conta neste dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SudsColors.onBrandMuted,
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
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 20.dp),
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
                color = SudsColors.glass,
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
                    color = SudsColors.onBrand,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Entre para ver os seus dados e marcações.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SudsColors.onBrandMuted,
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
private fun RestoreFailedSessionCard(
    message: String,
    onRequestSignIn: () -> Unit,
) {
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
                        text = "Não foi possível validar a sessão",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = message,
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
                    text = "Entrar novamente",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun NotificationDevicePromptCard(
    deviceState: NotificationDeviceUiState,
    isAdmin: Boolean,
    onOpenNotificationPreferences: () -> Unit,
) {
    val registeredTokenId = when (deviceState) {
        is NotificationDeviceUiState.Ready -> deviceState.registeredTokenId
        is NotificationDeviceUiState.Success -> deviceState.registeredTokenId
        else -> null
    }
    if (registeredTokenId != null) return
    if (
        deviceState == NotificationDeviceUiState.Checking ||
        deviceState == NotificationDeviceUiState.Registering ||
        deviceState == NotificationDeviceUiState.Unauthenticated ||
        deviceState is NotificationDeviceUiState.Removing ||
        deviceState is NotificationDeviceUiState.Unsupported
    ) {
        return
    }

    val title = when (deviceState) {
        is NotificationDeviceUiState.PermissionRequired -> "Ativar notificações"
        is NotificationDeviceUiState.Error -> "Notificações por ativar"
        else -> "Receber atualizações"
    }
    val body = when {
        deviceState is NotificationDeviceUiState.PermissionRequired -> deviceState.message
        deviceState is NotificationDeviceUiState.Error -> deviceState.message
        isAdmin -> "Ative este dispositivo para receber pedidos de lavagem assim que chegarem."
        else -> "Ative este dispositivo para acompanhar aceitações, recusas e alterações das suas marcações."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ProfileIconContainer(icon = Icons.Filled.Notifications)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.76f),
                )
                TextButton(
                    onClick = onOpenNotificationPreferences,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Text(
                        text = "Gerir notificações",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
        color = SudsColors.glass,
        contentColor = SudsColors.onBrand,
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
                        SudsColors.onBrand
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
                        SudsColors.onBrand
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = SudsColors.onBrandMuted,
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
        color = SudsColors.glass,
        contentColor = SudsColors.onBrand,
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
                color = SudsColors.onBrandMuted,
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

@Composable
private fun ProfileRewardBanner(
    statsState: ProfileStatsUiState,
    onOpenRewards: () -> Unit,
) {
    val stats = (statsState as? ProfileStatsUiState.Loaded)?.stats ?: return
    if (!stats.rewardReady || stats.availableRewards <= 0) return

    Spacer(Modifier.height(12.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.CardGiftcard,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Tens uma lavagem grátis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stats.rewardDescription,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f),
                )
            }
            TextButton(
                onClick = onOpenRewards,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            ) {
                Text(
                    text = "Ver",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProfilePhotoActionsDialog(
    hasPhoto: Boolean,
    saving: Boolean,
    onChooseFromGallery: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Foto de perfil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Escolhe uma imagem da galeria e ajusta o enquadramento antes de guardar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hasPhoto) {
                    OutlinedButton(
                        onClick = onRemove,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Remover foto")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onChooseFromGallery,
                enabled = !saving,
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Escolher da galeria", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !saving,
            ) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun ProfilePhotoErrorDialog(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Não foi possível alterar a foto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = { Text(message) },
        confirmButton = {
            if (retryable) {
                TextButton(onClick = onRetry) {
                    Text("Tentar novamente", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Fechar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = if (retryable) {
            {
                TextButton(onClick = onDismiss) {
                    Text("Fechar")
                }
            }
        } else {
            null
        },
    )
}

private fun ProfilePreferencesUiState.displayNameOrNull(): String? {
    return preferencesOrNull()?.displayName?.takeIf { it.isNotBlank() }
}

private fun ProfilePreferencesUiState.photoUrlOrBlank(): String {
    return preferencesOrNull()?.photoUrl?.trim().orEmpty()
}

private fun ProfilePhotoUiState.previewImageBytesOrNull(): ByteArray? {
    return when (this) {
        is ProfilePhotoUiState.Saving -> previewImageBytes
        is ProfilePhotoUiState.Saved -> previewImageBytes
        ProfilePhotoUiState.Idle,
        is ProfilePhotoUiState.Error -> null
    }
}

private fun ProfilePhotoUiState.hidesStoredPhoto(): Boolean {
    return when (this) {
        is ProfilePhotoUiState.Saving -> hidesStoredPhoto
        is ProfilePhotoUiState.Saved -> hidesStoredPhoto
        ProfilePhotoUiState.Idle,
        is ProfilePhotoUiState.Error -> false
    }
}

private fun ProfilePhotoUiState.hasPhotoOverride(): Boolean? {
    return when (this) {
        is ProfilePhotoUiState.Saving -> if (hidesStoredPhoto) false else previewImageBytes != null
        is ProfilePhotoUiState.Saved -> if (hidesStoredPhoto) false else previewImageBytes != null
        ProfilePhotoUiState.Idle,
        is ProfilePhotoUiState.Error -> null
    }
}

private fun ProfilePreferencesUiState.preferencesOrNull(): ProfilePreferencesUi? {
    return when (this) {
        is ProfilePreferencesUiState.Loaded -> preferences
        is ProfilePreferencesUiState.Saving -> preferences
        is ProfilePreferencesUiState.Saved -> preferences
        is ProfilePreferencesUiState.SaveError -> preferences
        ProfilePreferencesUiState.Idle,
        ProfilePreferencesUiState.Loading,
        ProfilePreferencesUiState.Unauthenticated,
        is ProfilePreferencesUiState.Error -> null
    }
}

private fun AuthUser.initials(profileDisplayName: String = displayName): String {
    val source = profileDisplayName.ifBlank { email.substringBefore("@") }
    return source
        .split(" ", ".", "_", "-", limit = 4)
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercaseChar().toString() }
        .ifBlank { "SS" }
}

@Composable
private fun NearestLocationCard(
    businessInfoState: ContactBusinessInfoUiState,
    onRetryBusinessInfo: () -> Unit,
    onOpenMaps: (String) -> Unit,
) {
    val businessInfo = businessInfoState.infoOrDefault()

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

            NearestLocationStatus(
                state = businessInfoState,
                onRetry = onRetryBusinessInfo,
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
                        text = businessInfo.fullAddressLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onOpenMaps(businessInfo.mapsUri) },
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
                            text = businessInfo.mapPreviewLabel(),
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
private fun NearestLocationStatus(
    state: ContactBusinessInfoUiState,
    onRetry: () -> Unit,
) {
    when (state) {
        ContactBusinessInfoUiState.Idle -> Unit
        ContactBusinessInfoUiState.Loading -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "A atualizar morada e navegação.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is ContactBusinessInfoUiState.Loaded -> Unit
        is ContactBusinessInfoUiState.Error -> Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                if (state.retryable) {
                    TextButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text(
                            text = "Tentar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun ContactBusinessInfoUi.fullAddressLabel(): String {
    return listOf(addressLine1, addressLine2)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n")
}

private fun ContactBusinessInfoUi.mapPreviewLabel(): String {
    return addressLine2
        .substringBefore(",")
        .trim()
        .ifBlank { addressLine1.substringAfterLast(",").trim() }
        .ifBlank { "Leiria" }
}

@Composable
private fun ProfileMenuCard(
    onOpenPersonalData: () -> Unit,
    onOpenNotificationPreferences: () -> Unit,
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
                            ProfileMenuAction.AdminBookings,
                            ProfileMenuAction.AdminAvailability,
                            ProfileMenuAction.AdminBookingPolicy,
                            ProfileMenuAction.AdminLoyaltySettings,
                            ProfileMenuAction.AdminNotificationSettings,
                            ProfileMenuAction.AdminNotificationCampaignDrafts,
                            ProfileMenuAction.AdminBusinessInfo,
                            ProfileMenuAction.AdminServiceCatalog,
                            ProfileMenuAction.AdminServiceExtras -> Unit
                            ProfileMenuAction.PersonalData -> onOpenPersonalData()
                            ProfileMenuAction.NotificationPreferences -> onOpenNotificationPreferences()
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
private fun AdminOperationsCard(
    adminAccessState: AdminAccessUiState,
    onRetryAdminAccess: () -> Unit,
    onOpenAdminBookings: () -> Unit,
    onOpenAdminAvailability: () -> Unit,
    onOpenAdminBookingPolicy: () -> Unit,
    onOpenAdminLoyaltySettings: () -> Unit,
    onOpenAdminNotificationSettings: () -> Unit,
    onOpenAdminNotificationCampaignDrafts: () -> Unit,
    onOpenAdminBusinessInfo: () -> Unit,
    onOpenAdminServiceCatalog: () -> Unit,
    onOpenAdminServiceExtras: () -> Unit,
) {
    when (adminAccessState) {
        is AdminAccessUiState.Admin -> Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column {
                AdminOperationsHeader(
                    state = adminAccessState,
                    onRetryAdminAccess = onRetryAdminAccess,
                )
                adminMenuItems.forEachIndexed { index, item ->
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    ProfileMenuRow(
                        item = item,
                        onClick = {
                            when (item.action) {
                                ProfileMenuAction.AdminBookings -> onOpenAdminBookings()
                                ProfileMenuAction.AdminAvailability -> onOpenAdminAvailability()
                                ProfileMenuAction.AdminBookingPolicy -> onOpenAdminBookingPolicy()
                                ProfileMenuAction.AdminLoyaltySettings -> onOpenAdminLoyaltySettings()
                                ProfileMenuAction.AdminNotificationSettings -> onOpenAdminNotificationSettings()
                                ProfileMenuAction.AdminNotificationCampaignDrafts ->
                                    onOpenAdminNotificationCampaignDrafts()
                                ProfileMenuAction.AdminBusinessInfo -> onOpenAdminBusinessInfo()
                                ProfileMenuAction.AdminServiceCatalog -> onOpenAdminServiceCatalog()
                                ProfileMenuAction.AdminServiceExtras -> onOpenAdminServiceExtras()
                                ProfileMenuAction.None,
                                ProfileMenuAction.PersonalData,
                                ProfileMenuAction.NotificationPreferences,
                                ProfileMenuAction.Vehicles,
                                ProfileMenuAction.Loyalty,
                                ProfileMenuAction.History,
                                ProfileMenuAction.Contact -> Unit
                            }
                        },
                    )
                    if (index == adminMenuItems.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        is AdminAccessUiState.Error -> AdminAccessErrorCard(
            state = adminAccessState,
            onRetryAdminAccess = onRetryAdminAccess,
        )

        AdminAccessUiState.Idle,
        AdminAccessUiState.Loading,
        AdminAccessUiState.NotAdmin -> Unit
    }
}

@Composable
private fun AdminOperationsHeader(
    state: AdminAccessUiState.Admin,
    onRetryAdminAccess: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProfileIconContainer(icon = Icons.Filled.Security)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Administração",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Acesso confirmado para operações protegidas.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${state.roleLabel} - ${state.email}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onRetryAdminAccess,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
        ) {
            Text(
                text = "Verificar",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AdminAccessErrorCard(
    state: AdminAccessUiState.Error,
    onRetryAdminAccess: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Permissões da conta indisponíveis",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (state.retryable) {
                TextButton(
                    onClick = onRetryAdminAccess,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text(
                        text = "Verificar novamente",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
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
private fun PreferencesCard(
    preferencesState: ProfilePreferencesUiState,
    onAppointmentReminderOptInChange: (Boolean) -> Unit,
    onMarketingOptInChange: (Boolean) -> Unit,
    onRetryPreferences: () -> Unit,
    onRetryPreferenceSave: () -> Unit,
) {
    val preferences = preferencesState.preferencesOrNull()
    val appointmentReminderChecked = preferences?.appointmentReminderOptIn ?: false
    val marketingChecked = preferences?.marketingOptIn ?: false
    val saving = preferencesState is ProfilePreferencesUiState.Saving
    val controlsEnabled = preferences != null && !saving

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
                title = "Lembretes de marcação",
                description = "Receber lembretes de marcações",
                checked = appointmentReminderChecked,
                enabled = controlsEnabled,
                loading = saving,
                onCheckedChange = onAppointmentReminderOptInChange,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PreferenceRow(
                title = "Email Marketing",
                description = "Receber ofertas e promoções",
                checked = marketingChecked,
                enabled = controlsEnabled,
                loading = saving,
                onCheckedChange = onMarketingOptInChange,
            )
            ProfilePreferencesStatus(
                preferencesState = preferencesState,
                onRetryPreferences = onRetryPreferences,
                onRetrySave = onRetryPreferenceSave,
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    loading: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
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
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 2.dp,
            )
        } else {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = if (enabled) onCheckedChange else null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                    checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                    checkedBorderColor = MaterialTheme.colorScheme.tertiary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    disabledUncheckedThumbColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledUncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
    }
}

@Composable
private fun ProfilePreferencesStatus(
    preferencesState: ProfilePreferencesUiState,
    onRetryPreferences: () -> Unit,
    onRetrySave: () -> Unit,
) {
    val status = when (preferencesState) {
        ProfilePreferencesUiState.Idle,
        is ProfilePreferencesUiState.Loaded -> return

        ProfilePreferencesUiState.Loading -> PreferenceStatusUi(
            title = "A carregar preferências",
            body = "Estamos a consultar as preferências associadas à conta.",
            loading = true,
            error = false,
            actionLabel = null,
            onAction = null,
        )

        ProfilePreferencesUiState.Unauthenticated -> PreferenceStatusUi(
            title = "Sessão necessária",
            body = "Entre novamente para atualizar preferências.",
            loading = false,
            error = true,
            actionLabel = null,
            onAction = null,
        )

        is ProfilePreferencesUiState.Error -> PreferenceStatusUi(
            title = "Não foi possível carregar preferências",
            body = preferencesState.message,
            loading = false,
            error = true,
            actionLabel = if (preferencesState.retryable) "Tentar novamente" else null,
            onAction = if (preferencesState.retryable) onRetryPreferences else null,
        )

        is ProfilePreferencesUiState.Saving -> PreferenceStatusUi(
            title = "A guardar preferências",
            body = "Estamos a atualizar as suas preferências.",
            loading = true,
            error = false,
            actionLabel = null,
            onAction = null,
        )

        is ProfilePreferencesUiState.Saved -> PreferenceStatusUi(
            title = "Preferências atualizadas",
            body = preferencesState.message,
            loading = false,
            error = false,
            actionLabel = null,
            onAction = null,
        )

        is ProfilePreferencesUiState.SaveError -> PreferenceStatusUi(
            title = "Não foi possível guardar",
            body = preferencesState.message,
            loading = false,
            error = true,
            actionLabel = if (preferencesState.retryable) "Tentar novamente" else null,
            onAction = if (preferencesState.retryable) onRetrySave else null,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (status.error) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        },
        contentColor = if (status.error) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = if (status.error) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    strokeWidth = 2.dp,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = status.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = status.body,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (status.actionLabel != null && status.onAction != null) {
                TextButton(
                    onClick = status.onAction,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (status.error) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    ),
                ) {
                    Text(
                        text = status.actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private data class PreferenceStatusUi(
    val title: String,
    val body: String,
    val loading: Boolean,
    val error: Boolean,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

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
