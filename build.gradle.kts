plugins {
    id("com.android.application") version "8.13.1" apply false
    kotlin("android") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    //  https://github.com/google/ksp/releases
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
    // https://mvnrepository.com/artifact/com.google.dagger/hilt-android-gradle-plugin
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
}