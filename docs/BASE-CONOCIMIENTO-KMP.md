# Base de conocimiento KMP

Guía para empezar y mantener apps **Kotlin Multiplatform + Compose Multiplatform** (Android + iOS, UI
100 % compartida). Nació de construir y publicar **Cazadores Perú** (nov. 2026). Las referencias
"(Cazadores Dnn)" apuntan a `docs/DECISIONES.md` de ese repo, donde está la evidencia.

## Cómo usar este documento

- **Parte 1, siempre aplica.** Cualquier app KMP nueva.
- **Parte 2, si aplica.** Solo si la app tiene anuncios, crashes reportados, datos públicos o una API ajena.
- **Parte 3, plantilla.** Qué archivos copiar de Cazadores Perú y qué renombrar.
- **Partes 4 y 5, checklists.** Para arrancar y para publicar.
- **Parte 6, mantenimiento.** Cómo evitar que este documento se quede viejo.

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

**Por qué dos módulos:** AGP 9 prohíbe `com.android.application` y KMP en el mismo módulo.

## 1.3 Arquitectura

- **Clean + MVVM unidireccional.** `UiState` inmutable en un `StateFlow`; la pantalla solo emite eventos.
  **Ningún ViewModel toca Ktor ni Room**: habla con casos de uso, y estos con interfaces de repositorio.
- **Resultado tipado.** `AppResult<T>` es `Success(data, cachedAtMillis?)` o `Failure(DataError)`.
  `DataError` es cerrado (`Network`, `Timeout`, `NotFound`, `Server(code)`, `Unknown`) para que la UI
  explique cada caso. Un solo `Throwable.toDataError()` en la capa de datos.
- **Koin en 4 módulos:** `platformModule` (expect/actual), `dataModule`, `domainModule` (factories) y
  `uiModule` (`viewModelOf`). Un `CoroutineScope` de app con `SupervisorJob`, calificado, para trabajo que
  sobrevive a la pantalla. Nunca `GlobalScope`.
- **Arranque:** Android llama `initKoin()` en `Application`. iOS llama desde Swift a una función Kotlin
  que recibe los puentes nativos y arranca Koin.
- **Lo local primero.** Si un dato puede estar empaquetado o guardado, la UI lo muestra al instante y
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
| Barra de estado | `WindowInsetsController` | pila propia |

**Patrón puente para SDKs que solo existen en Swift/SPM** (anuncios, Firebase, compras): declarar una
`interface` en `iosMain`, implementarla en Swift y pasarla a Koin al arrancar. Evita cinterop y CocoaPods.

**Implementación vacía por flag de build:** una clave en `local.properties` → Gradle genera `AppInfo.kt`
→ Koin entrega una implementación vacía (p. ej. `NoAdsController`). Sirve a ambas plataformas porque
Xcode compila el framework con el mismo Gradle.

## 1.5 Reglas de código

- **Textos de UI solo en `composeResources/values/strings.xml`.** Nada de literales en Composables.
- **Colores y tipografía solo desde tokens** del tema. Un `ContrastTest` verifica 4.5:1 (texto) y 3:1
  (elementos) en claro y oscuro: el contraste se testea, no se revisa a ojo.
- **Texto grande:** filas "texto + acción" con `FlowRow`; etiquetas de pestaña limitadas a ~1,3×. Probar
  Android 2× e iOS Accesibilidad XL.
- **Sin lógica en Composables.** Codificar imágenes, armar archivos, formatear o decidir qué mostrar va
  en el ViewModel o en un caso de uso, donde se puede testear. Señal de alerta: `withContext`,
  `runCatching` o `LaunchedEffect` con trabajo pesado dentro de una pantalla.
- **Pantallas chicas.** Si un `Screen.kt` pasa de ~300 líneas, dividirlo en secciones (`HeaderSection`,
  `ResultsList`…) en el mismo paquete. Cada sección recibe estado y lambdas, nunca el ViewModel.
- **Sin `!!`, sin `GlobalScope`.**
- **Conventional Commits, sin atribución de IA.** Un commit por paso lógico.

## 1.6 Una sola fuente de versión

La versión vive en `libs.versions.toml` (`app-versionName`, `app-versionCode`). iOS la necesita en
`Config.xcconfig` (`MARKETING_VERSION`, `CURRENT_PROJECT_VERSION`) y en Cazadores se desincronizó dos veces.
Desde el día 1, hacer que `check` falle si no coinciden:

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

## 1.7 Secretos

`local.properties` (Android) y `Secrets.xcconfig` (iOS) fuera de git, con sus `.example` versionados. Sin
secretos, el build **sigue funcionando** con valores de prueba y **avisa** con un warning de Gradle/Xcode.
La keystore de subida va fuera del repo (p. ej. `~/.config/<app>-signing/`) y se activa Play App Signing.

## 1.8 Testing

**Unitarios (en `commonTest`, corren en JVM con `testAndroidHostTest`):**
- Reglas puras, formatos y normalización de texto con tildes.
- Mapeo DTO → dominio y cualquier traducción con la API (paginación, ids).
- Cliente de API con **Ktor MockEngine**: éxito, 404, timeout, sin red → caché.
- Repositorios con **fakes a mano** (`testing/Fakes.kt`) en vez de librerías de mocks.
- ViewModels con **Turbine** + `StandardTestDispatcher` + `Dispatchers.setMain`; el trabajo de fondo usa
  el `backgroundScope` del test.
- Deep links, contraste del tema y política de enlaces del WebView.

**Room y migraciones en `iosTest`** (`iosSimulatorArm64Test`): la JVM host no tiene driver de SQLite.
Cada `AutoMigration` lleva su test.

**Flujos de UI, con Maestro (recomendado):**
- Flujos en YAML que corren igual en el emulador Android y en el simulador iOS. La CLI es gratis y corre
  en local, lo que encaja con repos privados (Maestro Cloud es de pago y no hace falta).
- Encuentra elementos por texto visible o por descripción de accesibilidad. Para usar `testTag` en
  Android, activar `Modifier.semantics { testTagsAsResourceId = true }` en la raíz de la app. En iOS,
  comprobar al montar si la versión de CMP expone `testTag` como `accessibilityIdentifier`; si no, usar texto.
- Espera elementos sola (sin sleeps fijos), así que sirve con emuladores lentos.
- Empezar con un smoke test del flujo principal y uno por bug de navegación arreglado.
- `takeScreenshot` en los flujos sirve también para las capturas de tienda.
- Lecciones de Cazadores (flujo `.maestro/smoke-guardar.yaml`, probado en el emulador Android):
  - **Anuncios fuera:** correr sobre un build con `ADS_ENABLED=false`. En debug el formulario de UMP
    (geografía EEE forzada) tapa la primera pantalla y el flujo no lo maneja.
  - **Espacios invisibles:** los montos formateados usan espacio de no separación (`S/ 500 000`). Un patrón
    `"S/ .*"` no encuentra nada; usar `"S/.*"`. Si un texto "visible" no se encuentra, sospechar de esto.
  - **Primera apertura lenta:** tras `clearState` el emulador puede tardar más de un minuto. Usar
    `extendedWaitUntil` con `timeout: 120000` en la primera pantalla y en la primera carga de datos.
  - **Diálogos del sistema:** con el emulador cargado aparece "System UI isn't responding". Cerrarlo con un
    `runFlow` condicional (`when: visible: ".*isn't responding"` → `tapOn: "Wait"`).
  - **Firma distinta:** si el emulador tiene la app instalada desde otra máquina o la tienda, Maestro no
    puede instalar el debug (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). Hay que desinstalarla y se pierden sus datos.
  - **Migraciones:** tras correr un build con Room más nuevo, instalar uno más viejo encima falla al abrir.
    Borrar los datos de la app antes.
- Alternativa sin dispositivo: `runComposeUiTest` (compose-ui-test multiplataforma) para probar
  componentes sueltos. Cubre menos (no prueba el host nativo) y en JVM puede pedir configuración extra.

**Verificación manual que no se salta:** release (R8) en Android, Release en iOS, claro y oscuro, letra
grande, modo avión.

## 1.9 CI con poco consumo (repos privados)

En repos privados GitHub Actions descuenta minutos del plan, y los runners de macOS cuentan varias veces
más que los de Linux. Revisar el plan vigente antes de decidir. De menor a mayor costo:

1. **Hook `pre-push` local (gratis, recomendado para empezar).** `./gradlew check` corre antes de cada push:
   ```bash
   mkdir -p .githooks && printf '#!/bin/sh\n./gradlew check --quiet\n' > .githooks/pre-push && chmod +x .githooks/pre-push
   ```
   ```bash
   git config core.hooksPath .githooks
   ```
   Cada clon nuevo debe repetir el `git config` (anotarlo en el README). `git push --no-verify` lo salta
   en una emergencia.
2. **CI solo en Linux y solo en PR a `main`:** `./gradlew check` con caché de Gradle,
   `concurrency: cancel-in-progress` y `paths-ignore` para `docs/**`. Los tests de iOS y Maestro se corren
   en local antes de cada release.
3. **Runner propio en tu Mac** para iOS, si algún día hace falta. Revisar si GitHub cobra por uso de
   runners propios en el plan vigente.

## 1.10 Errores y observabilidad

- Desde el día 1, una interfaz común `CrashReporter` (`log(message)`, `recordNonFatal(throwable)`) en
  `platformModule`, con una implementación vacía. Así, agregar Crashlytics u otro servicio no toca el código común.
- No reportar errores de red esperados (sin conexión, timeouts) si la app ya tiene respaldo: es ruido.
- En iOS, `setUnhandledExceptionHook` con `NSLog` de la traza antes del abort. Sin eso, el error de
  Kotlin no queda en el log del sistema.

## 1.11 Internacionalización

Aunque la app salga solo en un país, escribir todos los textos en `strings.xml` (ya es regla) y usar
plurales y formatos de `composeResources`. Agregar `values-en/strings.xml` después cuesta poco; buscar
literales repartidos cuesta mucho. Fechas y montos con formateadores propios en `core/`, testeados.

## 1.12 Presupuesto de rendimiento

Medir y anotar en cada release (en el `DECISIONES.md` de la app):
- Tamaño del AAB/APK de release y del IPA (el framework de Kotlin pesa; vigilar que no crezca de golpe).
- Arranque en frío hasta la primera pantalla útil, en un Android de gama baja.
- Datos que baja la app en la primera apertura (importa con datos móviles caros).

## 1.13 Trampas de KMP/CMP ya pagadas

| Problema | Causa | Arreglo |
|---|---|---|
| Release Android en blanco | R8 borra campos de `GeneratedMessageLite` que DataStore lee por reflexión | Regla keep en `proguard-rules.pro`. **Probar siempre el release.** |
| Hojas inferiores que oscilan en iOS | `ModalBottomSheet` (M3 1.9) suma relleno de la barra de estado en bucle | Relleno fijo arriba, insets solo abajo, `OnFocusBehavior.DoNothing` |
| Pestaña equivocada al volver | `popUpTo(startDestination)` apuntaba a una pantalla ya fuera de la pila (onboarding) | `popUpTo<Route.Home>` |
| Enums en rutas fallan en iOS | Navigation type-safe no los soporta en iOS | Pasar el nombre como `String` |
| Barra de estado equivocada al navegar | La pantalla nueva se compone antes de desechar la vieja | Pila de apariencias |
| "Guardar imagen" cierra la app iOS | Falta `NSPhotoLibraryAddUsageDescription` | Declararla si se comparte una imagen |
| Gmail ignora el destinatario | `mailto:` con `%40` | Destinatario sin codificar + `EXTRA_EMAIL` |
| Let's Encrypt falla en Android 7 | minSdk 24 no confía en ISRG Root X1 | Botón "Abrir en el navegador" de respaldo |
| Paquetes SPM | Se agregan a mano en `project.pbxproj` | Documentar los IDs en el `CLAUDE.md` de la app |
| Cambiar entidades Room | Rompe instalaciones existentes | Subir `version` + migración + test en `iosTest` |

## 1.14 Limitaciones de KMP/CMP

- SDKs de iOS sin binding Kotlin cómodo → patrón puente Swift (1.4).
- Trazas de Kotlin en iOS pobres en los reportes de crash (framework estático).
- Room no corre en los tests JVM host: los de BD van al simulador, que es más lento.
- Material 3 multiplataforma va detrás del de Android, con bugs propios en iOS (hojas, foco, teclado).
- WebView, mapas, cámara y share sheet siempre necesitan `expect/actual` o `UIKitView`/`AndroidView`.
- Apple exige un SDK nuevo cada año (desde abril de 2027, el de iOS 27). Planificarlo.

## 1.15 Ramas y versiones

- `main` = solo versiones publicadas, con tag `vX.Y.Z` en cada merge. Sin PRs directos.
- `release/X.Y.Z` = candidata a tienda, creada desde `main`. El primer commit sube la versión; todos los
  PRs apuntan aquí; de aquí salen TestFlight y la prueba interna de Play. Una sola release abierta a la vez.
  Al cerrarla: merge normal (sin squash) a `main`, tag y subida a las tiendas.
- `feature/…`, `fix/…`, `chore/…` = ramas de trabajo desde la release abierta, con PR a ella.
- `hotfix/…` = desde `main` hacia una `release/X.Y.(Z+1)` nueva. Si había otra release abierta, merge de
  `main` en ella al cerrar el hotfix.
- Misma versión en Android e iOS (§1.6). Arreglo de una sola plataforma: se sube la versión igual, se
  publica solo donde hace falta y la otra plataforma se salta ese número.
- En repos privados, este flujo combina con el CI barato (§1.9): los workflows corren solo en PRs a
  `release/**` y en el push a `main`.

## 1.16 Ajustes estándar

Toda app trae estas filas en Ajustes, en este orden. Textos en `strings.xml`.
- **Privacidad:** "Preferencias de anuncios" (formulario de UMP, solo si hay anuncios y UMP lo exige),
  "Política de privacidad" y "Términos y condiciones".
- **Comunidad:** "Compartir la app" y "Valorar la app".
- **Ayuda:** "Contacto / reportar un problema" (correo con versión, modelo y sistema ya escritos) y
  "Licencias de código abierto".
- **Pie:** la versión visible (`AppInfo.VERSION_NAME`), útil para soporte y para cruzar con Crashlytics.

**Compartir la app:**
- Mensaje de invitación amable, corto y sin presión: qué hace la app y por qué le sirve a quien lo recibe.
- **Un solo enlace de descarga que redirige desde el servidor:** `nekodojo.app/apps/<slug>/descargar`.
  El servidor mira el `User-Agent` y responde un 302, sin mostrar ninguna página:
  - Android → Play (si la app todavía no está en Play, a la landing).
  - iPhone → App Store (si todavía no está, a la landing).
  - Computadora u otro → la landing `nekodojo.app/apps/<slug>`.
  - Los iPad con iPadOS 13+ se presentan como Mac y caen en la landing, que ya tiene el botón del App Store.
- **Por qué así:** el texto de compartir queda grabado en cada versión instalada y se sigue mandando por
  años. Con el enlace del sitio, a dónde lleva se decide en el servidor: publicar en Play, cambiar una
  ficha o sumar una tienda no exige una versión nueva de la app. Además, el mensaje lleva un enlace y no dos,
  la vista previa de WhatsApp sale con la imagen de la landing y los clics se pueden contar.
- **Landing ≠ descarga:** la landing (`/apps/<slug>`) es para leer qué es la app y no redirige; va en la web,
  en redes y en las fichas. `/descargar` va solo en el mensaje de compartir. No poner la redirección dentro
  de la landing: quien entra a leer desde el celular terminaría en la tienda sin pedirlo.
- **Riesgo:** si el dominio vence o el sitio se cae, todos los enlaces compartidos fallan. Mantener el
  dominio renovado. Sin sitio propio, compartir los dos enlaces directos de las tiendas (y nunca el de una
  tienda donde la app aún no está).
- Por la hoja de compartir del sistema (`ExternalActions.share`), nunca a una app concreta.

**Valorar la app:**
- El botón abre la ficha para escribir la reseña: Android `market://details?id=<pkg>` (si falla, la web de
  Play); iOS `itms-apps://apps.apple.com/app/id<ID>?action=write-review`.
- El aviso nativo (Play In-App Review, `SKStoreReviewController`) es otra cosa: el sistema decide si lo
  muestra y limita cuántas veces. Pedirlo solo tras un momento bueno (p. ej. el tercer guardado), nunca tras
  un error ni en el arranque.
- Prohibido por las tiendas: preguntar antes "¿te gusta?" y mandar solo a los contentos a la tienda, o dar
  premios por valorar.

**Dónde viven las landings:** en el proyecto web de Nekodojo (`/Volumes/Neko/webs/neko-dojo`, Next.js).
- Ficha de cada app en `src/content/apps.ts`: `slug`, `status` y `stores` (URL de cada tienda, o `null`
  mientras no esté publicada; la landing muestra "Próximamente"). La ruta `/descargar` lee los mismos datos.
- Páginas en `src/app/[locale]/(apps)/apps/<slug>/`: landing, `privacy`, `terms` y `support`. Esas URLs son
  las que la app abre en Ajustes y las que se ponen en las fichas de las tiendas.
- El español va sin prefijo y el inglés con `/en/` (`src/proxy.ts`).

## 1.17 Enlaces: dónde se abre cada uno

| Enlace | Dónde | Por qué |
|---|---|---|
| Páginas propias (privacidad, términos, ayuda) | WebView dentro de la app | Mantiene el foco; el contenido es tuyo y se controla |
| Sitios ajenos (fuentes, noticias, redes) | Navegador in-app: Custom Tabs (Android), `SFSafariViewController` (iOS) | No sale de la app, muestra la URL real, usa las sesiones y el autocompletado del navegador y no expone la app a contenido ajeno. Google y Apple lo recomiendan en vez de WebView para terceros |
| Tiendas, `mailto:`, `tel:`, mapas | App del sistema | Es lo que el usuario espera |

Reglas del WebView: sin JavaScript si las páginas son HTML estático; la URL sale de la configuración (un
deep link no puede cargar otra); solo el host propio se queda dentro y el resto sale por `ExternalActions`;
HTTP ≥ 400 cuenta como fallo, con "Reintentar" y "Abrir en el navegador".

---

# Parte 2 · Si aplica

## 2.1 Anuncios (AdMob)

- Orden obligatorio: **UMP → ATT (iOS) → `MobileAds.initialize` → anuncios**, en la primera pantalla útil
  tras el onboarding, nunca en el arranque.
- `AdBanner` no ocupa espacio ni pide nada mientras `canRequestAds == false`.
- Debug: IDs de prueba de Google y geografía EEE forzada. Release: IDs reales desde los secretos.
- Ninguna acción depende de un anuncio: si un bonificado falla o tarda más de ~4 s, se hace la acción igual.
- Ajustes muestra "Preferencias de anuncios" solo si UMP lo exige.
- **Apagar los anuncios para capturas y pruebas**, en dos niveles:
  - Compilación: `ADS_ENABLED=false` en `local.properties` → Koin entrega `NoAdsController`. Gradle avisa
    al compilar así.
  - En ejecución, **solo en debug**: un argumento de arranque (`disableAds=true`, que Maestro, `adb` y
    Xcode pueden pasar) o un interruptor oculto en Ajustes (p. ej. 7 toques en la versión). Así no hay que
    recompilar para tomar capturas ni correr flujos. En release ambos se ignoran: nunca se publica sin
    anuncios por error.
- Skills: `add-admob` y `admob-compliance`.

## 2.2 Crashlytics

- Android: firebase-bom + `firebase-crashlytics` en `androidApp`. Los plugins `google-services` y
  `firebase.crashlytics` se aplican **solo si existe `google-services.json`**; si no, warning y compila.
  `-keepattributes SourceFile,LineNumberTable`; el mapping de R8 se sube solo.
- iOS: `firebase-ios-sdk` por SPM, fases "Copy GoogleService-Info.plist" y "Upload dSYM" (solo Release).
- Debug no recolecta. El crash de prueba solo se compila con un flag (`-P…` / `-D…`).
- Solo fatales, salvo que haga falta más: entonces se implementa `CrashReporter` (1.10).
- Al activarlo, actualizar la política de privacidad, Data safety de Play y Privacidad de App Store.

## 2.3 Datos públicos o de gobierno

- Aviso "app no oficial" visible en el onboarding, la pantalla principal, el detalle y Ajustes.
- **Enlaces a las fuentes oficiales** en la ficha de la tienda y dentro de la app. Play rechazó Cazadores
  sin ellos (política de afirmaciones engañosas).
- Si la API está en otro país o falla desde nubes extranjeras, sincronizar desde una máquina local y
  publicar archivos estáticos (p. ej. Firebase Storage). La app nunca depende de esa máquina.
- **Lo que el usuario guarda es una copia y envejece.** Al llegar datos nuevos, actualizar cada guardado por
  su id (conservando la fecha en que se guardó) y, si ya no está en la fuente, marcarlo ("Ya no figura")
  en vez de borrarlo. En Cazadores: `FavoritesRepositoryImpl.reconcile` + columna `delistedAt`.
- **Archivos que cambian poco:** si faltan pocos (p. ej. ≤ 30), bajarlos sueltos; si faltan muchos, el zip.

## 2.4 API ajena o inestable

- **Verificar contra la API real antes de escribir el repositorio**: paginación (0- o 1-indexed), filtros
  que no filtran, ordenamientos que dan 500, cabeceras necesarias. Anotar cada hallazgo y cubrirlo con un test.
- No exponer en la UI un filtro que siempre devuelve 0: engaña al usuario.
- Cabeceras honestas (`User-Agent: <App>/<versión> (<plataforma>)`), sin imitar a la web oficial.
- TLS: **nunca fijar el certificado hoja** (se renueva). Si el servidor omite la intermedia, en Android
  usar `network_security_config` con sistema + intermedia en `res/raw`, solo para ese dominio.
- Sin red, no reintentar: ir directo a la caché. Mostrar la fecha de los datos ("Sin conexión · datos del…").
- Catálogos fijos, empaquetados en `composeResources/files`, con refresco en segundo plano.

## 2.5 Pagos

- **Contenido digital** (premium, quitar anuncios, suscripciones, monedas): siempre compra dentro de la app
  de Google Play y App Store. Usar **RevenueCat** encima (tiene SDK de Kotlin Multiplatform): valida las
  compras en su servidor, unifica ambas tiendas y da paywalls y métricas. Revisar el precio vigente antes de
  elegirlo (es gratis hasta cierto nivel de facturación y luego cobra un porcentaje).
- **Bienes físicos o servicios fuera de la app** (envíos, reservas): pasarela propia (Stripe, Culqi,
  Mercado Pago…), sin RevenueCat ni compras de la tienda.
- El código pregunta por **derechos** (`premium`), no por productos (`pro_monthly`). El derecho cambia la
  app en ejecución: con `premium`, Koin o el `AdsController` pasan a `NoAdsController` (no el flag de
  compilación de 2.1).
- Obligatorio: botón "Restaurar compras" (Apple lo exige), probar con los testers de sandbox de cada tienda
  y actualizar la política de privacidad, Data safety y la privacidad de App Store (RevenueCat procesa
  datos de compra).
- Si solo hay una compra única sin suscripción, StoreKit 2 + Play Billing directos pueden bastar.

---

# Parte 3 · Plantilla: qué copiar de Cazadores Perú

Rutas relativas al repo de Cazadores (`pe/cazadores/` → el paquete nuevo). Copiar, renombrar el paquete y
borrar lo específico (padrón, criminales, recompensas).

| Qué | Archivos |
|---|---|
| Build | `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `composeApp/build.gradle.kts` (generación de `AppInfo`, flag `ADS_ENABLED`), `androidApp/build.gradle.kts` (firma, Crashlytics condicional, IDs de AdMob) |
| Secretos | `local.properties.example`, `iosApp/Configuration/Config.xcconfig`, `Secrets.xcconfig.example` |
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
| Tests | `commonTest/testing/Fakes.kt` (forma), `ui/screens/ViewModelsTest.kt` (patrón Turbine + dispatcher), `iosTest/data/local/AppDatabaseTest.kt`, `ui/screens/detail/PosterShareTest.kt` (lógica sacada de un Composable) |
| UI E2E | `.maestro/smoke-guardar.yaml` (esperas largas, diálogo del sistema, patrón con NBSP) |
| Versión | tarea `checkIosVersion` de `composeApp/build.gradle.kts` (§1.6) |
| Docs | `CLAUDE.md` (estructura de secciones), `docs/DECISIONES.md` y `docs/PENDIENTES.md` (vacíos, con el encabezado) |

**Siguiente paso posible:** convertir esto en un repo plantilla de GitHub (o un módulo `core` compartido)
para no copiar a mano en cada app.

---

# Parte 4 · Checklist para arrancar una app nueva

1. Copiar la plantilla (Parte 3), renombrar el paquete, `applicationId` y `BUNDLE_ID`. **El identificador
   no se puede cambiar una vez publicado.**
2. Crear `CLAUDE.md` (comandos, estructura que engaña, zonas peligrosas), `docs/DECISIONES.md` y
   `docs/PENDIENTES.md`.
   En `neko-dojo`, sumar la app a `src/content/apps.ts` con su landing, `privacy`, `terms` y `support`
   (1.16); el texto de compartir usa `nekodojo.app/apps/<slug>/descargar`.
3. Activar el hook `pre-push` (1.9) y la tarea `checkIosVersion` (1.6).
   Crear `release/0.1.0` desde `main` y trabajar con el flujo de ramas (1.15).
4. Si hay API ajena, verificarla primero (2.4) y anotarla.
5. Tema con tokens + `ContrastTest`; strings en recursos desde la primera pantalla.
6. `AppResult`/`DataError`, `NetworkMonitor`, `CrashReporter` vacío.
7. Tests en `commonTest` desde el primer caso de uso; un flujo Maestro desde la primera pantalla navegable.
8. Para cada vista nueva, usar la skill `ux-audit` (ficha, capturas, auditoría).

# Parte 5 · Checklist de publicación

**Antes de cada build para tienda:**
- `./gradlew check` en verde, tests de iOS y flujos de Maestro en local.
- Release Android (R8) y Release iOS probados: claro, oscuro, letra grande, modo avión.
- Versiones iguales en el catálogo y en el xcconfig; `versionCode` / build number mayores que el último subido.
- Flags de prueba apagados (`ADS_ENABLED=true`, sin crash de prueba).
- Ajustes estándar completos (1.16) y las URLs de privacidad y términos respondiendo 200.
- Medidas de rendimiento anotadas (1.12).

**Play Console:**
- Firmado con la clave de subida (no la de debug) y Play App Signing activado.
- Data safety al día con todo lo que recolectan los SDKs (anuncios, Crashlytics).
- "Contiene anuncios" marcado si aplica. Enlace a la política de privacidad.
- Si muestra datos de gobierno, fuentes oficiales en la descripción (2.3).

**App Store Connect:**
- Privacidad de la app al día; `PrivacyInfo.xcprivacy` de la app revisado.
- `NSUserTrackingUsageDescription` si hay ATT y claves de uso de cada permiso.
- Compilado con el SDK que Apple exige ese año.

**Fuera de las tiendas:**
- Política de privacidad y términos publicados en sus URL (que no den 404).
- `app-ads.txt` en el dominio del desarrollador si hay anuncios.

# Parte 6 · Mantenimiento de este documento

- Al cerrar cada versión de una app, revisar su `DECISIONES.md` y pasar aquí **solo** lo que se repitió en
  más de una app o costó caro (un rechazo, un release roto, días de depuración).
- Lo que solo aplica a una app se queda en su repo.
- Al tocar la Parte 1.1, volver a verificar versiones; anotar la fecha de verificación.
- Si una regla de aquí se rompe a propósito en una app, anotar por qué en el `DECISIONES.md` de esa app.
