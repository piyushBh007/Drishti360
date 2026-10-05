import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: ""

val devApiUrl = localProperties.getProperty("DEV_API_URL") ?: System.getenv("DEV_API_URL") ?: "http://10.0.2.2:3000"
// IMPORTANT: PROD_API_URL must be set in local.properties or PROD_API_URL environment variable to your actual Render backend URL.
// Do NOT build a release APK without setting this — "REPLACE-WITH-RENDER-URL" is not a real host.
val prodApiUrl = localProperties.getProperty("PROD_API_URL") ?: System.getenv("PROD_API_URL") ?: "https://REPLACE-WITH-RENDER-URL.onrender.com"

val stunServersConfig = localProperties.getProperty("STUN_SERVERS") ?: System.getenv("STUN_SERVERS") ?: "stun:stun.l.google.com:19302,stun:stun1.l.google.com:19302,stun:stun2.l.google.com:19302"
val turnServerUrl = localProperties.getProperty("TURN_SERVER_URL") ?: System.getenv("TURN_SERVER_URL") ?: ""
val turnServerUser = localProperties.getProperty("TURN_SERVER_USER") ?: System.getenv("TURN_SERVER_USER") ?: ""
val turnServerPass = localProperties.getProperty("TURN_SERVER_PASSWORD") ?: System.getenv("TURN_SERVER_PASSWORD") ?: ""

android {
    namespace = "com.drishti360.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.drishti360.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "STUN_SERVERS", "\"${stunServersConfig}\"")
        buildConfigField("String", "TURN_SERVER_URL", "\"${turnServerUrl}\"")
        buildConfigField("String", "TURN_SERVER_USER", "\"${turnServerUser}\"")
        buildConfigField("String", "TURN_SERVER_PASS", "\"${turnServerPass}\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_URL", "\"${devApiUrl}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_URL", "\"${prodApiUrl}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // WebRTC and WebSocket for Real Inspection Video Calls
    implementation("io.github.webrtc-sdk:android:144.7559.15")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Explicitly add 16KB compatible graphics-path
    implementation("androidx.graphics:graphics-path:1.1.0")
    
    // Play Services Location
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Osmdroid for map
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

gradle.taskGraph.whenReady {
    val isReleaseBuild = allTasks.any { it.name.contains("Release", ignoreCase = true) }
    if (isReleaseBuild) {
        val forbidden = listOf("localhost", "127.0.0.1", "10.0.2.2", "192.168.")
        val isForbidden = forbidden.any { prodApiUrl.contains(it) } || prodApiUrl.startsWith("http://")
        if (isForbidden || prodApiUrl.contains("REPLACE-WITH-RENDER-URL")) {
            throw GradleException(
                "SECURITY ERROR: Release builds must NEVER use local or unencrypted URLs (found: '$prodApiUrl'). " +
                "Configure a valid HTTPS PROD_API_URL in local.properties or PROD_API_URL environment variable."
            )
        }
    }
}

