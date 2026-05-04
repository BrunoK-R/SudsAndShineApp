package org.sudsmobile.app

import com.sudsmobile.di.initializeKoin
import com.sudsmobile.feature.blog.blogModule
import com.sudsmobile.feature.cart.cartModule
import com.sudsmobile.feature.home.homeModule
import com.sudsmobile.feature.payment.di.paymentModule
import com.sudsmobile.feature.profile.profileModule
import com.sudsmobile.feature.products.productsModule

fun initializeAndroidApp(isDebugBuild: Boolean) {
    initializeApp(isDebugBuild)
}

fun initializeIosApp(isDebugBuild: Boolean) {
    initializeApp(isDebugBuild)
}

private fun initializeApp(isDebugBuild: Boolean) {
    initializeKoin(
        isDebugBuild = isDebugBuild,
        additionalModules = listOf(
            homeModule,
            productsModule,
            blogModule,
            cartModule,
            profileModule,
            paymentModule,
        ),
    )
}
