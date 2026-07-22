package com.sudsmobile.data.entitlement

import com.sudsmobile.data.auth.AuthRepository

class FirebaseServiceEntitlementRepository(
    private val api: ServiceEntitlementFunctionsApi,
    private val authRepository: AuthRepository,
) : ServiceEntitlementRepository {
    override suspend fun getMyEntitlements(): ServiceEntitlementListResult {
        val token = authRepository.currentSession()?.idToken
            ?: return ServiceEntitlementListResult.Failure(
                ServiceEntitlementError.Unauthenticated("Inicie sessão para ver os seus planos."),
            )
        return api.getMyEntitlements(token)
    }

    override suspend fun getAdminEntitlements(customerEmail: String): AdminServiceEntitlementListResult {
        val email = customerEmail.trim().lowercase()
        if (!email.looksLikeEmail()) {
            return AdminServiceEntitlementListResult.Failure(
                ServiceEntitlementError.Validation("Indique o email da conta do cliente."),
            )
        }
        val token = authRepository.currentSession()?.idToken
            ?: return AdminServiceEntitlementListResult.Failure(
                ServiceEntitlementError.Unauthenticated("Inicie sessão como administrador."),
            )
        return api.getAdminEntitlements(email, token)
    }

    override suspend fun issueEntitlement(
        request: IssueServiceEntitlementRequest,
    ): ServiceEntitlementMutationResult = withAdminToken { token -> api.issueEntitlement(request, token) }

    override suspend fun adjustUsage(
        request: AdjustServiceEntitlementUsageRequest,
    ): ServiceEntitlementMutationResult = withAdminToken { token -> api.adjustUsage(request, token) }

    override suspend fun revokeEntitlement(
        request: RevokeServiceEntitlementRequest,
    ): ServiceEntitlementMutationResult = withAdminToken { token -> api.revokeEntitlement(request, token) }

    private suspend fun withAdminToken(
        action: suspend (String) -> ServiceEntitlementMutationResult,
    ): ServiceEntitlementMutationResult {
        val token = authRepository.currentSession()?.idToken
            ?: return ServiceEntitlementMutationResult.Failure(
                ServiceEntitlementError.Unauthenticated("Inicie sessão como administrador."),
            )
        return action(token)
    }
}

private fun String.looksLikeEmail(): Boolean =
    isNotBlank() && contains('@') && substringAfter('@').contains('.') && none(Char::isWhitespace)
