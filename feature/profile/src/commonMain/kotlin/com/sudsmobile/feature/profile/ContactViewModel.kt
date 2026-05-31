package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.business.BusinessFaq
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.BusinessOpeningHours
import com.sudsmobile.data.business.BusinessSocialLink
import com.sudsmobile.data.business.BusinessStat
import com.sudsmobile.data.business.DefaultBusinessInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ContactBusinessInfoUi(
    val phone: String,
    val phoneUri: String,
    val email: String,
    val emailUri: String,
    val addressLine1: String,
    val addressLine2: String,
    val mapsUri: String,
    val whatsappUri: String,
    val openingHours: List<ContactOpeningHoursUi>,
    val faq: List<ContactFaqUi>,
    val stats: List<ContactStatUi>,
    val socialLinks: List<ContactSocialLinkUi>,
)

internal data class ContactOpeningHoursUi(
    val dayLabel: String,
    val hoursLabel: String,
    val closed: Boolean,
)

internal data class ContactFaqUi(
    val question: String,
    val answer: String,
)

internal data class ContactStatUi(
    val value: String,
    val label: String,
)

internal data class ContactSocialLinkUi(
    val label: String,
    val uri: String,
)

internal sealed interface ContactBusinessInfoUiState {
    data object Idle : ContactBusinessInfoUiState
    data object Loading : ContactBusinessInfoUiState
    data class Loaded(val info: ContactBusinessInfoUi) : ContactBusinessInfoUiState
    data class Error(
        val fallbackInfo: ContactBusinessInfoUi,
        val message: String,
        val retryable: Boolean,
    ) : ContactBusinessInfoUiState
}

internal class ContactViewModel(
    private val businessInfoRepository: BusinessInfoRepository,
) : ViewModel() {
    private val _businessInfoState = MutableStateFlow<ContactBusinessInfoUiState>(ContactBusinessInfoUiState.Idle)
    val businessInfoState: StateFlow<ContactBusinessInfoUiState> = _businessInfoState.asStateFlow()
    private var businessInfoRequestSequence: Long = 0

    fun loadBusinessInfo(force: Boolean = false) {
        if (!force && _businessInfoState.value is ContactBusinessInfoUiState.Loading) return
        if (!force && _businessInfoState.value is ContactBusinessInfoUiState.Loaded) return

        val requestSequence = ++businessInfoRequestSequence
        _businessInfoState.value = ContactBusinessInfoUiState.Loading
        viewModelScope.launch {
            val nextState = when (val result = businessInfoRepository.getBusinessInfo()) {
                is BusinessInfoResult.Success -> ContactBusinessInfoUiState.Loaded(result.info.toContactUi())
                is BusinessInfoResult.Failure -> result.error.toContactErrorState()
            }
            if (requestSequence == businessInfoRequestSequence) {
                _businessInfoState.value = nextState
            }
        }
    }
}

internal fun ContactBusinessInfoUiState.infoOrDefault(): ContactBusinessInfoUi {
    return when (this) {
        ContactBusinessInfoUiState.Idle,
        ContactBusinessInfoUiState.Loading -> DefaultBusinessInfo.toContactUi()
        is ContactBusinessInfoUiState.Loaded -> info
        is ContactBusinessInfoUiState.Error -> fallbackInfo
    }
}

private fun BusinessInfoError.toContactErrorState(): ContactBusinessInfoUiState.Error {
    val retryable = this is BusinessInfoError.Unavailable || this is BusinessInfoError.Backend
    return ContactBusinessInfoUiState.Error(
        fallbackInfo = DefaultBusinessInfo.toContactUi(),
        message = message,
        retryable = retryable,
    )
}

private fun BusinessInfo.toContactUi(): ContactBusinessInfoUi = ContactBusinessInfoUi(
    phone = phone.trim().ifBlank { DefaultBusinessInfo.phone },
    phoneUri = phoneUri.trim().ifBlank { DefaultBusinessInfo.phoneUri },
    email = email.trim().ifBlank { DefaultBusinessInfo.email },
    emailUri = emailUri.trim().ifBlank { DefaultBusinessInfo.emailUri },
    addressLine1 = addressLine1.trim().ifBlank { DefaultBusinessInfo.addressLine1 },
    addressLine2 = addressLine2.trim().ifBlank { DefaultBusinessInfo.addressLine2 },
    mapsUri = mapsUri.trim().ifBlank { DefaultBusinessInfo.mapsUri },
    whatsappUri = whatsappUri.trim().ifBlank { DefaultBusinessInfo.whatsappUri },
    openingHours = openingHours.map { it.toContactUi() }.ifEmpty {
        DefaultBusinessInfo.openingHours.map { it.toContactUi() }
    },
    faq = faq.map { it.toContactUi() }.ifEmpty {
        DefaultBusinessInfo.faq.map { it.toContactUi() }
    },
    stats = stats.map { it.toContactUi() }.ifEmpty {
        DefaultBusinessInfo.stats.map { it.toContactUi() }
    },
    socialLinks = socialLinks.mapNotNull { it.toContactUiOrNull() },
)

private fun BusinessOpeningHours.toContactUi(): ContactOpeningHoursUi = ContactOpeningHoursUi(
    dayLabel = dayLabel.trim(),
    hoursLabel = hoursLabel.trim(),
    closed = closed,
)

private fun BusinessFaq.toContactUi(): ContactFaqUi = ContactFaqUi(
    question = question.trim(),
    answer = answer.trim(),
)

private fun BusinessStat.toContactUi(): ContactStatUi = ContactStatUi(
    value = value.trim(),
    label = label.trim(),
)

private fun BusinessSocialLink.toContactUiOrNull(): ContactSocialLinkUi? {
    val cleanLabel = label.trim()
    val cleanUri = uri.trim()
    if (cleanLabel.isBlank() || cleanUri.isBlank()) return null
    return ContactSocialLinkUi(
        label = cleanLabel,
        uri = cleanUri,
    )
}
