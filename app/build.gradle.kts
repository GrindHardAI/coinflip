import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing: reads ../keystore.properties if present (see keystore.properties.example).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

// AdMob ad IDs. Debug builds ALWAYS use Google's official test IDs (never click real ads
// while testing). Release builds use admob.properties at the project root when present,
// otherwise the test IDs too (test ads earn nothing but are safe).
val testAdmob = mapOf(
    "APP_ID" to "ca-app-pub-3940256099942544~3347511713",
    "BANNER_ID" to "ca-app-pub-3940256099942544/9214589741",
    "INTERSTITIAL_ID" to "ca-app-pub-3940256099942544/1033173712",
    "REWARDED_ID" to "ca-app-pub-3940256099942544/5224354917",
)
val admobPropsFile = rootProject.file("admob.properties")
val admobProps = Properties().apply {
    if (admobPropsFile.exists()) admobPropsFile.inputStream().use { load(it) }
}
fun releaseAdmob(key: String): String =
    admobProps.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: testAdmob.getValue(key)

android {
    // IMPORTANT: change this to your own unique id before the first Play upload.
    // It can never be changed after publishing.
    namespace = "com.coinflipper.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coinflipper.app"
        minSdk = 26 // Android 8.0+; lets the app use only the vector adaptive icon
        targetSdk = 36
        // Bumped automatically on GitHub Actions (run number); 1 for local builds.
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "1.0.0"
    }

    signingConfigs {
        // CI (GitHub Actions) passes the key through environment variables;
        // local builds use keystore.properties.
        val ciKeystore = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
        if (ciKeystore != null) {
            create("release") {
                storeFile = file(ciKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")?.trim()
                keyAlias = System.getenv("KEY_ALIAS")?.trim()
                // PKCS12 keys share the store password; fall back to it if KEY_PASSWORD is off.
                keyPassword = System.getenv("KEYSTORE_PASSWORD")?.trim()
            }
        } else if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdmob.getValue("APP_ID")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${testAdmob.getValue("BANNER_ID")}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${testAdmob.getValue("INTERSTITIAL_ID")}\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${testAdmob.getValue("REWARDED_ID")}\"")
        }
        release {
            manifestPlaceholders["admobAppId"] = releaseAdmob("APP_ID")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${releaseAdmob("BANNER_ID")}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${releaseAdmob("INTERSTITIAL_ID")}\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${releaseAdmob("REWARDED_ID")}\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.billing)

    testImplementation(libs.junit)
}
