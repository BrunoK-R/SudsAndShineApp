package org.sudsmobile.app

import com.sudsmobile.di.initializeKoin
import com.sudsmobile.feature.auth.authFeatureModule
import com.sudsmobile.feature.blog.blogModule
import com.sudsmobile.feature.cart.cartModule
import com.sudsmobile.feature.home.homeModule
import com.sudsmobile.feature.onboarding.onboardingModule
import com.sudsmobile.feature.payment.di.paymentModule
import com.sudsmobile.feature.profile.profileModule
import com.sudsmobile.feature.products.productsModule

fun initializeAndroidApp(isDebugBuild: Boolean) {
    initializeAndroidApp(
        isDebugBuild = isDebugBuild,
        useFirebaseEmulators = false,
    )
}

fun initializeAndroidApp(
    isDebugBuild: Boolean,
    useFirebaseEmulators: Boolean,
) {
    initializeApp(
        isDebugBuild = isDebugBuild,
        useFirebaseEmulators = useFirebaseEmulators,
    )
}

fun initializeIosApp(isDebugBuild: Boolean) {
    initializeApp(
        isDebugBuild = isDebugBuild,
        useFirebaseEmulators = false,
    )
}

private fun initializeApp(
    isDebugBuild: Boolean,
    useFirebaseEmulators: Boolean,
) {
    initializeKoin(
        isDebugBuild = isDebugBuild,
        useFirebaseEmulators = useFirebaseEmulators,
        additionalModules = listOf(
            authFeatureModule,
            homeModule,
            onboardingModule,
            productsModule,
            blogModule,
            cartModule,
            profileModule,
            paymentModule,
        ),
    )
}
