package com.sudsmobile.feature.profile

import com.sudsmobile.data.entitlement.AdminPackageTemplate
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminLookupEditorsTest {
    @Test
    fun openingHoursRoundTripPreservesGroupedDaysSplitShiftsAndClosedDays() {
        val source = "Segunda a Sexta | 09:00 - 12:00 / 14:00 - 19:00\nSábado | 09:00 - 13:00\nDomingo | Encerrado | fechado"
        assertEquals(source, serializeAdminHoursRows(parseAdminHoursRows(source)))
        assertEquals(true, parseAdminHoursRows(source).last().closed)
    }

    @Test
    fun socialLinksRoundTripPreservesUrlsIncludingQueryParameters() {
        val source = "Instagram | https://instagram.com/suds?ref=app\nRede personalizada | https://example.com/profile"
        assertEquals(source, serializeAdminSocialRows(parseAdminSocialRows(source)))
    }

    @Test
    fun selectingTemplateCopiesCommercialTermsAndPreservesActualPaymentAndCustomer() {
        val form = AdminServiceEntitlementForm(customerEmail = "client@example.com", amountPaidEuros = "49,90",
            issueNote = "Pago por MB Way", usageReservationCode = "SS-ONE")
        val applied = form.applyTemplate(AdminPackageTemplate("monthly", "Plano mensal", "membership", 4, 30, listOf("exterior")))
        assertEquals("monthly", applied.selectedTemplateId)
        assertEquals("membership", applied.kind)
        assertEquals("4", applied.totalUses)
        assertEquals("30", applied.validDays)
        assertEquals(setOf("exterior"), applied.selectedServiceIds)
        assertEquals(form.customerEmail, applied.customerEmail)
        assertEquals(form.amountPaidEuros, applied.amountPaidEuros)
        assertEquals(form.issueNote, applied.issueNote)
        assertEquals(form.usageReservationCode, applied.usageReservationCode)
    }

    @Test
    fun iconAliasesKeepTheirExistingAppearance() {
        assertEquals("auto_awesome", canonicalAdminIconKey("sparkles"))
        assertEquals("water_drop", canonicalAdminIconKey("exterior"))
        assertEquals("weekend", canonicalAdminIconKey("sofa"))
        assertEquals("shield", canonicalAdminIconKey("wax"))
    }
}
