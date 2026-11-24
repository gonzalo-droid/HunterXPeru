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
val testAdmobRewarded = "ca-app-pub-3940256099942544/5224354917"

// Clave de subida a Play (fuera del repo). Sin ella, el release se firma con la clave de debug:
// sirve para probar en local, pero Play rechaza ese AAB.
val releaseStoreFile = localProps.getProperty("RELEASE_STORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

// Crashlytics (D38). google-services.json sale de la consola de Firebase y no se versiona. Sin él la app
// compila igual, pero Firebase no arranca y no se reportan crashes.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
} else {
    logger.warn("Falta androidApp/google-services.json: el build no tendrá Crashlytics (D38).")
}
// Crash de prueba: solo con -PcrashlyticsTestCrash=true al compilar. Ningún build normal lo incluye.
val crashlyticsTestCrash = providers.gradleProperty("crashlyticsTestCrash").orNull == "true"

android {
    namespace = "pe.cazadores.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // D22. Una vez publicado en Play no se puede cambiar.
        applicationId = "com.nekodojo.peruxhunter"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.app.versionCode.get().toInt()
        versionName = libs.versions.app.versionName.get()
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS") ?: "upload"
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD") ?: localProps.getProperty("RELEASE_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["crashlyticsEnabled"] = false
            resValue("bool", "crashlytics_test_crash", "false")
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            resValue("string", "admob_banner_id", testAdmobBanner)
            resValue("string", "admob_rewarded_id", testAdmobRewarded)
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Con el plugin aplicado, el mapping de R8 se sube solo al terminar assembleRelease/bundleRelease.
            manifestPlaceholders["crashlyticsEnabled"] = true
            resValue("bool", "crashlytics_test_crash", crashlyticsTestCrash.toString())
            manifestPlaceholders["admobAppId"] = localProps.getProperty("ADMOB_APP_ID_ANDROID") ?: testAdmobAppId
            resValue("string", "admob_banner_id", localProps.getProperty("ADMOB_BANNER_ANDROID") ?: testAdmobBanner)
            resValue("string", "admob_rewarded_id", localProps.getProperty("ADMOB_REWARDED_ANDROID") ?: testAdmobRewarded)
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("Release firmado con la clave de debug: falta RELEASE_STORE_FILE en local.properties (Play lo rechazará).")
                signingConfigs.getByName("debug")
            }
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
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
}
