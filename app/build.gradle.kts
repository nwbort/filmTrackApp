import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystoreProps = Properties().also { props ->
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) props.load(propsFile.inputStream())
}

android {
    namespace = "com.filmtrack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.filmtrack.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("project") {
            val storeFilePath = keystoreProps["storeFile"] as? String
            if (storeFilePath != null) {
                storeFile = file("$storeFilePath")
                storePassword = keystoreProps["storePassword"] as? String
                keyAlias = keystoreProps["keyAlias"] as? String
                keyPassword = keystoreProps["keyPassword"] as? String
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            val hasKeystore = (keystoreProps["storeFile"] as? String) != null
            signingConfig = if (hasKeystore) {
                signingConfigs.getByName("project")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("project")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Location
    implementation(libs.play.services.location)

    // Coroutines Play Services
    implementation(libs.kotlinx.coroutines.play.services)

    // Glance Widget
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Coil
    implementation(libs.coil.compose)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // ExifInterface
    implementation(libs.androidx.exifinterface)
}
