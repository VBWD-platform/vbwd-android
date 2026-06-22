plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.vbwd.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vbwd.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.vbwd:vbwd-android-core:0.1.0")
    // Compiled-in plugins the host bundles (the available-plugins list). A02
    // ships the reference example; further plugins are added per sprint.
    implementation("com.vbwd:vbwd-android-example:1.0.0")
    implementation("com.vbwd:vbwd-android-subscription:1.0.0")
    implementation("com.vbwd:vbwd-android-token-payment:1.0.0")
    implementation("com.vbwd:vbwd-android-stripe:1.0.0")
    implementation("com.vbwd:vbwd-android-invoice:1.0.0")
    implementation("com.vbwd:vbwd-android-cms:0.1.0")
    implementation("com.vbwd:vbwd-android-tarot:0.1.0")
    implementation("com.vbwd:vbwd-android-meinchat:1.1.0")
    implementation("com.vbwd:vbwd-android-meinchat-plus:0.2.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}
