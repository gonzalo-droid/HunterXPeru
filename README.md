# Cazadores Perú

App independiente para Android e iOS que muestra la lista pública del **Programa de Recompensas del
Ministerio del Interior del Perú**: personas requisitoriadas, sus delitos y la recompensa ofrecida. Permite
buscar y filtrar, ver la ficha, guardar fichas para verlas sin conexión y orienta a reportar por la línea
gratuita **0-800-40-007**, sin enfrentarse nunca.

> **App no oficial.** No está afiliada al Ministerio del Interior ni a la PNP. Los datos vienen de la API
> pública que usa recompensas.pe y pueden cambiar sin aviso.

Reemplaza a la versión Flutter (mismo `applicationId`). El análisis de esa versión y el diseño están en
[`docs/kmp-migration/`](docs/kmp-migration/); lo verificado y decidido durante la construcción, en
[`docs/DECISIONES.md`](docs/DECISIONES.md).

## Stack

Kotlin 2.4 · Compose Multiplatform 1.12 (Material 3, UI 100 % compartida) · Ktor 3.6 (OkHttp / Darwin) ·
kotlinx.serialization · Room KMP 2.8 con SQLite empaquetado · DataStore 1.3 · Koin 4.2 · Navigation Compose
(rutas type-safe) · AdMob + UMP (+ ATT en iOS). Tests: kotlin.test, Turbine, Ktor MockEngine, Room en memoria.

Versiones en [`gradle/libs.versions.toml`](gradle/libs.versions.toml). Android: minSdk 24, targetSdk 36,
compileSdk 37. iOS 15+.

## Estructura

```
androidApp/   Punto de entrada Android: Application, MainActivity, manifiesto, ícono, TLS (network_security_config)
composeApp/   Todo el código compartido (biblioteca KMP) y el framework "ComposeApp" para Xcode
  commonMain/kotlin/pe/cazadores/
    core/       config, reglas puras (peligrosidad, formato de recompensa, fotos Base64, fechas), AppResult
    data/       remote (Ktor, DTOs) · local (Room, DataStore) · mapper · repository
    domain/     modelos, interfaces de repositorio, casos de uso
    ui/         theme (tokens "Expediente cívico"), components, navigation, screens/<pantalla>
    ads/        AdsController + AdBanner (implementación por plataforma)
  androidMain/  OkHttp, ConnectivityManager, intents, AdMob/UMP
  iosMain/      Darwin, NWPathMonitor, UIKit, puente de AdMob
iosApp/       Proyecto Xcode (SwiftUI): hospeda MainViewController y AdsBridge.swift (GoogleMobileAds + UMP por SPM, ATT)
```

AGP 9 no permite `com.android.application` y Kotlin Multiplatform en el mismo módulo; por eso `androidApp`
existe aparte.

## Puesta en marcha

Requisitos: JDK 21, Android SDK 37, Xcode 26.

```bash
cp local.properties.example local.properties   # ajusta sdk.dir
cp iosApp/Configuration/Secrets.xcconfig.example iosApp/Configuration/Secrets.xcconfig   # opcional
```

Sin IDs reales de AdMob, la app usa los **IDs de prueba de Google** (debug y release).

### Android

```bash
./gradlew :androidApp:installDebug
```

```bash
./gradlew :androidApp:assembleRelease   # R8 activado; firmado con la clave de debug para probar en local
```

### iOS

Abre `iosApp/iosApp.xcodeproj` en Xcode y ejecuta el esquema `iosApp`, o desde la terminal:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build
```

La fase "Compile Kotlin Framework" llama a `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.

### Tests

```bash
./gradlew :composeApp:testAndroidHostTest        # comunes en la JVM (rápidos)
```

```bash
./gradlew :composeApp:iosSimulatorArm64Test      # comunes + Room real en memoria (iosTest)
```

```bash
./gradlew check                                  # todo, más lint
```

Para un solo test: `./gradlew :composeApp:testAndroidHostTest --tests "pe.cazadores.data.repository.CriminalRepositoryImplTest"`.

## Secretos y configuración

| Qué | Dónde | Versionado |
|---|---|---|
| IDs de AdMob Android (`ADMOB_APP_ID_ANDROID`, `ADMOB_BANNER_ANDROID`) | `local.properties` | No (`.example` sí) |
| IDs de AdMob iOS (`ADMOB_APP_ID_IOS`, `ADMOB_BANNER_IOS`) | `iosApp/Configuration/Secrets.xcconfig` | No (`.example` sí) |
| Teléfono, correos, URLs legales | `composeApp/.../core/config/AppConfig.kt` | Sí |
| Firma de release | Pendiente: hoy usa la clave de debug | — |

## Anuncios y consentimiento

UMP (formulario GDPR) → ATT en iOS → `MobileAds` → recién entonces se piden anuncios. Se dispara al mostrar
Inicio por primera vez. Banner en Inicio y uno cada 8 resultados en Buscar y Guardados; nunca en la Ficha,
Reportar ni el onboarding. En debug se fuerza la geografía EEE para ver el formulario desde Perú. Ajustes
permite reabrir el formulario cuando UMP lo exige.

## Deep links

`cazadores://criminal/{hash}` abre la ficha (Android: intent-filter; iOS: URL scheme + `onOpenURL`).

```bash
adb shell am start -a android.intent.action.VIEW -d "cazadores://criminal/D1321FADB4B9D70B17504891006156FC"
```

```bash
xcrun simctl openurl booted "cazadores://criminal/D1321FADB4B9D70B17504891006156FC"
```

## Ícono de iOS

`iosApp/scripts/AppIcon.swift` dibuja el ícono (mismo diseño que el adaptativo de Android):

```bash
swift iosApp/scripts/AppIcon.swift /tmp/icon.png && sips -z 1024 1024 /tmp/icon.png --out /tmp/icon.png && sips -s format jpeg /tmp/icon.png --out /tmp/icon.jpg && sips -s format png /tmp/icon.jpg --out iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png
```

## Licencias

Fuentes Archivo e IBM Plex Mono bajo SIL Open Font License ([`docs/licenses/`](docs/licenses/)).
