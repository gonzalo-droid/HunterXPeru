import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// Versión única de la app, compartida por Android, iOS (Config.xcconfig la repite) y el código común.
val appVersionName: String = libs.versions.app.versionName.get()

// ADS_ENABLED=false en local.properties apaga AdMob (sin UMP, ATT, banners ni bonificado) para tomar capturas de tienda.
// Lo lee también el build de Xcode, porque compila el framework con este mismo Gradle.
val adsEnabled: Boolean = rootProject.file("local.properties").takeIf { it.exists() }
    ?.readLines()?.firstOrNull { it.trim().startsWith("ADS_ENABLED=") }
    ?.substringAfter("=")?.trim()?.toBooleanStrictOrNull() ?: true
if (!adsEnabled) logger.warn("ADS_ENABLED=false: compilando SIN anuncios (solo para capturas, no publicar).")

val generateAppInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/appinfo/kotlin")
    val version = appVersionName
    val ads = adsEnabled
    inputs.property("version", version)
    inputs.property("adsEnabled", ads)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("pe/cazadores/core/config/AppInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package pe.cazadores.core.config
            |
            |/** Generado por Gradle (composeApp/build.gradle.kts). No editar. */
            |object AppInfo {
            |    const val VERSION_NAME: String = "$version"
            |    const val ADS_ENABLED: Boolean = $ads
            |}
            |""".trimMargin(),
        )
    }
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "pe.cazadores.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        androidResources { enable = true }
        withHostTest {}
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppInfo)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.ui.tooling.preview)

                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.navigation.compose)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.androidx.datastore.preferences)

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.play.services.ads)
            implementation(libs.ump)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "pe.cazadores.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Config.xcconfig repite la versión del catálogo para Xcode y ya se desincronizó (4fbc62c): `check` lo detecta.
val checkIosVersion by tasks.registering {
    val xcconfig = rootProject.file("iosApp/Configuration/Config.xcconfig")
    val name = appVersionName
    val code = libs.versions.app.versionCode.get()
    inputs.file(xcconfig)
    doLast {
        val values = xcconfig.readLines().mapNotNull { line ->
            line.substringBefore("//").split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() }
        }.toMap()
        check(values["MARKETING_VERSION"] == name && values["CURRENT_PROJECT_VERSION"] == code) {
            "Config.xcconfig (${values["MARKETING_VERSION"]} / ${values["CURRENT_PROJECT_VERSION"]}) no coincide con " +
                "libs.versions.toml ($name / $code)."
        }
    }
}
tasks.named("check") { dependsOn(checkIosVersion) }

dependencies {
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach {
        add(it, libs.androidx.room.compiler)
    }
    add("androidRuntimeClasspath", libs.compose.ui.tooling)
}
