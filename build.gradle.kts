plugins {
    id("com.android.application") version "8.13.2" apply false
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.android
    kotlin("android") version "2.3.0" apply false
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
    kotlin("plugin.serialization") version "2.3.0" apply false
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.compose
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    //  https://github.com/google/ksp/releases
    id("com.google.devtools.ksp") version "2.3.4" apply false
    // https://mvnrepository.com/artifact/com.google.dagger/hilt-android-gradle-plugin
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
}