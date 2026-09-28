package com.sudsmobile.shared.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class SudsAutomotivePhotoTest {

    @Test
    fun serviceKeysResolveToStableProductionPhotography() {
        assertEquals(
            SudsAutomotivePhotoKind.Standard,
            automotivePhotoKindForKey("Lavagem Standard completa"),
        )
        assertEquals(
            SudsAutomotivePhotoKind.Premium,
            automotivePhotoKindForKey("premium-detail"),
        )
        assertEquals(
            SudsAutomotivePhotoKind.Exterior,
            automotivePhotoKindForKey("Lavagem Exterior"),
        )
        assertEquals(
            SudsAutomotivePhotoKind.FabricUpholstery,
            automotivePhotoKindForKey("fabric-upholstery Lavagem de Estofos em Tecido"),
        )
        assertEquals(
            SudsAutomotivePhotoKind.LeatherUpholstery,
            automotivePhotoKindForKey("leather-upholstery Lavagem de Estofos em Pele"),
        )
        assertEquals(
            SudsAutomotivePhotoKind.HeadlightPolish,
            automotivePhotoKindForKey("headlight-polish Polimento de Faróis"),
        )
    }
}
