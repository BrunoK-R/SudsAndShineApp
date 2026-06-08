package com.sudsmobile.data.business

data class BusinessInfo(
    val phone: String,
    val phoneUri: String,
    val email: String,
    val emailUri: String,
    val addressLine1: String,
    val addressLine2: String,
    val mapsUri: String,
    val whatsappUri: String,
    val openingHours: List<BusinessOpeningHours>,
    val faq: List<BusinessFaq>,
    val stats: List<BusinessStat>,
    val socialLinks: List<BusinessSocialLink>,
)

data class BusinessOpeningHours(
    val dayLabel: String,
    val hoursLabel: String,
    val closed: Boolean,
)

data class BusinessFaq(
    val question: String,
    val answer: String,
)

data class BusinessStat(
    val value: String,
    val label: String,
)

data class BusinessSocialLink(
    val label: String,
    val uri: String,
)

sealed interface BusinessInfoResult {
    data class Success(val info: BusinessInfo) : BusinessInfoResult
    data class Failure(val error: BusinessInfoError) : BusinessInfoResult
}

sealed interface BusinessInfoError {
    val message: String

    data class Permission(override val message: String) : BusinessInfoError
    data class Unauthenticated(override val message: String) : BusinessInfoError
    data class Unavailable(override val message: String) : BusinessInfoError
    data class Backend(override val message: String) : BusinessInfoError
}

interface BusinessInfoRepository {
    suspend fun getBusinessInfo(): BusinessInfoResult
}

val DefaultBusinessInfo = BusinessInfo(
    phone = "913 005 855",
    phoneUri = "tel:913005855",
    email = "info@sudsshine.pt",
    emailUri = "mailto:info@sudsshine.pt",
    addressLine1 = "Rua Virgílio Vieira da Cunha, R. Pte. das Mestras, 2400-447",
    addressLine2 = "Leiria, Portugal",
    mapsUri = "https://www.google.com/maps/search/?api=1&query=Suds%20%26%20Shine%20Solutions%2C%20Rua%20Virg%C3%ADlio%20Vieira%20da%20Cunha%2C%20R.%20Pte.%20das%20Mestras%2C%202400-447%20Leiria",
    whatsappUri = "https://wa.me/351913005855",
    openingHours = listOf(
        BusinessOpeningHours(dayLabel = "Segunda a Sexta", hoursLabel = "09:00 - 19:00", closed = false),
        BusinessOpeningHours(dayLabel = "Sábado", hoursLabel = "09:00 - 13:00", closed = false),
        BusinessOpeningHours(dayLabel = "Domingo", hoursLabel = "Encerrado", closed = true),
    ),
    faq = listOf(
        BusinessFaq(
            question = "Como posso marcar uma lavagem?",
            answer = "Pode marcar através da app na secção Marcar, escolhendo o serviço, tipo de veículo, data e hora desejados. Também pode ligar para 913 005 855.",
        ),
        BusinessFaq(
            question = "Quanto tempo demora cada serviço?",
            answer = "Lavagem Exterior: 20 min, Lavagem Standard: 30 min, Limpeza Interior: 25 min, Lavagem Premium: 45 min.",
        ),
        BusinessFaq(
            question = "Como funciona o programa de fidelização?",
            answer = "A cada lavagem completa, recebe 1 selo. Quando completar 10 selos, ganha 1 lavagem grátis automaticamente.",
        ),
        BusinessFaq(
            question = "Posso cancelar ou remarcar?",
            answer = "Sim, pode cancelar ou remarcar até 2 horas antes da marcação através da app ou contactando-nos diretamente.",
        ),
        BusinessFaq(
            question = "Aceitam pagamento com cartão?",
            answer = "Sim, aceitamos pagamento em dinheiro, cartão de débito e crédito, e MB Way.",
        ),
        BusinessFaq(
            question = "Onde estão localizados?",
            answer = "Estamos localizados no Shopping NorteSul, Piso -1, na Rua Virgílio Vieira da Cunha, R. Pte. das Mestras, em Leiria. Temos estacionamento gratuito e fácil acesso.",
        ),
    ),
    stats = listOf(
        BusinessStat(value = "500+", label = "Carros Tratados"),
        BusinessStat(value = "4.9", label = "Avaliação Média"),
        BusinessStat(value = "3+", label = "Anos Experiência"),
    ),
    socialLinks = emptyList(),
)
