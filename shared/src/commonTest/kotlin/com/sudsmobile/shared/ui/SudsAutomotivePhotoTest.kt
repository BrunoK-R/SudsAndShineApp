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
    }
}
