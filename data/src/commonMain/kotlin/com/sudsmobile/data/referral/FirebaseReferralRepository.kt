package com.sudsmobile.data.referral

import com.sudsmobile.data.auth.AuthRepository

class FirebaseReferralRepository(
    private val api: ReferralFunctionsApi,
    private val authRepository: AuthRepository,
) : ReferralRepository {
    override suspend fun getMyReferral(): ReferralProgramResult {
        val session = authRepository.currentSession()
            ?: return ReferralProgramResult.Failure(
                ReferralError.Unauthenticated("Inicie sessão para ver as suas indicações."),
            )
        return api.getMyReferral(session.idToken)
    }

    override suspend fun claimReferralCode(code: String): ReferralProgramResult {
        val normalizedCode = code.trim().replace(Regex("\\s+"), "").uppercase()
        if (!normalizedCode.matches(Regex("^SUDS-[A-F0-9]{10}$"))) {
            return ReferralProgramResult.Failure(
                ReferralError.Validation("Indique um código no formato SUDS-XXXXXXXXXX."),
            )
        }
        val session = authRepository.currentSession()
            ?: return ReferralProgramResult.Failure(
                ReferralError.Unauthenticated("Inicie sessão para aplicar um código de indicação."),
            )
        return api.claimMyReferralCode(normalizedCode, session.idToken)
    }
}
