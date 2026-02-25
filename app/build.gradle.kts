import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.talosross.summaryyou"
    compileSdk = 36

    // Load signing properties once; missing files yield unsigned builds.
    val playstoreSigningProps = loadSigningProperties(rootProject.file("keystore-playstore.properties"))
    val fossSigningProps = loadSigningProperties(rootProject.file("keystore-foss.properties"))

    defaultConfig {
        applicationId = "com.talosross.summaryyou"
        minSdk = 33
        targetSdk = 36
        versionCode = 2026022515
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("playstore") {
            if (playstoreSigningProps != null) {
                keyAlias = playstoreSigningProps.getProperty("keyAlias")
                keyPassword = playstoreSigningProps.getProperty("keyPassword")
                storeFile = playstoreSigningProps.getProperty("storeFile")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { rootProject.file(it) }
                storePassword = playstoreSigningProps.getProperty("storePassword")
            }
        }
        create("foss") {
            if (fossSigningProps != null) {
                keyAlias = fossSigningProps.getProperty("keyAlias")
                keyPassword = fossSigningProps.getProperty("keyPassword")
                storeFile = fossSigningProps.getProperty("storeFile")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { rootProject.file(it) }
                storePassword = fossSigningProps.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("playstore") {
            dimension = "distribution"
            if (playstoreSigningProps != null) {
                signingConfig = signingConfigs.getByName("playstore")
            }

            // Proxy URL and GCP project number from local.properties (never committed to git)
            val localProps = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localProps.load(FileInputStream(localPropsFile))
            }
            val proxyUrl = localProps.getProperty("proxy.url", "")
            val gcpProjectNumber = localProps.getProperty("gcp.project.number", "")

            buildConfigField("String", "PROXY_URL", "\"$proxyUrl\"")
            buildConfigField("String", "GCP_PROJECT_NUMBER", "\"$gcpProjectNumber\"")
        }
        create("foss") {
            dimension = "distribution"
            applicationIdSuffix = ".foss"
            versionNameSuffix = "-foss"
            if (fossSigningProps != null) {
                signingConfig = signingConfigs.getByName("foss")
            }

            buildConfigField("String", "PROXY_URL", "\"\"")
            buildConfigField("String", "GCP_PROJECT_NUMBER", "\"\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }


    packaging {
        resources {
            excludes += "META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/INDEX.LIST"
            merges += "META-INF/DEPENDENCIES"
        }
    }



    androidResources {
        generateLocaleConfig = true
    }

    lint {
        disable.add("MissingTranslation")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

// Helper to load signing properties; returns null when file is absent.
fun loadSigningProperties(file: java.io.File): Properties? {
    if (!file.exists()) return null
    return Properties().apply {
        FileInputStream(file).use { load(it) }
    }
}

dependencies {
    // https://developer.android.com/develop/ui/compose/bom/bom-mapping
    val composeBomVersion = "2026.01.00"
    val roomVersion = "2.8.4"

    // Core & Lifecycle
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.webkit:webkit:1.15.0")

    // DI (Hilt)
    implementation("com.google.dagger:hilt-android:2.59")
    ksp("com.google.dagger:hilt-compiler:2.59")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    // Keep alpha override for material expressive features, as intended
    // https://developer.android.com/jetpack/androidx/releases/compose-material3#compose_material3_version_15_2
    implementation("androidx.compose.material3:material3:1.5.0-alpha12")

    // Paging
    implementation("androidx.paging:paging-compose:3.3.6")
    implementation("androidx.paging:paging-runtime-ktx:3.3.6")

    // Data Persistence
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.room:room-runtime:${roomVersion}")
    implementation("androidx.room:room-paging:${roomVersion}")
    implementation("androidx.room:room-ktx:${roomVersion}")
    ksp("androidx.room:room-compiler:${roomVersion}")

    // ML & AI
    // Custom configurations for build flavors to manage ML model packaging
    // Bundles model in APK
    "fossImplementation"("com.google.mlkit:text-recognition:16.0.1")
    // Uses Google Play Services
    "playstoreImplementation"("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    // Play Integrity API (playstore flavor only)
    "playstoreImplementation"("com.google.android.play:integrity:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("ai.koog:koog-agents:0.6.3")

    // Networking
    implementation("io.ktor:ktor-client-android:3.4.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.4.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")

    // Serialization & Utilities
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("org.jsoup:jsoup:1.22.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Debug & Tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    // debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
