// Plugin versions are declared once here so every module shares one Kotlin plugin.
plugins {
    id("com.android.application") version "8.13.0" apply false
    kotlin("android") version "2.2.20" apply false
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.compose") version "2.2.20" apply false
    kotlin("plugin.serialization") version "2.2.20" apply false
}
