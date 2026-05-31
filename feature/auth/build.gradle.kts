import groovy.json.JsonSlurper
import java.util.Properties
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val googleWebClientId = providers
    .provider {
        providers.gradleProperty("GOOGLE_WEB_CLIENT_ID").orNull?.takeIf(String::isNotBlank)
            ?: localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")?.takeIf(String::isNotBlank)
            ?: googleWebClientIdFromGoogleServices()
    }

val resolvedGoogleWebClientId = googleWebClientId.get().ifBlank {
    throw GradleException(
        "Missing GOOGLE_WEB_CLIENT_ID. Set it in local.properties or keep composeApp/google-services.json " +
            "with the Firebase web client OAuth ID.",
    )
}

fun String.asBuildConfigString(): String {
    return "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

fun googleWebClientIdFromGoogleServices(): String {
    val googleServicesFile = rootProject.file("composeApp/google-services.json")
    if (!googleServicesFile.isFile) return ""

    val root = JsonSlurper().parse(googleServicesFile) as? Map<*, *> ?: return ""
    val clients = root["client"] as? List<*> ?: return ""
    return clients.asSequence()
        .mapNotNull { client -> (client as? Map<*, *>)?.get("oauth_client") as? List<*> }
        .flatMap { oauthClients -> oauthClients.asSequence() }
        .mapNotNull { oauthClient -> oauthClient as? Map<*, *> }
        .firstOrNull { oauthClient ->
            (oauthClient["client_type"] as? Number)?.toInt() == 3
        }
        ?.get("client_id") as? String ?: ""
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
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
    ).forEach {
        it.binaries.framework {
            baseName = "auth"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":data"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.compose.navigation)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.sudsmobile.feature.auth"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", resolvedGoogleWebClientId.asBuildConfigString())
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
