# iPad adaptativo (versión rápida) — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** que Cazadores Perú se vea bien en iPad (y en pantallas anchas de Android) sin rediseñar: contenido centrado con ancho máximo, cuadrículas de 3 columnas cuando hay espacio, y volver a declarar iPad en iOS.

**Architecture:** una sola regla de ancho en `ui/components/AdaptiveLayout.kt`: funciones puras (`contentGutter`, `posterColumnsFor`) con tests, y un contenedor `AdaptiveWidth` que mide el ancho disponible con `BoxWithConstraints` y entrega el margen lateral. Cada pantalla usa ese margen como `contentPadding`/`padding` horizontal, así los fondos y el área de scroll siguen a lo ancho y solo el contenido se centra. Sin vista de dos paneles ni `NavigationRail` (fuera de alcance).

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3), Koin, Xcode (iosApp), AdMob.

**Spec:** este documento (sección "Decisiones") — nace de la revisión del 19-nov-2026 tras enviar la 2.0.0 (5) solo para iPhone.

## Decisiones (lo revisado)

- Hoy ninguna pantalla limita el ancho: en iPad 13" todo se estira (tarjetas, textos, botones de 1000+ dp).
- Ya resuelto y no se toca: banners AdMob adaptativos (Android `getCurrentOrientationAnchoredAdaptiveBannerAdSize`, iOS `currentOrientationAnchoredAdaptiveBanner`), cartel para compartir de tamaño fijo (1080×1350 px), `ModalBottomSheet` de M3 (ancho máx. 640 dp por defecto), `Info.plist` con solo vertical y `UIRequiresFullScreen`.
- Bug latente encontrado: en Mis carteles la cadencia de anuncios es `((index + 1) * perRow) % 8 == 0`; con 3 columnas solo hay anuncio cada 24 carteles. Se corrige con una función pura.
- Riesgo: en iPadOS 26 las ventanas son redimensionables y `UIRequiresFullScreen` está en desuso; el diseño no puede depender de un ancho fijo. La regla por ancho medido cubre ventanas de cualquier tamaño.
- Riesgo: `UIActivityViewController` en iPad usa `sourceView` sin `sourceRect`; el popover se ancla en la esquina superior izquierda. Se ancla al centro inferior de la vista.
- Publicación: no lanzar iPad en 2.0.0. Esta es la 2.1.0 (build ≥ 6). Una vez publicada con iPad no se puede retirar.

## Global Constraints

- `CazadoresDimens.contentMaxWidth = 720.dp`; margen mínimo = `CazadoresDimens.screenPadding` (20.dp).
- Columnas de carteles: 1 si `fontScale > 1.3f` (regla existente), 3 si el ancho de contenido ≥ 560.dp, si no 2.
- Anuncios en listas: uno cada `AD_EVERY_N_ITEMS` (8) ítems, sin importar las columnas.
- Textos de UI solo en `composeResources/values/strings.xml`; colores/tipos solo desde `Cz.colors` / `Cz.type`.
- Filas "texto + acción" con `FlowRow`; etiquetas de pestaña ≤ 1,3×.
- Aviso de app no oficial sigue visible en onboarding, Inicio, Ficha y Ajustes.
- Conventional Commits, sin atribución de IA. Decisión nueva arriba en `docs/DECISIONES.md`.
- UX: skill `ux-audit` en modo Construir (ficha antes de programar, checkpoints con capturas, auditoría rápida al cerrar).
- Base de la rama: `main` con el PR #8 (solo iPhone, build 5) ya fusionado.

## Review Focus

1. Letra 2× en iPad: debe quedar 1 columna, ancho máximo igual y nada partido en palabras. → test en Task 1 (`posterColumnsFor` con fontScale 2f y ancho 720).
2. Ventana redimensionada en iPadOS 26 (Stage Manager / Split View): al cambiar el ancho la cuadrícula se recalcula sin perder scroll ni duplicar claves. → verificación manual en Task 5 paso 4.
3. Ancho justo en el umbral (560 dp) y anchos de iPhone (≈ 335–400 dp de contenido): iPhone debe seguir en 2 columnas. → test en Task 1.
4. Mis carteles con 1, 2 y 3 columnas: el anuncio aparece tras los carteles 8, 16, 24… (o el primer múltiplo cruzado). → tests en Task 2.
5. Compartir en iPad: la hoja aparece anclada y no se cierra la app. → verificación manual en Task 5 paso 4.

---

### Task 0: Ficha UX y capturas base

**Files:**
- Create: `docs/capturas/ipad-base/` (ignorada por git, igual que `docs/capturas/`)

- [ ] **Step 1:** con la skill `ux-audit` (modo Construir) escribe la ficha de la vista "Layout ancho": objetivo, pantallas afectadas (Onboarding, Inicio, Buscar, Mis carteles, Ajustes, Ficha + acciones inferiores, Régimen, Sobre el proyecto, Legal), criterios de aceptación = Global Constraints.
- [ ] **Step 2:** en una copia local cambia temporalmente `TARGETED_DEVICE_FAMILY` a `"1,2"` (sin commit), compila Debug e instala en "iPad Pro 13-inch (M5)" e "iPad mini (A17 Pro)". Captura Inicio, Buscar, Ficha, Mis carteles y Ajustes con `xcrun simctl io <udid> screenshot`. Son el "antes".

### Task 1: Reglas de ancho (puras, con tests)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/components/AdaptiveLayout.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/theme/Theme.kt:28-33` (agregar `contentMaxWidth`)
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/components/Poster.kt:235` (`posterColumns` pasa a recibir el ancho)
- Test: `composeApp/src/commonTest/kotlin/pe/cazadores/ui/components/AdaptiveLayoutTest.kt`

**Interfaces:**
- Produces:
  - `val CazadoresDimens.contentMaxWidth: Dp` = 720.dp
  - `fun contentGutter(availableWidth: Dp): Dp` → `max(screenPadding, (availableWidth - contentMaxWidth) / 2)`
  - `fun posterColumnsFor(contentWidth: Dp, fontScale: Float): Int`
  - `@Composable fun AdaptiveWidth(modifier: Modifier = Modifier, content: @Composable BoxWithConstraintsScope.(gutter: Dp, contentWidth: Dp) -> Unit)` — `contentWidth = maxWidth - 2 * gutter`
  - `@Composable fun posterColumns(contentWidth: Dp): Int` = `posterColumnsFor(contentWidth, LocalDensity.current.fontScale)`

- [ ] **Step 1: Write the failing test**

```kotlin
class AdaptiveLayoutTest {
    @Test fun gutterIsScreenPaddingOnPhones() = assertEquals(20.dp, contentGutter(440.dp))
    @Test fun gutterCentersOnTablets() = assertEquals(156.dp, contentGutter(1032.dp))
    @Test fun gutterAtExactMax() = assertEquals(20.dp, contentGutter(760.dp))
    @Test fun phoneKeepsTwoColumns() = assertEquals(2, posterColumnsFor(400.dp, 1f))
    @Test fun thresholdGivesThreeColumns() = assertEquals(3, posterColumnsFor(560.dp, 1f))
    @Test fun justBelowThreshold() = assertEquals(2, posterColumnsFor(559.dp, 1f))
    @Test fun tabletGetsThreeColumns() = assertEquals(3, posterColumnsFor(720.dp, 1f))
    @Test fun largeFontIsOneColumnEvenOnTablet() = assertEquals(1, posterColumnsFor(720.dp, 2f))
}
```

- [ ] **Step 2:** `./gradlew :composeApp:testAndroidHostTest --tests "pe.cazadores.ui.components.AdaptiveLayoutTest"` → FAIL (no compila: funciones inexistentes).
- [ ] **Step 3:** implementa las funciones de "Interfaces" en `AdaptiveLayout.kt` y agrega `contentMaxWidth` a `CazadoresDimens`. El `posterColumns()` actual (sin parámetros, Poster.kt:235) se queda igual en este task; Task 3 lo reemplaza por `posterColumns(contentWidth)` y cambia sus 2 llamadas.
- [ ] **Step 4:** mismo comando → PASS; `./gradlew :composeApp:testAndroidHostTest` completo → PASS.
- [ ] **Step 5:** commit `feat(ui): add width rules for wide screens`.

### Task 2: Cadencia de anuncios independiente de las columnas

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ads/InlineAd.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/saved/SavedScreen.kt:117`
- Test: `composeApp/src/commonTest/kotlin/pe/cazadores/ads/InlineAdCadenceTest.kt`

**Interfaces:**
- Produces: `fun adAfterRow(rowIndex: Int, perRow: Int, every: Int = AD_EVERY_N_ITEMS): Boolean` — verdadero si la fila cruza un múltiplo de `every`: `(rowIndex + 1) * perRow / every > rowIndex * perRow / every`.

- [ ] **Step 1: Write the failing test**

```kotlin
class InlineAdCadenceTest {
    private fun rowsWithAd(perRow: Int, rows: Int = 9) = (0 until rows).filter { adAfterRow(it, perRow) }
    @Test fun oneColumn() = assertEquals(listOf(7), rowsWithAd(1, 9))
    @Test fun twoColumns() = assertEquals(listOf(3, 7), rowsWithAd(2))
    @Test fun threeColumns() = assertEquals(listOf(2, 5, 7), rowsWithAd(3))
}
```

- [ ] **Step 2:** `./gradlew :composeApp:testAndroidHostTest --tests "pe.cazadores.ads.InlineAdCadenceTest"` → FAIL.
- [ ] **Step 3:** implementa `adAfterRow` y reemplaza la condición de `SavedScreen.kt:117` por `if (adAfterRow(index, perRow)) InlineAdSlot(AdPlacement.SAVED_INLINE)`.
- [ ] **Step 4:** mismo comando → PASS.
- [ ] **Step 5:** commit `fix(ads): keep saved-list ad cadence with any column count`.

### Task 3: Pantallas con listas (Inicio, Buscar, Mis carteles)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/home/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/search/SearchScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/saved/SavedScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/components/Poster.kt` (`PosterGrid(items, perRow, onOpenDetail, modifier)`)

**Interfaces:**
- Consumes: `AdaptiveWidth`, `posterColumns(contentWidth)`, `adAfterRow` (Tasks 1–2).

- [ ] **Step 1:** envuelve el contenido de cada pantalla en `AdaptiveWidth { gutter, contentWidth -> … }`. Reemplaza `CazadoresDimens.screenPadding` horizontal por `gutter` en: cabecera oscura de Inicio (el fondo sigue a todo el ancho, el texto y el buscador se centran), cuerpo de Inicio, cabecera y `LazyColumn(contentPadding = PaddingValues(horizontal = gutter, …))` de Buscar, cabecera y `LazyColumn` de Mis carteles. Reemplaza `posterColumns()` por `posterColumns(contentWidth: Dp)` (Poster.kt:235) y úsalo como `perRow` en Mis carteles y en `PosterGrid`, que pasa a recibir `perRow: Int`.
- [ ] **Step 2:** `./gradlew :composeApp:testAndroidHostTest` → PASS; compila iOS: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'id=<iPad Pro 13>' -derivedDataPath iosApp/build build` → BUILD SUCCEEDED (con la copia local de Task 0 en `"1,2"`).
- [ ] **Step 3:** checkpoint ux-audit: capturas en iPhone 17 Pro Max (sin cambios visibles: 2 columnas, margen 20) e iPad Pro 13" (3 columnas, contenido de 720 dp centrado, banner con el ancho del contenido).
- [ ] **Step 4:** commit `feat(ui): center list screens on wide displays`.

### Task 4: Pantallas de lectura (Ficha, Régimen, Sobre el proyecto, Legal, Ajustes, Onboarding)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/detail/DetailScreen.kt` (`DetailBody`, `BottomActions`, barra superior)
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/regime/SpecialRegimeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/about/AboutScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/legal/LegalScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/settings/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/onboarding/OnboardingScreen.kt`

**Interfaces:**
- Consumes: `AdaptiveWidth` (Task 1).

- [ ] **Step 1:** mismo patrón que Task 3: el scroll y los fondos a todo el ancho, el padding horizontal = `gutter`. En Ficha, `BottomActions` también usa `gutter` (botón Llamar no debe medir 1000 dp). Iconos de la barra superior quedan alineados al contenido, no a los bordes de la pantalla.
- [ ] **Step 2:** tests JVM → PASS; build iOS en iPad → BUILD SUCCEEDED.
- [ ] **Step 3:** checkpoint ux-audit en iPad Pro 13" y iPad mini: Ficha, Reportar (hoja), Filtros (hoja), Régimen, Ajustes, Onboarding; y letra 2× en Ficha e Inicio.
- [ ] **Step 4:** commit `feat(ui): center reading screens on wide displays`.

### Task 5: iOS — declarar iPad otra vez y anclar Compartir

**Files:**
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj` (`TARGETED_DEVICE_FAMILY = "1,2";` en Debug y Release)
- Modify: `composeApp/src/iosMain/kotlin/pe/cazadores/platform/IosExternalActions.kt:46`
- Modify: `gradle/libs.versions.toml` (`app-versionName = "2.1.0"`, `app-versionCode` +1), `iosApp/Configuration/Config.xcconfig` (`MARKETING_VERSION`, `CURRENT_PROJECT_VERSION`), `CURRENT_PROJECT_VERSION` en `project.pbxproj`
- Modify: `docs/DECISIONES.md` (decisión nueva arriba: iPad con ancho máximo 720 dp, sin dos paneles; por qué)

- [ ] **Step 1:** en `share(...)` agrega `sourceRect` = rectángulo de 1×1 pt en el centro inferior de `presenter.view.bounds` y `permittedArrowDirections = 0` (sin flecha).
- [ ] **Step 2:** sube versión y familia de dispositivos.
- [ ] **Step 3:** `./gradlew check` → BUILD SUCCESSFUL; `./gradlew :composeApp:iosSimulatorArm64Test` → PASS.
- [ ] **Step 4:** prueba manual en iPad Pro 13" (simulador): compartir un cartel (hoja anclada, sin cierre), Llamar/Correo, ventana redimensionada con Stage Manager (la cuadrícula pasa de 3 a 2 columnas sin saltos), modo oscuro. Android: regresión rápida en teléfono (2 columnas, márgenes iguales a antes).
- [ ] **Step 5:** auditoría rápida ux-audit de cierre con capturas antes/después.
- [ ] **Step 6:** commit `feat(ios): support iPad again with centered layout`.

### Task 6: Publicación 2.1.0

- [ ] **Step 1:** capturas para App Store: iPhone 6,3" (1206×2622) e iPad 13" (2064×2752) de Inicio, Buscar, Ficha, Reportar y Mis carteles, sin anuncios de prueba visibles, en `docs/capturas/ios-store/` (ignorada por git).
- [ ] **Step 2:** archivar y subir con `iosApp/build-release/ExportOptions.plist` (como la build 5); esperar "Lista para enviar".
- [ ] **Step 3:** crear la versión 2.1.0 en App Store Connect, notas de la versión ("Ahora también en iPad"), subir capturas de iPad, seleccionar la build. Detenerse antes de "Enviar a revisión" y pedir confirmación al usuario.
- [ ] **Step 4:** Android: el mismo cambio llega a tablets; generar el bundle con el nuevo versionCode solo si se publica también en Play.
