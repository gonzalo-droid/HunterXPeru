# Base de conocimiento KMP

Guía para empezar, mantener y **auditar** apps **Kotlin Multiplatform + Compose Multiplatform** (Android +
iOS, UI 100 % compartida). Nació de construir y publicar **Cazadores Perú** (nov. 2026). Las referencias
"(Cazadores Dnn)" apuntan a `docs/DECISIONES.md` de ese repo, donde está la evidencia.

**Versión de la base: 2026-11-25** (166 reglas). Cambiar la fecha en cada cambio de reglas; los informes
de auditoría la citan para saber contra qué versión se compararon.

## Cómo usar este documento

- **Parte 1, siempre aplica.** Cualquier app KMP.
- **Parte 2, si aplica.** Anuncios, crashes, datos públicos, API ajena, pagos, cuentas, notificaciones,
  deep links, actualización obligatoria, analítica, tablets/iPad y apps del estudio Nekodojo.
- **Parte 3, plantilla.** Solo al crear una app: qué archivos copiar de Cazadores. No se audita.
- **Partes 4 y 5, checklists.** Para arrancar y para publicar.
- **Parte 6, mantenimiento.** Cómo evitar que este documento se quede viejo.
- **Parte 7, auditoría.** Cómo comparar una app con esta base y qué entregar.

**IDs:** cada regla lleva un ID estable (`ARQ-03`, `ADS-04`…). No se renumeran: si una regla se elimina, su
ID queda retirado; si se agrega una, toma el número siguiente de su prefijo. Así las auditorías de distintas
apps o fechas se pueden comparar.

**Verificar:** cuando una regla se puede comprobar en el repo, lleva al final cómo (un archivo, un `grep`, una
tarea). Sin "Verificar", se comprueba a mano o en el dispositivo.

Lo propio de cada app (bugs de su API, sus pantallas, su roadmap) **no va aquí**: va en el
`docs/DECISIONES.md` y el `docs/PENDIENTES.md` de esa app.

---

# Parte 1 · Siempre aplica

## 1.1 Stack (verificado en nov. 2026)

Verificar la última versión estable de cada pieza antes de fijarla en `libs.versions.toml`.

| Pieza | Elección |
|---|---|
| Build | Kotlin 2.4, Gradle KTS, **version catalog** |
| UI | Compose Multiplatform 1.12 + Material 3; `composeResources` para strings, fuentes, íconos y JSON |
| Red | Ktor Client (OkHttp / Darwin) + kotlinx.serialization + `HttpRequestRetry`; logging solo en debug |
| BD | Room KMP + `sqlite-bundled`; KSP por target (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`); esquemas exportados |
| Preferencias | DataStore Preferences KMP |
| DI | Koin (BOM) + `koin-compose-viewmodel` |
| Estado | ViewModel KMP (`org.jetbrains.androidx.lifecycle`) + `StateFlow` |
| Navegación | Navigation Compose multiplataforma, rutas type-safe |
| Tests | kotlin.test, Turbine, Ktor MockEngine, coroutines-test; Maestro para flujos E2E |

- **STK-01** Dependencias en el version catalog, sin versiones sueltas en los `build.gradle.kts`.
  Verificar: `grep -rn '"[a-z].*:[0-9]' --include=build.gradle.kts`.
- **STK-02** Las piezas de la tabla, o una desviación anotada en el `DECISIONES.md` de la app.

## 1.2 Estructura de módulos

```
androidApp/   → SOLO punto de entrada Android (Application, Activity, manifest, firma, R8)
composeApp/   → biblioteca KMP con TODO el código (plugin com.android.kotlin.multiplatform.library)
  src/commonMain/kotlin/<pkg>/
    core/            config, AppResult/DataError, formateadores, reglas puras
    data/remote/     Ktor + DTOs
    data/local/      Room (entidades, DAOs), DataStore
    data/repository/ implementaciones + mapeo de errores
    domain/          modelos, interfaces de repositorio, casos de uso
    ui/              theme, components, navigation, screens/<pantalla>/{Screen, ViewModel}
    platform/        interfaces que implementa cada plataforma
    di/Koin.kt       módulos + `expect val platformModule`
  src/androidMain/   actuals
  src/iosMain/       actuals + interfaces que implementa Swift
  src/commonTest/    casi todos los tests
  src/iosTest/       tests que necesitan Room real
  schemas/           esquemas Room exportados (versionados)
iosApp/       → host SwiftUI mínimo + SPM + Config.xcconfig / Secrets.xcconfig
```

- **MOD-01** `androidApp` + `composeApp` + `iosApp`. Por qué: AGP 9 prohíbe `com.android.application` y KMP en
  el mismo módulo. Verificar: `settings.gradle.kts`.
- **MOD-02** Paquetes por capa como arriba (`core`, `data`, `domain`, `ui`, `platform`, `di`).

## 1.3 Arquitectura

- **ARQ-01** Clean + MVVM unidireccional: `UiState` inmutable en un `StateFlow`; la pantalla solo emite eventos.
- **ARQ-02** Ningún ViewModel toca Ktor ni Room: habla con casos de uso, y estos con interfaces de repositorio.
  Verificar: `grep -rln "HttpClient\|Dao\b" ui/`.
- **ARQ-03** Resultado tipado: `AppResult<T>` es `Success(data, cachedAtMillis?)` o `Failure(DataError)`.
  `DataError` es cerrado (`Network`, `Timeout`, `NotFound`, `Server(code)`, `Unknown`) para que la UI explique
  cada caso. Un solo `Throwable.toDataError()` en la capa de datos.
- **ARQ-04** Koin en 4 módulos: `platformModule` (expect/actual), `dataModule`, `domainModule` (factories) y
  `uiModule` (`viewModelOf`).
- **ARQ-05** Un `CoroutineScope` de app con `SupervisorJob`, calificado, para trabajo que sobrevive a la
  pantalla. Nunca `GlobalScope`. Verificar: `grep -rn GlobalScope`.
- **ARQ-06** Arranque: Android llama `initKoin()` en `Application`; iOS llama desde Swift a una función Kotlin
  que recibe los puentes nativos y arranca Koin.
- **ARQ-07** Lo local primero: si un dato puede estar empaquetado o guardado, la UI lo muestra al instante y
  refresca en segundo plano. La UI nunca espera a la red por algo que ya tiene.

## 1.4 Qué se comparte y qué no

**Compartido:** UI, tema, navegación, deep links (un objeto común que traduce URL → ruta), lógica, red,
BD, decodificación de imágenes (`decodeToImageBitmap()`) y dibujo fuera de pantalla (`GraphicsLayer`).

**Por plataforma, siempre detrás de una interfaz en commonMain:**

| Interfaz común | Android | iOS |
|---|---|---|
| `ExternalActions` (tel, mailto, compartir, tienda, navegador) | Intents, `FileProvider` | `UIApplication`, `UIActivityViewController` |
| `NetworkMonitor` | `ConnectivityManager` | `NWPathMonitor` |
| WebView | `WebView` vía `AndroidView` | `WKWebView` vía `UIKitView` |
| Barra de estado | `WindowInsetsController` | pila de apariencias propia |

- **PLT-01** Todo lo nativo detrás de una interfaz en `commonMain`; el código común no conoce Android ni UIKit.
- **PLT-02** Patrón puente para SDKs que solo existen en Swift/SPM (anuncios, Firebase, compras): una
  `interface` en `iosMain`, implementada en Swift y pasada a Koin al arrancar. Sin cinterop ni CocoaPods.
- **PLT-03** Implementación vacía por flag de build: una clave en `local.properties` → Gradle genera
  `AppInfo.kt` → Koin entrega una implementación vacía (p. ej. `NoAdsController`). Sirve a ambas plataformas
  porque Xcode compila el framework con el mismo Gradle.

## 1.5 Reglas de código

- **COD-01** Textos de UI solo en `composeResources/values/strings.xml`. Nada de literales en Composables.
  Verificar: `grep -rn 'Text("' ui/`.
- **COD-02** Colores y tipografía solo desde tokens del tema. Un `ContrastTest` verifica 4.5:1 (texto) y 3:1
  (elementos) en claro y oscuro: el contraste se testea, no se revisa a ojo. Verificar: `ContrastTest.kt`.
- **COD-03** Texto grande: filas "texto + acción" con `FlowRow`; etiquetas de pestaña limitadas a ~1,3×.
  Probar Android 2× e iOS Accesibilidad XL.
- **COD-04** Sin lógica en Composables. Codificar imágenes, armar archivos, formatear o decidir qué mostrar va
  en el ViewModel o en un caso de uso, donde se puede testear. Señal de alerta: `withContext`, `runCatching` o
  `LaunchedEffect` con trabajo pesado dentro de una pantalla. Verificar:
  `grep -rn "withContext\|runCatching" --include="*Screen.kt" ui/screens` (las funciones puras del mismo paquete no
  cuentan).
- **COD-05** Pantallas chicas: el límite es **por archivo**. Si un `Screen.kt` pasa de ~300 líneas, sus secciones
  (`HeaderSection`, `ResultsList`…) van a archivos propios del mismo paquete. Secciones privadas en el mismo archivo
  cuentan como ⚠️. Cada sección recibe estado y lambdas, nunca el ViewModel.
  Verificar: `wc -l ui/screens/*/*Screen.kt`.
- **COD-06** Sin `!!` y sin `GlobalScope`. Verificar: `grep -rn '!!' src/*Main`.
- **COD-07** Conventional Commits, sin atribución de IA. Un commit por paso lógico. Verificar: `git log`.

## 1.6 Una sola fuente de versión

- **VER-01** La versión vive en `libs.versions.toml` (`app-versionName`, `app-versionCode`) y es la misma en
  Android e iOS.
- **VER-02** `check` falla si `Config.xcconfig` (`MARKETING_VERSION`, `CURRENT_PROJECT_VERSION`) no coincide
  con el catálogo. En Cazadores se desincronizó dos veces. Verificar: `./gradlew checkIosVersion`.
- **VER-03** El número de build de iOS sale del xcconfig, no está repetido en `project.pbxproj`.
  Verificar: `grep CURRENT_PROJECT_VERSION iosApp/*.xcodeproj/project.pbxproj` no devuelve nada.

```kotlin
// composeApp/build.gradle.kts
val checkIosVersion by tasks.registering {
    val xcconfig = rootProject.file("iosApp/Configuration/Config.xcconfig")
    val name = libs.versions.app.versionName.get()
    val code = libs.versions.app.versionCode.get()
    inputs.file(xcconfig)
    doLast {
        // Compara clave por clave: buscar "MARKETING_VERSION=2.0.2" en el texto aceptaría "2.0.25".
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
```

Otra opción: que Gradle **genere** un `Version.xcconfig` que `Config.xcconfig` incluya. Así no hay copia.
`AppInfo.kt` (versión para el código común) también se genera desde el catálogo.

## 1.7 Secretos, llaves y archivos fuera de git

- **SEC-01** `local.properties` (Android) y `Secrets.xcconfig` (iOS) fuera de git, con sus `.example`
  versionados. Verificar: `.gitignore` y `git ls-files | grep -i "secret\|local.properties"`.
- **SEC-02** Sin secretos, el build **sigue funcionando** con valores de prueba y **avisa** con un warning de
  Gradle/Xcode.
- **SEC-03** La keystore de subida va fuera del repo (p. ej. `~/.config/<app>-signing/`) y Play App Signing
  está activado.
- **SEC-04** Respaldo de llaves y credenciales anotado (dónde está, no la llave): keystore de subida y su
  contraseña, clave `.p8` de App Store Connect, archivos de Firebase, IDs de AdMob. Perder la keystore sin
  Play App Signing impide actualizar la app.
- **SEC-05** Material de marketing (reels, videos, capturas de tienda generadas) fuera de git desde el día 1:
  pesa decenas de MB y se regenera. Los guiones o el contexto que las generan (`.md`, `.json`) sí pueden
  versionarse. Verificar: `.gitignore` incluye `docs/capturas/` y las carpetas de salida.

## 1.8 Testing

**Unitarios (en `commonTest`, corren en JVM con `testAndroidHostTest`):**
- **TST-01** Reglas puras, formatos y normalización de texto con tildes.
- **TST-02** Mapeo DTO → dominio y cualquier traducción con la API (paginación, ids).
- **TST-03** Cliente de API con **Ktor MockEngine**: éxito, 404, timeout, sin red → caché.
- **TST-04** Repositorios con **fakes a mano** (`testing/Fakes.kt`) en vez de librerías de mocks.
- **TST-05** ViewModels con **Turbine** + `StandardTestDispatcher` + `Dispatchers.setMain`; el trabajo de fondo
  usa el `backgroundScope` del test.
- **TST-06** Deep links, contraste del tema y política de enlaces del WebView.

**Base de datos:**
- **TST-07** Room y migraciones en `iosTest` (`iosSimulatorArm64Test`): la JVM host no tiene driver de SQLite.
  Cada migración queda probada: un test por migración, o uno que parta de la primera versión publicada y verifique
  el cambio de cada versión. Verificar: cada versión de `schemas/` tiene una aserción en algún test.

**Flujos de UI, con Maestro:**
- **TST-08** Al menos un smoke test del flujo principal en `.maestro/`, y uno por bug de navegación arreglado.
- Flujos en YAML que corren igual en el emulador Android y en el simulador iOS. La CLI es gratis y corre en
  local, lo que encaja con repos privados (Maestro Cloud es de pago y no hace falta).
- Encuentra elementos por texto visible o por descripción de accesibilidad. Para usar `testTag` en Android,
  activar `Modifier.semantics { testTagsAsResourceId = true }` en la raíz de la app. En iOS, comprobar si la
  versión de CMP expone `testTag` como `accessibilityIdentifier`; si no, usar texto.
- Espera elementos sola (sin sleeps fijos). `takeScreenshot` sirve también para las capturas de tienda.
- Lecciones de Cazadores (`.maestro/smoke-guardar.yaml`, probado en el emulador Android):
  - **Anuncios fuera:** correr sobre un build sin anuncios (ADS-06). En debug el formulario de UMP (geografía
    EEE forzada) tapa la primera pantalla y el flujo no lo maneja.
  - **Espacios invisibles:** los montos formateados usan espacio de no separación (`S/ 500 000`). Un patrón
    `"S/ .*"` no encuentra nada; usar `"S/.*"`. Si un texto "visible" no se encuentra, sospechar de esto.
  - **Primera apertura lenta:** tras `clearState` el emulador puede tardar más de un minuto. Usar
    `extendedWaitUntil` con `timeout: 120000` en la primera pantalla y en la primera carga de datos.
  - **Diálogos del sistema:** con el emulador cargado aparece "System UI isn't responding". Cerrarlo con un
    `runFlow` condicional (`when: visible: ".*isn't responding"` → `tapOn: "Wait"`).
  - **Firma distinta:** si el emulador tiene la app instalada desde otra máquina o la tienda, no se puede
    instalar el debug (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). Hay que desinstalarla y se pierden sus datos.
  - **Migraciones:** tras correr un build con Room más nuevo, instalar uno más viejo encima falla al abrir.
    Borrar los datos de la app antes.
- Alternativa sin dispositivo: `runComposeUiTest` (compose-ui-test multiplataforma) para componentes sueltos.
  Cubre menos (no prueba el host nativo) y en JVM puede pedir configuración extra.

**Verificación manual:**
- **TST-09** Antes de cada release: release (R8) en Android y Release en iOS, en claro y oscuro, letra grande
  y modo avión.

## 1.9 CI con poco consumo (repos privados)

En repos privados GitHub Actions descuenta minutos del plan, y los runners de macOS cuentan varias veces más
que los de Linux. Revisar el plan vigente antes de decidir.

- **CI-01** Como mínimo, un hook `pre-push` local que corre `./gradlew check`. Verificar: `.githooks/pre-push`
  y `git config core.hooksPath`.
- **CI-02** Si hay CI en la nube: solo Linux, solo en PRs a `release/**` y en el push a `main` (ver GIT-01),
  con caché de Gradle, `concurrency: cancel-in-progress` y `paths-ignore` para `docs/**`. Los tests de iOS y
  Maestro se corren en local antes de cada release.

De menor a mayor costo:
1. **Hook `pre-push` local (gratis, recomendado para empezar):**
   ```bash
   mkdir -p .githooks && printf '#!/bin/sh\n./gradlew check --quiet\n' > .githooks/pre-push && chmod +x .githooks/pre-push
   ```
   ```bash
   git config core.hooksPath .githooks
   ```
   Cada clon nuevo debe repetir el `git config` (anotarlo en el README). `git push --no-verify` lo salta en una
   emergencia.
2. **CI en Linux** con las condiciones de CI-02.
3. **Runner propio en tu Mac** para iOS, si algún día hace falta. Revisar si GitHub cobra por uso de runners
   propios en el plan vigente.

## 1.10 Errores y observabilidad

- **OBS-01** Si la app registra errores no fatales o mensajes desde el código común, hacerlo por una interfaz
  `CrashReporter` (`log(message)`, `recordNonFatal(throwable)`) en `platformModule`, con implementación vacía en
  debug. Así, cambiar de servicio no toca el código común. Si solo se reportan crashes fatales (CRA-05), el SDK
  basta y la regla es ➖.
- **OBS-02** No reportar errores de red esperados (sin conexión, timeouts) si la app ya tiene respaldo: es ruido.
- **OBS-03** En iOS, `setUnhandledExceptionHook` con `NSLog` de la traza antes del abort. Sin eso, el error de
  Kotlin no queda en el log del sistema. Verificar: `grep -rn setUnhandledExceptionHook iosMain`.
- **OBS-04** Nada de datos personales en logs ni en reportes de crash (nombres, correos, ubicación, ids de
  cuenta). El logging de Ktor solo en debug.

## 1.11 Internacionalización

- **I18N-01** Todos los textos en `strings.xml` (ver COD-01), con plurales y formatos de `composeResources`.
  Agregar `values-en/strings.xml` después cuesta poco; buscar literales repartidos cuesta mucho.
- **I18N-02** Fechas y montos con formateadores propios en `core/`, testeados.

## 1.12 Presupuesto de rendimiento

- **PERF-01** Medir y anotar en cada release (en el `DECISIONES.md` de la app):
  - Tamaño del AAB/APK de release y del IPA (el framework de Kotlin pesa; vigilar que no crezca de golpe).
  - Arranque en frío hasta la primera pantalla útil, en un Android de gama baja.
  - Datos que baja la app en la primera apertura (importa con datos móviles caros).

## 1.13 Trampas de KMP/CMP ya pagadas

| ID | Problema | Causa | Arreglo |
|---|---|---|---|
| **TRP-01** | Release Android en blanco | R8 borra campos de `GeneratedMessageLite` que DataStore lee por reflexión | Regla keep en `proguard-rules.pro`. **Probar siempre el release.** |
| **TRP-02** | Hojas inferiores que oscilan en iOS | `ModalBottomSheet` (M3 1.9) suma relleno de la barra de estado en bucle | Relleno fijo arriba, insets solo abajo, `OnFocusBehavior.DoNothing` |
| **TRP-03** | Pestaña equivocada al volver | `popUpTo(startDestination)` apuntaba a una pantalla ya fuera de la pila (onboarding) | `popUpTo<Route.Home>` |
| **TRP-04** | Enums en rutas fallan en iOS | Navigation type-safe no los soporta en iOS | Pasar el nombre como `String` |
| **TRP-05** | Barra de estado equivocada al navegar | La pantalla nueva se compone antes de desechar la vieja | Pila de apariencias |
| **TRP-06** | "Guardar imagen" cierra la app iOS | Falta `NSPhotoLibraryAddUsageDescription` | Declararla si se comparte una imagen |
| **TRP-07** | Gmail ignora el destinatario | `mailto:` con `%40` | Destinatario sin codificar + `EXTRA_EMAIL` |
| **TRP-08** | Let's Encrypt falla en Android 7 | minSdk 24 no confía en ISRG Root X1 | Botón "Abrir en el navegador" de respaldo |
| **TRP-09** | Paquetes SPM | Se agregan a mano en `project.pbxproj` | Documentar los IDs en el `CLAUDE.md` de la app |
| **TRP-10** | Cambiar entidades Room | Rompe instalaciones existentes | Subir `version` + migración + test en `iosTest` + instalar encima de la versión publicada (PUB-04) |

En una auditoría, cada trampa se marca ✅ si la app está protegida, ➖ si no usa esa pieza.

## 1.14 Limitaciones de KMP/CMP (informativo, no se audita)

- SDKs de iOS sin binding Kotlin cómodo → patrón puente Swift (PLT-02).
- Trazas de Kotlin en iOS pobres en los reportes de crash (framework estático).
- Room no corre en los tests JVM host: los de BD van al simulador, que es más lento.
- Material 3 multiplataforma va detrás del de Android, con bugs propios en iOS (hojas, foco, teclado).
- WebView, mapas, cámara y share sheet siempre necesitan `expect/actual` o `UIKitView`/`AndroidView`.

## 1.15 Ramas y versiones

- **GIT-01** `main` = solo versiones publicadas, con tag `vX.Y.Z` en cada merge. Sin PRs directos.
  Verificar: `git tag` y que cada merge a `main` venga de una `release/…`.
- **GIT-02** `release/X.Y.Z` = candidata a tienda, creada desde `main`. El primer commit sube la versión; todos
  los PRs apuntan aquí; de aquí salen TestFlight y la prueba interna de Play. Una sola release abierta a la vez.
  Al cerrarla: merge normal (sin squash) a `main`, tag y subida a las tiendas.
- **GIT-03** `feature/…`, `fix/…`, `chore/…` = ramas de trabajo desde la release abierta, con PR a ella.
- **GIT-04** `hotfix/…` = desde `main` hacia una `release/X.Y.(Z+1)` nueva. Si había otra release abierta,
  merge de `main` en ella al cerrar el hotfix.
- **GIT-05** Arreglo de una sola plataforma: la versión sube igual (VER-01), se publica solo donde hace falta y
  la otra plataforma se salta ese número (anotarlo en el changelog).
- **GIT-06** Sacar algo que ya entró en la release: `git revert -m 1 <merge>` en una rama `chore/…` con PR a la
  release. Para retomarlo después, volver a unir la rama no trae nada (git ya la considera unida): hay que
  revertir el commit de reversión. Anotarlo en `PENDIENTES.md`.
- **GIT-07** Una función sin vuelta atrás (p. ej. declarar iPad: App Store no deja retirarlo) va en su propia
  release, con su prueba manual y sus capturas, no mezclada con arreglos.
- **GIT-08** Borrar las ramas de trabajo al unirlas; conservar solo `main`, la release abierta y las que guardan
  trabajo revertido o sin terminar. Verificar: `git branch -a --merged`.

## 1.16 Ajustes estándar

- **AJU-01** Ajustes trae estas filas, con textos en `strings.xml`. El agrupamiento es una recomendación: la app
  puede tener secciones propias, y "Compartir" y "Valorar" pueden vivir en una sub-pantalla "Sobre la app"
  enlazada desde Ajustes.
  - **Privacidad:** "Preferencias de anuncios" (solo con anuncios, ADS-05), "Política de privacidad" y
    "Términos y condiciones".
  - **Comunidad:** "Compartir la app" y "Valorar la app".
  - **Ayuda:** "Contacto / reportar un problema" y "Licencias de código abierto".
  - **Pie:** la versión visible (`AppInfo.VERSION_NAME`), útil para soporte y para cruzar con los crashes.
- **AJU-02** "Contacto" abre un correo con la versión, el modelo y el sistema ya escritos.
- **AJU-03** Compartir la app: mensaje de invitación amable, corto y sin presión (qué hace la app y por qué le
  sirve a quien lo recibe), por la hoja de compartir del sistema (`ExternalActions.share`), nunca a una app
  concreta.
- **AJU-04** El mensaje lleva **un solo enlace de descarga que redirige desde el servidor** (en apps de
  Nekodojo: NEK-02). El servidor mira el `User-Agent` y responde un 302: Android → Play, iPhone → App Store,
  computadora u otro → la landing; si la app aún no está en una tienda, a la landing.
  - **Por qué:** el texto queda grabado en cada versión instalada y se sigue mandando por años. Con el enlace
    del sitio, a dónde lleva se decide en el servidor: publicar en Play, cambiar una ficha o sumar una tienda no
    exige una versión nueva. Además, un enlace en vez de dos, vista previa con la imagen de la landing y clics
    medibles.
  - **Landing ≠ descarga:** la landing es para leer qué es la app y no redirige; va en la web, en redes y en
    las fichas. El enlace de descarga va solo en el mensaje de compartir.
  - **Riesgo:** si el dominio vence o el sitio se cae, todos los enlaces compartidos fallan. Sin sitio propio,
    compartir los dos enlaces directos de las tiendas, y nunca el de una tienda donde la app aún no está.
  - Los iPad con iPadOS 13+ se presentan como Mac y caen en la landing, que ya tiene el botón del App Store.
- **AJU-05** Valorar la app: el botón abre la ficha para escribir la reseña. Android `market://details?id=<pkg>`
  (si falla, la web de Play); iOS `itms-apps://apps.apple.com/app/id<ID>?action=write-review`.
- **AJU-06** El aviso nativo de reseña (Play In-App Review, `SKStoreReviewController`), si se usa, solo tras un
  momento bueno (p. ej. el tercer guardado), nunca tras un error ni en el arranque. El sistema decide si lo
  muestra.
- **AJU-07** Prohibido por las tiendas: preguntar antes "¿te gusta?" y mandar solo a los contentos a la tienda, o
  dar premios por valorar.

## 1.17 Enlaces: dónde se abre cada uno

| Enlace | Dónde | Por qué |
|---|---|---|
| Páginas propias (privacidad, términos, ayuda) | WebView dentro de la app | Mantiene el foco; el contenido es tuyo y se controla |
| Sitios ajenos (fuentes, noticias, redes) | Navegador in-app: Custom Tabs (Android), `SFSafariViewController` (iOS) | No sale de la app, muestra la URL real, usa las sesiones y el autocompletado del navegador y no expone la app a contenido ajeno. Google y Apple lo recomiendan en vez de WebView para terceros |
| Tiendas, `mailto:`, `tel:`, mapas | App del sistema | Es lo que el usuario espera |

- **LNK-01** Cada tipo de enlace se abre donde dice la tabla.
- **LNK-02** Reglas del WebView: sin JavaScript si las páginas son HTML estático; la URL sale de la
  configuración (un deep link no puede cargar otra); solo el host propio se queda dentro y el resto sale por
  `ExternalActions`; HTTP ≥ 400 cuenta como fallo, con "Reintentar" y "Abrir en el navegador".

## 1.18 Requisitos anuales de las tiendas

- **TIE-01** iOS: compilar con el SDK que Apple exige ese año (desde abril de 2027, el de iOS 27 / Xcode 27).
- **TIE-02** Android: `targetSdk` dentro del plazo de Play (cada año, hacia fines de agosto, las
  actualizaciones deben apuntar al nivel de API del año anterior o superior). Verificar: `targetSdk` en
  `androidApp/build.gradle.kts`.
- **TIE-03** Desde `targetSdk 35`, Android dibuja de borde a borde por obligación: cada pantalla maneja los
  insets de barras de estado y navegación. Probar con navegación por gestos y con 3 botones.
- **TIE-04** Con `targetSdk 36`, Android 16 ignora el bloqueo de orientación y de tamaño en pantallas de 600 dp
  o más (tablets, plegables abiertos). Aunque la app sea "solo teléfono", debe verse bien en horizontal y en
  ancho grande, o al menos sin cortes. Ver TAB-01.

## 1.19 Accesibilidad

- **A11Y-01** Contraste y letra grande: ver COD-02 y COD-03.
- **A11Y-02** Cada ícono o imagen con significado lleva `contentDescription` (en `strings.xml`); los
  decorativos, `null` o `clearAndSetSemantics {}`.
- **A11Y-03** Zonas táctiles de al menos 48 dp en Android y 44 pt en iOS (Material lo cumple por defecto;
  revisar los íconos sueltos y las filas propias).
- **A11Y-04** Una pasada con TalkBack y otra con VoiceOver por el flujo principal antes de cada release: orden
  de lectura, encabezados (`semantics { heading() }`) y botones con nombre.

## 1.20 Estados de pantalla y permisos

- **UIS-01** Cada pantalla con datos tiene los cuatro estados: cargando (esqueleto, no spinner suelto), vacío
  (qué hacer), error (qué pasó + "Reintentar") y sin conexión (datos guardados + fecha). Componentes comunes en
  `ui/components/StateViews.kt`.
- **UIS-02** Los mensajes de error salen de `DataError` (ARQ-03), nunca el texto de la excepción.
- **PERM-01** Los permisos se piden en el momento en que se usan, con una explicación antes, nunca todos al
  arrancar.
- **PERM-02** Si el usuario niega un permiso, la app sigue funcionando sin esa función y explica cómo activarlo.
- **PERM-03** Cada permiso de iOS con su clave de uso en `Info.plist` y un texto que explique para qué.

## 1.21 Privacidad técnica

- **PRIV-01** Datos que el usuario guarda en el dispositivo: decidir si entran o no en el respaldo de Android
  (`android:dataExtractionRules` / `fullBackupContent`). Por defecto Android respalda la base de datos.
- **PRIV-02** `PrivacyInfo.xcprivacy` de la app con las razones de las APIs que las exigen (UserDefaults,
  marcas de tiempo de archivos, espacio en disco, tiempo de arranque…) y revisar que los SDKs traigan el suyo.
- **PRIV-03** La política de privacidad, Data safety de Play y la privacidad de App Store dicen lo mismo y
  cubren todo lo que recolectan los SDKs. Revisarlas cada vez que se suma un SDK.

## 1.22 Ficha de tienda y documentación del repo

- **DOC-01** `CLAUDE.md` (comandos, estructura que engaña, zonas peligrosas), `docs/DECISIONES.md` y
  `docs/PENDIENTES.md` al día y **versionados en git** (`CLAUDE.md` no lleva secretos). Skill: `project-memory`.
  Verificar: `git ls-files CLAUDE.md docs/DECISIONES.md docs/PENDIENTES.md`.
- **DOC-02** `CHANGELOG.md` con una entrada por versión, de la que salen las notas de las tiendas. Skill:
  `changelog-update`.
- **DOC-03** README con capturas reales, stack y versión. Skill: `readme-format`.
- **DOC-04** Capturas de tienda en los tamaños de cada tienda, sin anuncios (ADS-06). Skill: `store-screenshots`.

---

# Parte 2 · Si aplica

Cada sección empieza con la pregunta que decide si aplica. Si la respuesta es "no", toda la sección es ➖.

## 2.1 Anuncios (AdMob) · ¿La app muestra anuncios?

- **ADS-01** Orden obligatorio: **UMP → ATT (iOS) → `MobileAds.initialize` → anuncios**, en la primera pantalla
  útil tras el onboarding, nunca en el arranque.
- **ADS-02** `AdBanner` no ocupa espacio ni pide nada mientras `canRequestAds == false`.
- **ADS-03** Debug: IDs de prueba de Google y geografía EEE forzada. Release: IDs reales desde los secretos.
- **ADS-04** Ninguna acción depende de un anuncio: si un bonificado falla o tarda más de ~4 s, se hace la acción
  igual.
- **ADS-05** Ajustes muestra "Preferencias de anuncios" (formulario de UMP) cuando UMP lo exige.
- **ADS-06** Apagar los anuncios para capturas y pruebas, en dos niveles:
  - Compilación: `ADS_ENABLED=false` en `local.properties` → Koin entrega `NoAdsController`. Gradle avisa al
    compilar así.
  - En ejecución, **solo en debug**: un argumento de arranque (`disableAds=true`, que Maestro, `adb` y Xcode
    pueden pasar) o un interruptor oculto en Ajustes (p. ej. 7 toques en la versión). En release ambos se
    ignoran: nunca se publica sin anuncios por error.
- **ADS-07** `app-ads.txt` publicado en el dominio del desarrollador.
- Skills: `add-admob` y `admob-compliance`.

## 2.2 Reporte de crashes (Crashlytics) · ¿La app reporta crashes?

- **CRA-01** Android: firebase-bom + `firebase-crashlytics` en `androidApp`. Los plugins `google-services` y
  `firebase.crashlytics` se aplican **solo si existe `google-services.json`**; si no, warning y compila.
- **CRA-02** `-keepattributes SourceFile,LineNumberTable` y el mapping de R8 se sube solo.
- **CRA-03** iOS: `firebase-ios-sdk` por SPM, fases "Copy GoogleService-Info.plist" y "Upload dSYM" (solo Release).
- **CRA-04** Debug no recolecta. El crash de prueba solo se compila con un flag (`-P…` / `-D…`).
- **CRA-05** Solo fatales, salvo que haga falta más: entonces se implementa `CrashReporter` (OBS-01).
- **CRA-06** Al activarlo, actualizar la política de privacidad, Data safety y la privacidad de App Store (PRIV-03).

## 2.3 Datos públicos o de gobierno · ¿La app muestra información de una entidad pública?

- **GOV-01** Aviso "app no oficial" visible en el onboarding, la pantalla principal, el detalle y Ajustes.
- **GOV-02** Enlaces a las fuentes oficiales en la ficha de la tienda y dentro de la app. Play rechazó Cazadores
  sin ellos (política de afirmaciones engañosas).
- **GOV-03** Si la API está en otro país o falla desde nubes extranjeras, sincronizar desde una máquina local y
  publicar archivos estáticos (p. ej. Firebase Storage). La app nunca depende de esa máquina.
- **GOV-04** Lo que el usuario guarda es una copia y envejece: al llegar datos nuevos, actualizar cada guardado
  por su id (conservando la fecha en que se guardó) y, si ya no está en la fuente, marcarlo ("Ya no figura") en
  vez de borrarlo. En Cazadores: `FavoritesRepositoryImpl.reconcile` + columna `delistedAt`.
- **GOV-05** Archivos que cambian poco: si faltan pocos (p. ej. ≤ 30), bajarlos sueltos; si faltan muchos, el zip.

## 2.4 API ajena o inestable · ¿La app depende de una API que no controlas?

- **API-01** Verificar contra la API real antes de escribir el repositorio: paginación (0- o 1-indexed), filtros
  que no filtran, ordenamientos que dan 500, cabeceras necesarias. Anotar cada hallazgo y cubrirlo con un test.
- **API-02** No exponer en la UI un filtro que siempre devuelve 0: engaña al usuario.
- **API-03** Cabeceras honestas (`User-Agent: <App>/<versión> (<plataforma>)`), sin imitar a la web oficial.
- **API-04** TLS: **nunca fijar el certificado hoja** (se renueva). Si el servidor omite la intermedia, en Android
  usar `network_security_config` con sistema + intermedia en `res/raw`, solo para ese dominio.
- **API-05** Sin red, no reintentar: ir directo a la caché. Mostrar la fecha de los datos ("Sin conexión · datos del…").
- **API-06** Catálogos fijos, empaquetados en `composeResources/files`, con refresco en segundo plano.

## 2.5 Pagos · ¿La app cobra algo?

- **PAY-01** Contenido digital (premium, quitar anuncios, suscripciones, monedas): siempre compra dentro de la app
  de Google Play y App Store, con **RevenueCat** encima (SDK de Kotlin Multiplatform): valida en su servidor,
  unifica ambas tiendas y da paywalls y métricas. Revisar el precio vigente antes de elegirlo (gratis hasta
  cierto nivel de facturación, luego un porcentaje). Si solo hay una compra única sin suscripción, StoreKit 2 +
  Play Billing directos pueden bastar.
- **PAY-02** Bienes físicos o servicios fuera de la app (envíos, reservas): pasarela propia (Stripe, Culqi,
  Mercado Pago…), sin RevenueCat ni compras de la tienda.
- **PAY-03** El código pregunta por **derechos** (`premium`), no por productos (`pro_monthly`). El derecho cambia la
  app en ejecución: con `premium`, el `AdsController` pasa a `NoAdsController` (no el flag de compilación de ADS-06).
- **PAY-04** Botón "Restaurar compras" (Apple lo exige).
- **PAY-05** Probado con los testers de sandbox de cada tienda: compra, cancelación, renovación y restauración.
- **PAY-06** Privacidad actualizada: RevenueCat procesa datos de compra (PRIV-03).

## 2.6 Cuentas de usuario · ¿La app tiene registro o inicio de sesión?

- **ACC-01** Si hay inicio de sesión con Google, Facebook u otro tercero, en iOS ofrecer también Sign in with
  Apple u otra opción equivalente en privacidad (App Store 4.8).
- **ACC-02** Borrar la cuenta desde la app (Apple y Google lo exigen) y desde una página web (Google lo pide en
  Data safety). Borrar también los datos del servidor, no solo desactivar.
- **ACC-03** La sesión se guarda en el almacén seguro de cada plataforma (Keystore / Keychain), nunca en
  DataStore sin cifrar.
- **ACC-04** La app sirve sin cuenta hasta donde se pueda; pedir la cuenta solo para lo que la necesita.
- **ACC-05** Cuenta de prueba para la revisión de las tiendas, anotada en App Store Connect y Play Console.

## 2.7 Notificaciones push · ¿La app manda notificaciones?

- **PUSH-01** FCM en Android y APNs en iOS (FCM puede cubrir ambos). La clave `.p8` de APNs, en el respaldo de
  SEC-04.
- **PUSH-02** En Android 13 o más, el permiso `POST_NOTIFICATIONS` se pide en contexto (PERM-01), no al abrir.
- **PUSH-03** Canales de notificación en Android con nombres claros; el usuario puede apagarlos por tipo.
- **PUSH-04** Tocar una notificación abre la pantalla correcta por deep link (DL-02).

## 2.8 Deep links · ¿La app abre enlaces o deep links (web o esquema propio)?

DL-01 aplica solo a enlaces web (`https://`); DL-02 y DL-03, a cualquier deep link.


- **DL-01** App Links y Universal Links verificados: `/.well-known/assetlinks.json` y
  `/.well-known/apple-app-site-association` en el dominio, con el `applicationId`, la huella SHA-256 de Play App
  Signing y el Team ID + bundle de iOS.
- **DL-02** Un objeto común traduce URL → ruta (ver 1.4) y se testea (TST-06). Una URL desconocida abre Inicio,
  nunca una pantalla vacía.
- **DL-03** Sin esquemas propios (`miapp://`) en lo que se comparte: las apps de mensajería no los abren como
  enlace (Cazadores D25).

## 2.9 Actualización y formato de datos · ¿Un cambio de API o de datos puede romper versiones viejas?

- **UPD-01** Un archivo remoto (manifest, Remote Config o la propia API) publica la versión mínima soportada.
- **UPD-02** Si la versión instalada es menor, la app muestra una pantalla que lleva a la tienda. Si el archivo
  no se puede leer, la app sigue normal (nunca bloquear por falta de red).
- **UPD-03** Avisar de una versión nueva no obligatoria es opcional y nunca bloquea.
- **UPD-04** Si la app lee archivos publicados por un proceso propio (p. ej. un padrón semanal en Storage), el
  formato lleva una versión de esquema y la app ignora los campos que no conoce. Cambiar el formato sin romper las
  versiones instaladas: campos nuevos opcionales; un cambio incompatible sube la versión de esquema y la versión
  mínima (UPD-01).

## 2.10 Analítica · ¿La app mide uso?

- **ANA-01** Lista corta de eventos con nombre y propósito, anotada en `DECISIONES.md`. Medir decisiones, no todo.
- **ANA-02** Sin datos personales ni texto libre del usuario en los eventos (OBS-04).
- **ANA-03** Detrás de una interfaz común (como OBS-01), con implementación vacía en debug.
- **ANA-04** Privacidad actualizada (PRIV-03); si cruza datos con anuncios, ATT en iOS.

## 2.11 Tablets e iPad · ¿La app se publica para iPad o debe verse bien en tablets?

- **TAB-01** El diseño depende del ancho medido, no del dispositivo: contenido con ancho máximo (~720 dp)
  centrado y margen mínimo, listas en más columnas desde ~560 dp, una columna con letra muy grande. Sirve también
  para tablets y plegables Android (TIE-04).
- **TAB-02** Declarar iPad (`TARGETED_DEVICE_FAMILY = "1,2"`) no tiene vuelta atrás: va en su propia release
  (GIT-07) con prueba en iPad (letra 2×, oscuro, Stage Manager) y capturas de iPad 13".
- **TAB-03** En iPad, la hoja de compartir necesita un ancla (popover); sin ella la app se cierra.

## 2.12 Apps del estudio Nekodojo · ¿La app es de Nekodojo?

- **NEK-01** La app está en el proyecto web de Nekodojo (`/Volumes/Neko/webs/neko-dojo`, Next.js):
  - Ficha en `src/content/apps.ts`: `slug`, `status` y `stores` (URL de cada tienda, o `null` mientras no esté
    publicada; la landing muestra "Próximamente").
  - Páginas en `src/app/[locale]/(apps)/apps/<slug>/`: landing, `privacy`, `terms` y `support`. Esas URLs son las
    que la app abre en Ajustes (AJU-01) y las que van en las fichas de las tiendas.
  - El español va sin prefijo y el inglés con `/en/` (`src/proxy.ts`).
- **NEK-02** El mensaje de compartir usa `https://nekodojo.app/apps/<slug>/descargar` (AJU-04). La ruta
  `/apps/[slug]/descargar` del sitio lee `stores` de `apps.ts`, así que sirve a todas las apps.
- **NEK-03** `app-ads.txt` en `nekodojo.app` si la app tiene anuncios (ADS-07).

---

# Parte 3 · Plantilla: qué copiar de Cazadores Perú (solo al crear una app)

Rutas relativas al repo de Cazadores (`pe/cazadores/` → el paquete nuevo). Copiar, renombrar el paquete y borrar
lo específico (padrón, criminales, recompensas).

| Qué | Archivos |
|---|---|
| Build | `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `composeApp/build.gradle.kts` (generación de `AppInfo`, flag `ADS_ENABLED`, `checkIosVersion`), `androidApp/build.gradle.kts` (firma, Crashlytics condicional, IDs de AdMob) |
| Secretos | `local.properties.example`, `iosApp/Configuration/Config.xcconfig`, `Secrets.xcconfig.example`, `.gitignore` |
| R8 | `androidApp/proguard-rules.pro` (regla de DataStore + atributos para Crashlytics) |
| Resultado y errores | `core/result/AppResult.kt`, `data/repository/ErrorMapping.kt` |
| Red | `data/remote/HttpClientFactory.kt` (timeouts, retry, User-Agent, logging en debug) |
| DI | `di/Koin.kt` (forma de los 4 módulos, `APP_SCOPE`, `initKoin`), `di/PlatformModule.*.kt`, `iosMain/di/IosEntry.kt` (puente + hook de excepciones) |
| Plataforma | `platform/ExternalActions.kt` + actuals, `NetworkMonitor` (interfaz en `domain/repository/Repositories.kt`) + `AndroidNetworkMonitor` / `IosNetworkMonitor` |
| Anuncios | `ads/AdsController.kt`, `ads/NoAdsController.kt`, `ads/InlineAd*.kt`, actuals Android, `iosMain/ads/IosAdsBridge.kt`, `iosApp/iosApp/AdsBridge.swift` |
| Tema | `ui/theme/{Color,Type,Theme}.kt` (tokens `Cz.*`), `commonTest/ui/theme/ContrastTest.kt` |
| UI base | `ui/components/StateViews.kt`, `Buttons.kt`, `StatusBarAppearance.kt` + actuals |
| Navegación | `ui/navigation/{Routes,AppNavHost,BottomBar,DeepLinks}.kt` |
| WebView | `ui/screens/legal/*` + `PlatformWebView.*.kt` (política de enlaces y su test) |
| Ajustes | `ui/screens/settings/*`, `ui/screens/about/*` (filas de AJU-01) |
| Tests | `commonTest/testing/Fakes.kt` (forma), `ui/screens/ViewModelsTest.kt` (patrón Turbine + dispatcher), `iosTest/data/local/AppDatabaseTest.kt`, `ui/screens/detail/PosterShareTest.kt` (lógica sacada de un Composable) |
| UI E2E | `.maestro/smoke-guardar.yaml` (esperas largas, diálogo del sistema, patrón con NBSP) |
| Docs | `CLAUDE.md` (estructura de secciones), `docs/DECISIONES.md` y `docs/PENDIENTES.md` (vacíos, con el encabezado) |

**Siguiente paso posible:** convertir esto en un repo plantilla de GitHub (o un módulo `core` compartido) para no
copiar a mano en cada app.

---

# Parte 4 · Checklist para arrancar una app nueva

1. Copiar la plantilla (Parte 3), renombrar el paquete, `applicationId` y `BUNDLE_ID`. **El identificador no se
   puede cambiar una vez publicado.**
2. Crear `CLAUDE.md`, `docs/DECISIONES.md` y `docs/PENDIENTES.md` (DOC-01). Si es de Nekodojo, sumarla a
   `neko-dojo` con su landing, `privacy`, `terms` y `support` (NEK-01).
3. Activar el hook `pre-push` (CI-01) y `checkIosVersion` (VER-02). Crear `release/0.1.0` desde `main` y trabajar
   con el flujo de ramas (1.15).
4. Responder las preguntas de la Parte 2 y anotar en `DECISIONES.md` qué secciones aplican.
5. Si hay API ajena, verificarla primero (API-01) y anotarla.
6. Tema con tokens + `ContrastTest`; strings en recursos desde la primera pantalla (COD-01, COD-02).
7. `AppResult`/`DataError`, `NetworkMonitor`, `CrashReporter` vacío, `StateViews` (ARQ-03, OBS-01, UIS-01).
8. Tests en `commonTest` desde el primer caso de uso; un flujo Maestro desde la primera pantalla navegable (TST-08).
9. Para cada vista nueva, usar la skill `ux-audit` (ficha, capturas, auditoría).

# Parte 5 · Checklist de publicación

**Antes de cada build para tienda:**
- **PUB-01** `./gradlew check` en verde, tests de iOS y flujos de Maestro en local.
- **PUB-02** Release Android (R8) y Release iOS probados: claro, oscuro, letra grande, modo avión (TST-09), y una
  pasada con TalkBack y VoiceOver (A11Y-04).
- **PUB-03** Versiones iguales en el catálogo y en el xcconfig; `versionCode` / build number mayores que el último
  subido (VER-02).
- **PUB-04** Si cambió la base de datos: instalar la versión nueva **encima de la publicada**, con datos guardados,
  y comprobar que siguen ahí.
- **PUB-05** Flags de prueba apagados (`ADS_ENABLED=true`, sin crash de prueba).
- **PUB-06** Ajustes completos (AJU-01) y las URLs de privacidad y términos respondiendo 200.
- **PUB-07** Si hay pagos: compra, cancelación y restauración probadas en sandbox (PAY-05).
- **PUB-08** Medidas de rendimiento anotadas (PERF-01) y entrada en el changelog (DOC-02).

**Play Console:**
- **PUB-09** Firmado con la clave de subida y Play App Signing activado (SEC-03).
- **PUB-10** Data safety al día con todo lo que recolectan los SDKs (PRIV-03); "Contiene anuncios" si aplica.
- **PUB-11** `targetSdk` dentro del plazo (TIE-02). Si muestra datos de gobierno, fuentes oficiales en la
  descripción (GOV-02).

**App Store Connect:**
- **PUB-12** Privacidad de la app al día y `PrivacyInfo.xcprivacy` revisado (PRIV-02).
- **PUB-13** `NSUserTrackingUsageDescription` si hay ATT y claves de uso de cada permiso (PERM-03).
- **PUB-14** Compilado con el SDK que Apple exige ese año (TIE-01). Cuenta de prueba si hay login (ACC-05).

**Fuera de las tiendas y al cerrar:**
- **PUB-15** Política de privacidad y términos publicados (que no den 404); `app-ads.txt` si hay anuncios (ADS-07).
- **PUB-16** Merge de la release a `main` sin squash, tag `vX.Y.Z` y borrar las ramas unidas (GIT-01, GIT-08).

# Parte 6 · Mantenimiento de este documento

- Al cerrar cada versión de una app, revisar su `DECISIONES.md` y pasar aquí **solo** lo que se repitió en más de
  una app o costó caro (un rechazo, un release roto, días de depuración).
- Lo que solo aplica a una app se queda en su repo.
- Al tocar la Parte 1.1 o la 1.18, volver a verificar versiones y plazos; anotar la fecha de verificación.
- Si una regla de aquí se rompe a propósito en una app, anotar por qué en el `DECISIONES.md` de esa app: en la
  auditoría cuenta como ➖ con esa referencia.
- IDs nuevos al final de su prefijo; los retirados no se reutilizan.
- Después de cada auditoría (Parte 7), aplicar aquí sus "Propuestas para la base" y subir la fecha de versión.

---

# Parte 7 · Auditar una app contra esta base

## 7.1 Pasos

1. **Aplicabilidad.** Responder con evidencia del repo (dependencias, manifest, `Info.plist`, pantallas) las
   preguntas de la Parte 2:

   | Sección | Pregunta | Respuesta |
   |---|---|---|
   | 2.1 ADS | ¿Muestra anuncios? | |
   | 2.2 CRA | ¿Reporta crashes? | |
   | 2.3 GOV | ¿Muestra información de una entidad pública? | |
   | 2.4 API | ¿Depende de una API que no controla? | |
   | 2.5 PAY | ¿Cobra algo? | |
   | 2.6 ACC | ¿Tiene cuentas de usuario? | |
   | 2.7 PUSH | ¿Manda notificaciones? | |
   | 2.8 DL | ¿Abre enlaces o deep links (web o esquema propio)? | |
   | 2.9 UPD | ¿Un cambio de API o de datos puede romper versiones viejas? | |
   | 2.10 ANA | ¿Mide uso? | |
   | 2.11 TAB | ¿Se publica para iPad o debe verse bien en tablets? | |
   | 2.12 NEK | ¿Es de Nekodojo? | |

   Una sección con "no" queda ➖ entera. Si la respuesta es "todavía no, pero se planea", la sección se audita y
   sus ❌ van a pendientes como "antes de activar X".
2. **Revisar cada regla** de la Parte 1, de las secciones que aplican de la Parte 2 y de la Parte 5. La Parte 3,
   la 1.14 y la Parte 6 no se auditan.
3. **Marcar el estado** con evidencia:
   - ✅ **Hecho** — con `archivo:línea`, el comando que lo comprueba o la decisión que lo documenta.
   - ⚠️ **Parcial** — qué parte falta.
   - ❌ **Falta** — y la acción concreta.
   - ➖ **No aplica** — por qué (la sección no aplica, o la app lo rompe a propósito con su decisión anotada).
   - ❔ **No verificable desde el repo** — requiere dispositivo, consola de la tienda o un dato del dueño.
4. **Pruebas en dispositivo, en una sola sesión.** Desde el repo no se ven, y quedan ❔ hasta probarlas en el
   emulador, el simulador o las consolas:
   - Release (R8) y Release iOS en claro, oscuro, letra grande y modo avión (TST-09, PUB-02).
   - TalkBack y VoiceOver por el flujo principal, zonas táctiles y descripciones (A11Y-02/03/04).
   - Negar cada permiso y seguir usando la app (PERM-02).
   - Emulador de tablet ≥ 600 dp en horizontal (TIE-04).
   - Instalar la versión nueva encima de la publicada con datos (PUB-04).
   - Política publicada, Data safety y privacidad de App Store frente a los SDKs (PRIV-03, PUB-10, PUB-12).
   - Play App Signing activado (SEC-03).
5. **Decidir si un ❌ es necesario.** No toda regla vale para toda app: si no tiene sentido, se marca ➖ con el
   motivo y se anota la decisión en el `DECISIONES.md` de la app, en vez de implementarla.
6. **Entregar** el informe (7.2) en `docs/auditorias/<fecha>-base-kmp.md` de la app y pasar cada ❌ y ⚠️ aceptado
   a su `PENDIENTES.md`, con su ID y prioridad. Las propuestas para la base se aplican aquí (Parte 6).
7. **Contar** los estados con los IDs de la base (que ninguna regla quede sin estado) antes de dar los totales.

## 7.2 Formato del informe

La tabla de detalle lleva solo lo que **no** es ✅; los ✅ van agrupados por tema con su evidencia. Con todas las
reglas en la tabla el informe se vuelve ilegible.

```markdown
# Auditoría de <App> contra BASE-CONOCIMIENTO-KMP (<fecha>, base del <fecha de la base>)

Rama auditada: `<rama>` (`<commit>`).

## Aplicabilidad
| Sección | Pregunta | Respuesta | Evidencia |
|---|---|---|---|

## Resumen
- Aplican: … · No aplican: 2.5 PAY (no cobra), …
- Reglas: <total>. ✅ n · ⚠️ n · ❌ n · ➖ n (+n en bloque) · ❔ n
- Lo más urgente (máx. 5): <ID> — <por qué>.

## Detalle (lo que no está ✅)
| ID | Regla (resumen) | Estado | Evidencia | Acción |
|---|---|---|---|---|
| VER-02 | `check` falla si el xcconfig no coincide | ❌ | no existe `checkIosVersion` | Copiar la tarea de 1.6 |

## Hechos (✅)
- **Arquitectura:** ARQ-01 a ARQ-07 (`AppResult.kt`, `Koin.kt`…).

## Para PENDIENTES.md
- Alta / Media / Baja: <ID> — <acción>.

## Propuestas para la base
- Reglas mal escritas, difíciles de comprobar o que faltan.
```

**Prioridad de los ❌:** primero lo que puede causar rechazo en tienda, pérdida de datos o un release roto (PUB-*,
TRP-*, PRIV-*, ACC-02, PAY-04), luego seguridad y privacidad, luego lo demás.

## 7.3 Prompt para pedir la auditoría

> Lee `BASE-CONOCIMIENTO-KMP.md` y audita este repo contra la base siguiendo su Parte 7: primero la tabla de
> aplicabilidad con evidencia, después cada regla con su estado y evidencia (`archivo:línea` o comando), y entrega
> el informe en el formato 7.2 en `docs/auditorias/<fecha>-base-kmp.md`. No cambies código. Si una regla no tiene
> sentido para esta app, márcala ➖ y explica por qué. Lista aparte lo que necesita dispositivo (paso 4). Al final,
> propón qué pasar a `PENDIENTES.md` y qué corregir en la base.
