import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.moko.resources) apply false
}

tasks.register<Exec>("checkNoProductionPrintln") {
    group = "verification"
    description = "Fails when println() is present in production source sets."
    commandLine("bash", "${rootDir}/scripts/check_no_production_println.sh")
}
