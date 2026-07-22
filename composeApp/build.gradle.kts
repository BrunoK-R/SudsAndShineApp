import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val useFirebaseEmulators = providers
    .gradleProperty("suds.useFirebaseEmulators")
    .map(String::toBoolean)
    .orElse(false)

val releaseStoreFilePath = providers
    .gradleProperty("SUDS_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("SUDS_RELEASE_STORE_FILE"))
    .orNull
    ?.trim()
    .orEmpty()
val releaseStorePassword = providers
    .gradleProperty("SUDS_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("SUDS_RELEASE_STORE_PASSWORD"))
    .orNull
    .orEmpty()
val releaseKeyAlias = providers
    .gradleProperty("SUDS_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("SUDS_RELEASE_KEY_ALIAS"))
    .orNull
    ?.trim()
    .orEmpty()
val releaseKeyPassword = providers
    .gradleProperty("SUDS_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("SUDS_RELEASE_KEY_PASSWORD"))
    .orNull
    .orEmpty()
val releaseSigningConfigured = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all(String::isNotBlank)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "org.sudsmobile.app.ComposeApp")
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.messaging)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(project(path = ":di"))
            implementation(project(path = ":navigation"))
            implementation(project(path = ":shared"))
            implementation(project(path = ":data"))

            implementation(project(path = ":feature:auth"))
            implementation(project(path = ":feature:home"))
            implementation(project(path = ":feature:onboarding"))
            implementation(project(path = ":feature:products"))
            implementation(project(path = ":feature:cart"))
            implementation(project(path = ":feature:payment"))
            implementation(project(path = ":feature:profile"))
            implementation(project(path = ":feature:blog"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.sudsmobile.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.sudsmobile.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("boolean", "USE_FIREBASE_EMULATORS", useFirebaseEmulators.get().toString())
        manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
        getByName("release") {
            isMinifyEnabled = false
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

val verifyReleaseSigning by tasks.registering {
    group = "verification"
    description = "Prevents an unsigned Android App Bundle from being created for production."
    doLast {
        check(releaseSigningConfigured) {
            "Release signing is not configured. Set SUDS_RELEASE_STORE_FILE, " +
                "SUDS_RELEASE_STORE_PASSWORD, SUDS_RELEASE_KEY_ALIAS, and SUDS_RELEASE_KEY_PASSWORD."
        }
        check(file(releaseStoreFilePath).isFile) {
            "Release keystore does not exist at SUDS_RELEASE_STORE_FILE."
        }
    }
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseSigning)
}
