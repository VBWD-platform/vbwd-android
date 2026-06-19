plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.junit5)
    alias(libs.plugins.roborazzi)
    // Publishes the core AAR to GitHub Packages so the (future) standalone
    // plugin + template repos consume it as `com.vbwd:vbwd-android-core:<ver>`.
    `maven-publish`
}

group = "com.vbwd"
version = "0.1.0"

android {
    namespace = "com.vbwd.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        // A01.3 — :core hosts the shared Compose UI (Login/RootView), the port
        // of the iOS `VBWDCore` `UI/` source set.
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    // Expose a single publishable `release` variant (+ sources) for Maven.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // `api` (not `implementation`): these leak through :core's PUBLIC surface —
    // `ApiClient.request(..., DeserializationStrategy)` and the `StateFlow`
    // returns — so plugin modules must see them transitively.
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)

    // Compose UI + ViewModel (A01.3 shared UI layer).
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // DI — @HiltViewModel for the Login screen logic.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)

    // Roborazzi JVM screenshot tests (Robolectric — no emulator). The vintage
    // engine lets these JUnit4/Robolectric tests run on the JUnit5 platform
    // alongside the unit tests. Record with `-Proborazzi.test.record=true`.
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testRuntimeOnly(libs.junit.vintage.engine)
    debugImplementation(libs.compose.ui.test.manifest)

    // Instrumented tests (device/emulator) — EncryptedTokenStore needs a real
    // Android Keystore; the Compose UI test (LoginScreen) needs a real
    // composition host. Both live in androidTest, run under `connectedCheck`.
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

// GitHub Packages publishing. Run `./gradlew :core:publish` with GITHUB_ACTOR +
// GITHUB_TOKEN (a PAT with write:packages) in the environment — CI sets these.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.vbwd"
            artifactId = "vbwd-android-core"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/dantweb/vbwd-android-core")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
