pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// Lets Gradle (and Android Studio's daemon JVM check) download a matching JDK when one is missing.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AfterHours"
include(":core")

// The Android app module is only included when an Android SDK is available,
// so the pure-Kotlin game engine (:core) can be built and tested anywhere.
val sdkConfigured = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    file("local.properties").let { it.exists() && it.readText().contains("sdk.dir") }
if (sdkConfigured) include(":app")
