package com.sudsmobile.data.referral

data class ReferralProgram(
    val code: String,
    val shareMessage: String,
    val rewardPoints: Int,
    val attributionDays: Int,
    val referredBy: ReferralAttribution?,
    val stats: ReferralStats,
    val invitations: List<ReferralInvitation>,
    val canClaimCode: Boolean = true,
    val claimIneligibleReason: String? = null,
)

data class ReferralAttribution(
    val code: String,
    val status: String,
    val claimedAtIso: String,
    val qualifiedAtIso: String?,
)

data class ReferralStats(
    val claimedCount: Int,
    val qualifiedCount: Int,
    val pendingCount: Int,
    val bonusPointsEarned: Int,
)

data class ReferralInvitation(
    val id: String,
    val status: String,
    val claimedAtIso: String,
    val qualifiedAtIso: String?,
)

sealed interface ReferralProgramResult {
    data class Success(val program: ReferralProgram) : ReferralProgramResult
    data class Failure(val error: ReferralError) : ReferralProgramResult
}

sealed interface ReferralError {
    val message: String

    data class Validation(override val message: String) : ReferralError
    data class Permission(override val message: String) : ReferralError
    data class Unauthenticated(override val message: String) : ReferralError
    data class NotFound(override val message: String) : ReferralError
    data class AlreadyClaimed(override val message: String) : ReferralError
    data class NotEligible(override val message: String) : ReferralError
    data class Unavailable(override val message: String) : ReferralError
    data class Backend(override val message: String) : ReferralError
}

interface ReferralRepository {
    suspend fun getMyReferral(): ReferralProgramResult
    suspend fun claimReferralCode(code: String): ReferralProgramResult
}

interface ReferralFunctionsApi {
    suspend fun getMyReferral(idToken: String): ReferralProgramResult
    suspend fun claimMyReferralCode(code: String, idToken: String): ReferralProgramResult
}
