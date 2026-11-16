import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Secretos de release (IDs de AdMob). Ver local.properties.example.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// IDs de prueba públicos de Google: se usan en debug y como respaldo si faltan los reales.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBanner = "ca-app-pub-3940256099942544/9214589741"

android {
    namespace = "pe.cazadores.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Se conserva el applicationId de la app Flutter para que esta versión la reemplace en Play.
        applicationId = "com.cazadores.cazadores_peru"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.app.versionCode.get().toInt()
        versionName = libs.versions.app.versionName.get()
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            resValue("string", "admob_banner_id", testAdmobBanner)
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = localProps.getProperty("ADMOB_APP_ID_ANDROID") ?: testAdmobAppId
            resValue("string", "admob_banner_id", localProps.getProperty("ADMOB_BANNER_ANDROID") ?: testAdmobBanner)
            // Firma de debug para probar el build de release en local. Reemplazar antes de publicar.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        resValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
