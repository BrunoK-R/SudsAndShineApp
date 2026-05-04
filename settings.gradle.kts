rootProject.name = "SudsAndShine"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":data")
include(":di")
include(":feature:auth")
include(":feature:home")
include(":feature:products")
include(":feature:cart")
include(":feature:payment")
include(":feature:profile")
include(":feature:blog")
include(":navigation")
include(":composeApp")
include(":shared")
