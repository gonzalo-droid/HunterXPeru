# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Qué es este repo
App KMP (Android + iOS, Compose Multiplatform con UI 100 % compartida) "Cazadores Perú": consume la API pública
del Programa de Recompensas del MININTER. **No oficial**: el aviso debe estar en onboarding, Inicio, Ficha y
Ajustes. Reemplaza a una app Flutter que no vive aquí (su análisis está en `docs/kmp-migration/01-ANALISIS.md`).

## Comandos
- Android debug: `./gradlew :androidApp:installDebug` · release (R8, clave debug): `./gradlew :androidApp:assembleRelease`
- iOS: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'id=<sim>' -derivedDataPath iosApp/build build`
- Tests JVM: `./gradlew :composeApp:testAndroidHostTest` (un test: `--tests "pe.cazadores...Clase"`)
- Tests iOS (incluye Room real): `./gradlew :composeApp:iosSimulatorArm64Test`
- Todo + lint: `./gradlew check`
- Post-clone: `cp local.properties.example local.properties`. Sin IDs de AdMob se usan los de prueba de Google.
- Post-clone: `git config core.hooksPath .githooks` (hook `pre-push` con `./gradlew check`; `--no-verify` lo salta).
- UI E2E: `maestro test .maestro/` sobre un build sin anuncios (ver `.maestro/smoke-guardar.yaml`).

## Estructura que engaña
- `androidApp` es solo el punto de entrada: AGP 9 prohíbe `com.android.application` + KMP en un módulo.
  Todo el código vive en `composeApp` (biblioteca KMP, namespace `pe.cazadores.shared`, Res en `pe.cazadores.resources`).
- `applicationId = com.nekodojo.peruxhunter` (D22; la app Flutter nunca se publicó). Bundle iOS igual (`BUNDLE_ID` en `Config.xcconfig`). Firma de release desde `local.properties`.
- Versión única en `libs.versions.toml` (`app-versionName/Code`); iOS la repite en `Config.xcconfig` y `./gradlew check` falla si no coinciden (`checkIosVersion`). `AppInfo.kt` se genera.

## API (verificado contra producción, ver docs/DECISIONES.md)
- Paginación: petición **1-indexed**, respuesta `number` **0-indexed** → `PageTranslation`. Tiene tests.
- Filtros `sexo` y `alias` **siempre devuelven 0**: no se exponen en la UI.
- Cabeceras honestas (sin Origin/Referer de recompensas.pe). `ApiJson` necesita `encodeDefaults = true` (tipoFilter "F").
- TLS: sin pinning. Android confía en sistema + intermedia Sectigo OV R36 (`res/raw`) solo para el dominio de la API.

## Arquitectura y flujo
- Clean + MVVM: UiState inmutable en StateFlow; ViewModels solo hablan con casos de uso. Koin en `di/Koin.kt`
  (`platformModule` expect/actual provee engine, Room, DataStore, NetworkMonitor, ExternalActions, AdsController).
- Arranque: `AppViewModel` lee DataStore (onboarding + tema) → `AppNavHost`. Deep links pasan por `DeepLinks` (objeto común).
- Datos: primero el padrón semanal de Firebase Storage (`data/padron/PadronStore`, Room + memoria, revisa cada 6 h;
  D21); sin padrón, la API. Fotos del padrón vía `PhotoRepository` + `LocalPhotoRepository` (miniatura → foto grande).
- Offline (ruta API): el repositorio consulta `NetworkMonitor` antes de llamar; sin red va a la caché (página 1, 24 h).
- Anuncios: `ads.start()` en el primer `Home`. iOS implementa `IosAdsBridge` en Swift (`iosApp/iosApp/AdsBridge.swift`).
- Barra de estado: `StatusBarAppearance` usa una pila (la navegación compone la pantalla nueva antes de desechar la vieja).

## Zonas peligrosas
- `androidApp/proguard-rules.pro`: sin la regla de `GeneratedMessageLite` el release se queda en blanco (DataStore + R8).
- `iosApp/iosApp.xcodeproj/project.pbxproj` tiene los paquetes SPM agregados a mano (IDs `C7A1…`).
- `composeApp/schemas/`: esquemas exportados de Room; cambiar entidades exige migración y subir `version`.
- `composeResources/files/catalogs/*.json`: respaldo de catálogos descargado de la API (no editar a mano).

## Convenciones
- Conventional Commits, sin atribución de IA. Textos de UI solo en `composeResources/values/strings.xml`.
- Colores y tipos solo desde `Cz.colors` / `Cz.type`; `ContrastTest` verifica 4.5:1 en ambos temas.
- Filas "texto + acción" con `FlowRow` (letra 2× no debe partir palabras). Etiquetas de pestaña escalan ≤ 1,3×.
- Decisiones nuevas van arriba en `docs/DECISIONES.md`.
- Ramas (D39): `main` solo versiones publicadas (tag `vX.Y.Z`); `release/X.Y.Z` es la candidata y el destino
  de todos los PRs; `feature/` `fix/` `chore/` salen de la release abierta; `hotfix/` sale de `main` hacia una
  `release/X.Y.(Z+1)` nueva. Nunca PR ni commit directo a `main`.
- Emulador local lento: para pruebas de UI esperar elementos (uiautomator) en vez de usar sleeps fijos.

<!-- project-memory: rev=0af7e89 date=2026-11-16 -->
