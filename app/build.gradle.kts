import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val versionProperties = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun readSecret(propertyName: String, envName: String, defaultValue: String = ""): String {
    return (localProperties.getProperty(propertyName)
        ?: System.getenv(envName)
        ?: defaultValue).trim()
}

fun escapeForBuildConfig(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

fun readVersionCode(): Int {
    return versionProperties.getProperty("VERSION_CODE")?.toIntOrNull() ?: 1
}

fun readVersionName(): String {
    return versionProperties.getProperty("VERSION_NAME")?.trim().orEmpty().ifBlank { "1.0.0" }
}

android {
    namespace = "com.example.replybubble"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.replybubble"
        minSdk = 26
        targetSdk = 35
        versionCode = readVersionCode()
        versionName = readVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"${escapeForBuildConfig(readSecret("openrouter.apiKey", "OPENROUTER_API_KEY"))}\"",
        )
        buildConfigField(
            "String",
            "OPENROUTER_MODEL",
            "\"${escapeForBuildConfig(readSecret("openrouter.model", "OPENROUTER_MODEL", "openrouter/auto"))}\"",
        )
        buildConfigField(
            "String",
            "OPENROUTER_REFERER",
            "\"${escapeForBuildConfig(readSecret("openrouter.referer", "OPENROUTER_REFERER", "https://replybubble.local"))}\"",
        )
        buildConfigField(
            "String",
            "OPENROUTER_TITLE",
            "\"${escapeForBuildConfig(readSecret("openrouter.title", "OPENROUTER_TITLE", "ReplyBubble"))}\"",
        )
        buildConfigField(
            "String",
            "UPDATE_FEED_URL",
            "\"${escapeForBuildConfig(readSecret("update.feedUrl", "UPDATE_FEED_URL"))}\"",
        )
        buildConfigField(
            "String",
            "UPDATE_SITE_URL",
            "\"${escapeForBuildConfig(readSecret("update.siteUrl", "UPDATE_SITE_URL"))}\"",
        )
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("com.google.android.material:material:1.12.0")

    implementation("com.google.dagger:hilt-android:2.57.1")
    kapt("com.google.dagger:hilt-android-compiler:2.57.1")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel-compose:1.3.0")

    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
