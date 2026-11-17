# Rediseño "caza recompensas" — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cambiar la piel de la app al estilo "cartel SE BUSCA" con rango de recompensa, medidor de peligro, cartel compartible como imagen y pantalla del régimen especial, sin desbordes de texto en Android ni iOS.

**Architecture:** Reglas nuevas en `core` (puras, con tests en `commonTest`); tokens de papel en `CazadoresColors`; componentes en `ui/components`; cada pantalla se cambia en su propia tarea. Compartir imagen agrega un método a `ExternalActions` con implementación Android/iOS.

**Tech Stack:** Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material3 1.9.0, Koin, DataStore, Room.

**Spec:** `docs/superpowers/specs/2026-11-17-rediseno-caza-recompensas-design.md`

## Global Constraints
- Colores y tipos solo vía `Cz.colors` / `Cz.type`; textos solo en `composeResources/values/strings.xml`.
- Sin fuentes nuevas: estilo cartel con `Cz.type.display/headline/title/amount/amountSmall` (Archivo condensada 800).
- `display` (34 sp) es el techo y solo para "SE BUSCA" de la ficha; miniaturas usan `amountSmall`/`caption`/`overline`.
- Ningún contenedor de texto con alto fijo (solo fotos). Nombres en listas/miniaturas `maxLines = 2` + elipsis; franja de delito `maxLines = 2`; montos `maxLines = 1` sin partir (si no caben, bajar a `amountSmall`).
- Sellos (rango, peligro) dentro de `FlowRow`, nunca en `Row` rígida con texto.
- Contraste ≥ 4.5:1 (texto) en ambos temas; el cartel es **siempre papel**, igual en claro y oscuro.
- Aviso "no oficial" en onboarding, Inicio, Ficha, Ajustes y en la imagen compartida.
- Reportar no usa tono de cacería; mantiene "no te acerques".
- Commits Conventional Commits, **sin atribución de IA**.
- Cortes de clase: S ≥ 300 000, A ≥ 100 000, B ≥ 50 000, C > 0 (provisionales).

## Review Focus
- Nombre y delito muy largos con letra 2× en un iPhone SE: se ven en ≤ 2 líneas con elipsis en listas y completos en la ficha, sin encimarse con sellos → Task 4 (captura) y Task 10.
- Recompensa 0 / sin monto: no muestra sello de rango ni "S/ 0" → test en Task 1, uso en Task 4.
- Persona sin delitos o con delitos que no coinciden con ninguna palabra clave: titular = primer delito (o ninguno), medidor dice "Nivel base" → test en Task 2.
- Compartir sin foto (foto nula o inválida): la imagen sale con la silueta, no falla → Task 6.
- "Cerca de ti" sin departamento elegido: muestra la invitación a elegir, no una lista vacía ni error → test en Task 7.

---

### Task 1: Rango de recompensa

**Files:**
- Create: `composeApp/src/commonMain/kotlin/pe/cazadores/core/RewardClassCalculator.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/domain/model/Criminal.kt` (agregar enum)
- Test: `composeApp/src/commonTest/kotlin/pe/cazadores/core/CoreRulesTest.kt`

**Interfaces:**
- Produces: `enum class RewardClass(val stars: Int) { S(4), A(3), B(2), C(1) }` en `Criminal.kt`; `object RewardClassCalculator { fun of(reward: Double): RewardClass? }` (null si `reward <= 0`).

- [ ] **Step 1: Test `RewardClassCalculatorTest`**
```kotlin
@Test fun bordersAreInclusive() {
    assertEquals(RewardClass.S, RewardClassCalculator.of(300_000.0))
    assertEquals(RewardClass.A, RewardClassCalculator.of(299_999.0))
    assertEquals(RewardClass.A, RewardClassCalculator.of(100_000.0))
    assertEquals(RewardClass.B, RewardClassCalculator.of(50_000.0))
    assertEquals(RewardClass.C, RewardClassCalculator.of(49_999.0))
    assertEquals(RewardClass.C, RewardClassCalculator.of(1.0))
}
@Test fun noRewardHasNoClass() {
    assertNull(RewardClassCalculator.of(0.0))
    assertNull(RewardClassCalculator.of(-5.0))
}
```
- [ ] **Step 2:** `./gradlew :composeApp:testAndroidHostTest --tests "pe.cazadores.core.RewardClassCalculatorTest"` → FAIL (no compila).
- [ ] **Step 3:** Implementar enum y calculador con los cortes de Global Constraints como constantes privadas.
- [ ] **Step 4:** Mismo comando → PASS.
- [ ] **Step 5:** Commit `feat(core): add reward class calculator`.

### Task 2: Peligro con delito determinante y régimen especial

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/core/DangerLevelCalculator.kt`
- Create: `composeApp/src/commonMain/kotlin/pe/cazadores/core/SpecialRegime.kt`
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/screens/detail/DetailViewModel.kt:92` (usar `assess`)
- Test: `composeApp/src/commonTest/kotlin/pe/cazadores/core/CoreRulesTest.kt`

**Interfaces:**
- Produces: `data class DangerAssessment(val level: DangerLevel, val headlineCrime: String?, val matched: Boolean)`; `DangerLevelCalculator.assess(crimes: List<String>): DangerAssessment`; `of(crimes)` se mantiene como `assess(crimes).level`. `object SpecialRegime { fun applies(crimes: List<String>): Boolean }` (palabras clave normalizadas: `EXTORSION`, `SICARIATO`).
- `DetailContent.Loaded.danger` pasa a ser `DangerAssessment`; agrega `specialRegime: Boolean`.

- [ ] **Step 1: Tests**
```kotlin
@Test fun headlineIsMostSevereThenApiOrder() {
    val a = DangerLevelCalculator.assess(listOf("ESTAFA", "EXTORSION", "SICARIATO", "FEMINICIDIO"))
    assertEquals(DangerLevel.EXTREMO, a.level); assertEquals("SICARIATO", a.headlineCrime); assertTrue(a.matched)
}
@Test fun noMatchFallsBackToFirstCrime() {
    val a = DangerLevelCalculator.assess(listOf("LAVADO DE ACTIVOS", "ESTAFA"))
    assertEquals(DangerLevel.ALTO, a.level); assertEquals("LAVADO DE ACTIVOS", a.headlineCrime); assertFalse(a.matched)
}
@Test fun emptyCrimes() {
    val a = DangerLevelCalculator.assess(emptyList())
    assertNull(a.headlineCrime); assertFalse(a.matched)
}
@Test fun specialRegimeOnlyForExtortionOrHitman() {
    assertTrue(SpecialRegime.applies(listOf("Extorsión agravada")))
    assertTrue(SpecialRegime.applies(listOf("ROBO", "SICARIATO")))
    assertFalse(SpecialRegime.applies(listOf("FEMINICIDIO")))
}
```
- [ ] **Step 2:** Correr `--tests "pe.cazadores.core.*"` → FAIL.
- [ ] **Step 3:** Implementar `assess` (headline = primer delito, en orden de la API, cuyo nivel coincide con el nivel final; si no hay coincidencia, el primer delito) y `SpecialRegime` reutilizando `normalizedForMatch()`. Actualizar `DetailViewModel` y `CriminalCard` (`of` sigue disponible).
- [ ] **Step 4:** `./gradlew :composeApp:testAndroidHostTest` → PASS (incluye `ViewModelsTest`).
- [ ] **Step 5:** Commit `feat(core): expose decisive crime and special regime rule`.

### Task 3: Tokens de papel y contraste

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/theme/Color.kt`
- Test: `composeApp/src/commonTest/kotlin/pe/cazadores/ui/theme/ContrastTest.kt`

**Interfaces:**
- Produces en `CazadoresColors` (mismo valor en `Light` y `Dark`): `paper`, `onPaper`, `onPaperMuted`, `paperLine`, `paperPhoto`, `posterRed`, `onPosterRed`, `rankS`, `onRankS`, `rankA`, `onRankA`, `rankB`, `onRankB`, `rankC`, `onRankC`. Valores de partida de los mockups: paper `#EFE4CC`, onPaper `#2B1D10`, onPaperMuted `#5A4630`, paperLine `#CDBF9F`, posterRed `#7A1D12`, onPosterRed `#FFF3E6`, rankS `#2B1D10`/`#F2B93B`, rankA `#7A1D12`/`#FFF3E6`, rankB `#B0480C`/`#FFF3E6`, rankC `#EADFB8`/`#3D3200`.

- [ ] **Step 1:** Agregar a `check()` los pares de texto `onPaper/paper`, `onPaperMuted/paper`, `posterRed/paper`, `onPosterRed/posterRed` y `onRankX/rankX` (×4), todos ≥ 4.5. `paperLine` y `paperPhoto` no llevan texto y no se prueban.
- [ ] **Step 2:** `--tests "pe.cazadores.ui.theme.ContrastTest"` → FAIL (no compila).
- [ ] **Step 3:** Agregar los campos con un `private val Paper = ...` compartido por ambos temas; ajustar valores si algún par queda < 4.5:1.
- [ ] **Step 4:** Mismo comando → PASS.
- [ ] **Step 5:** Commit `feat(theme): add paper poster and rank color tokens`.

### Task 4: Componentes del cartel

**Files:**
- Create: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/components/Poster.kt` (`WantedPoster`, `PosterMini`)
- Modify: `composeApp/src/commonMain/kotlin/pe/cazadores/ui/components/Badges.kt` (`RankBadge`, `DangerMeter`)
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: Task 1–3.
- Produces:
  - `@Composable fun RankBadge(rewardClass: RewardClass, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null)` — "S ★★★★", `contentDescription` "Clase S, 4 estrellas"; con `onClick` muestra ⓘ y es botón.
  - `@Composable fun DangerMeter(assessment: DangerAssessment, modifier: Modifier = Modifier)` — 3 barras + etiqueta + "Estimado por: <delito>" o "Nivel base…" + "No oficial".
  - `@Composable fun WantedPoster(criminal: Criminal, assessment: DangerAssessment, rewardClass: RewardClass?, onRankClick: (() -> Unit)?, modifier: Modifier = Modifier, photo: @Composable () -> Unit)` — "SE BUSCA" (`display`), franja "POR <DELITO>" (`title`, maxLines 2), "+ N delitos más", foto, nombre (`headline`), bloque recompensa+clase (`amount`).
  - `@Composable fun PosterMini(criminal: Criminal, onClick: () -> Unit, modifier: Modifier = Modifier)` — tarjeta clavada para tablón y Mis carteles; nombre maxLines 2, delito maxLines 1, `amountSmall`.
- Strings nuevas (prefijos `poster_`, `rank_`, `danger_meter_`): `poster_wanted` "SE BUSCA", `poster_for` "POR %1$s", `poster_more_crimes` (plural "+ %d delito más"/"+ %d delitos más"), `rank_label` "Clase %1$s", `rank_cd` "Clase %1$s, %2$d estrellas", `danger_meter_by` "Estimado por: %1$s", `danger_meter_base` "Nivel base: todos los buscados tienen delitos graves", `danger_meter_unofficial` "Estimado por la app. No es una calificación oficial."

- [ ] **Step 1:** Implementar componentes siguiendo Global Constraints (FlowRow para sellos; sin alturas fijas en texto).
- [ ] **Step 2:** Agregar `@Preview` con caso largo ("GONZALO EDUARDO MONTEVERDE BUSSALLEU", "VIOLACIÓN SEXUAL DE MENOR DE EDAD", 3 delitos, S/ 500 000) y caso sin recompensa (sin `RankBadge`).
- [ ] **Step 3:** `./gradlew :composeApp:testAndroidHostTest :composeApp:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit `feat(ui): add wanted poster, rank badge and danger meter`.

### Task 5: Ficha, leyenda y régimen especial

**Files:**
- Modify: `ui/screens/detail/DetailScreen.kt`, `ui/screens/detail/ReportSheet.kt`, `ui/navigation/Routes.kt`, `ui/navigation/AppNavHost.kt`, `strings.xml`
- Create: `ui/screens/legend/RankLegendSheet.kt`, `ui/screens/regime/SpecialRegimeScreen.kt`

**Interfaces:**
- Consumes: Task 2 (`DetailContent.Loaded.danger: DangerAssessment`, `specialRegime`), Task 4.
- Produces: `Route.SpecialRegime` (`@Serializable data object`); `@Composable fun RankLegendSheet(onDismiss: () -> Unit)`; `@Composable fun SpecialRegimeScreen(onBack: () -> Unit, onCall: () -> Unit, onOpenOfficial: () -> Unit)`. URL oficial en una constante `SPECIAL_REGIME_URL` (hoy `https://recompensas.pe/`, pendiente del enlace exacto).
- Copy fijo (spec §Pantallas 7): título "Recompensas por extorsión y sicariato", decreto "D. S. N.º 097-2025-PCM", "Tu recompensa puede ser hasta S/ 500 000", los 5 puntos del spec, aviso "Resumen hecho por una app no oficial. Lo que vale es el decreto y el Mininter." Reportar suma "No te acerques a la persona."

- [ ] **Step 1:** Ficha: reemplazar cabecera por `WantedPoster` (fondo `paper` en ambos temas); `RankBadge(onClick = abrir leyenda)`; aviso del régimen (borde punteado `posterRed`) solo si `specialRegime`; conservar "Si lo reconoces", barra Llamar/Reportar y aviso no oficial.
- [ ] **Step 2:** Leyenda (hoja inferior, papel) y pantalla del régimen; ruta en `AppNavHost`.
- [ ] **Step 3:** Compilar Android e iOS; instalar debug y capturar ficha, leyenda y régimen con fuente 1,0 y 2,0 (Android: `adb shell settings put system font_scale 2.0`; iOS: Accessibility XL). Ningún texto cortado.
- [ ] **Step 4:** Auditoría rápida `ux-audit` sobre las capturas.
- [ ] **Step 5:** Commit `feat(detail): poster layout, rank legend and special regime screen`.

### Task 6: Compartir cartel como imagen

**Files:**
- Modify: `platform/ExternalActions.kt`, `androidMain/.../AndroidExternalActions.kt`, `iosMain/.../IosExternalActions.kt`, `DetailScreen.kt`, `strings.xml`
- Create: `ui/screens/detail/SharePoster.kt`, `commonMain/.../platform/ImageEncoding.kt` (+ `actual` en android/ios), `androidApp/src/main/res/xml/share_paths.xml`
- Modify: `androidApp/src/main/AndroidManifest.xml` (`FileProvider`, authority `${applicationId}.share`)

**Interfaces:**
- Produces: `ExternalActions.shareImage(png: ByteArray, text: String)`; `expect fun ImageBitmap.encodePng(): ByteArray`; `@Composable fun SharePosterContent(criminal: Criminal, assessment: DangerAssessment, rewardClass: RewardClass?)` (incluye 0-800 y "Imagen no oficial · verifica en recompensas.pe").
- Render a 1080×1350 px con `CompositionLocalProvider(LocalDensity provides Density(density = …, fontScale = 1f))` y `rememberGraphicsLayer()` → `toImageBitmap()`. Android: escribe en `cacheDir/share/cartel.png`, `ACTION_SEND` `image/png` + `EXTRA_TEXT`. iOS: `UIImage(data:)` en `UIActivityViewController` con el texto.
- Fakes: agregar `shareImage` al fake de `ExternalActions` en `commonTest/.../testing/Fakes.kt` si existe allí.

- [ ] **Step 1:** Implementar; el botón compartir de la ficha usa imagen y cae al `share(text)` actual si el render falla.
- [ ] **Step 2:** Probar en emulador y simulador: compartir con foto y con persona sin foto (silueta). Verificar la imagen guardada a mano.
- [ ] **Step 3:** `./gradlew check` → OK.
- [ ] **Step 4:** Commit `feat(share): share wanted poster as image`.

### Task 7: Inicio tablón y "Cerca de ti"

**Files:**
- Modify: `data/local/SettingsDataSource.kt`, `domain/repository/Repositories.kt` (`SettingsRepository`), su implementación, `ui/screens/home/HomeViewModel.kt`, `HomeScreen.kt`, `di/Koin.kt`, `strings.xml`, `testing/Fakes.kt`
- Test: `commonTest/.../ui/screens/ViewModelsTest.kt`

**Interfaces:**
- Produces: `SettingsRepository.nearDepartamentoId: Flow<String?>` y `suspend fun setNearDepartamento(id: String?)` (clave DataStore `near_departamento`). `HomeUiState` agrega `nearDepartamento: Departamento?` y `near: List<Criminal>`; `HomeViewModel` recibe `SearchCriminalsUseCase`, `GetCatalogsUseCase`, `SettingsRepository` y expone `fun onPickDepartamento(id: String?)`. "Cerca de ti" = `search(SearchFilters(departamentoId = id, sort = SortOrder.REWARD), page = 1)`, primeros 6.

- [ ] **Step 1: Tests**
```kotlin
@Test fun homeNearbyEmptyUntilDepartmentPicked() = runTest(dispatcher) {
    // sin departamento: near vacío, nearDepartamento null, sin error
}
@Test fun homeNearbyLoadsByRewardForPickedDepartment() = runTest(dispatcher) {
    // onPickDepartamento("15") → el fake recibe SearchFilters(departamentoId="15", sort=REWARD) y near = sus items
}
```
- [ ] **Step 2:** Correr → FAIL.
- [ ] **Step 3:** Implementar VM y DataStore. UI: encabezado "%d SE BUSCAN", CallCard, banner régimen especial (→ `Route.SpecialRegime`), sección "Clase S ★★★★" con `PosterMini` del top, sección "Cerca de ti" (selector de departamento si no hay), aviso no oficial. Quitar el bloque de estadísticas duplicado si queda redundante con el encabezado.
- [ ] **Step 4:** Tests PASS + capturas 1,0 / 1,3 / 2,0 en Android e iOS.
- [ ] **Step 5:** Commit `feat(home): reward board with nearby section`.

### Task 8: Buscar, Filtros y Mis carteles

**Files:**
- Modify: `components/CriminalCard.kt`, `screens/search/SearchScreen.kt`, `screens/search/FiltersSheet.kt`, `screens/saved/SavedScreen.kt`, `strings.xml` (`tab_saved` → "Carteles", `saved_title` → "Mis carteles", textos vacíos acordes)

**Interfaces:**
- Consumes: Task 4 (`RankBadge`, `PosterMini`).
- `CriminalCard`: nombre (maxLines 2), delito titular en `posterRed` (maxLines 1), `FlowRow { RankBadge; monto }`. Filtros: solo piel nueva, sin filtro por clase. Mis carteles: grilla de `PosterMini` con la acción quitar/deshacer actual.

- [ ] **Step 1:** Implementar.
- [ ] **Step 2:** `./gradlew :composeApp:testAndroidHostTest` (incluye `savedRemoveThenUndoRestoresItem`) → PASS.
- [ ] **Step 3:** Capturas 1,0 / 2,0 de Buscar, Filtros y Mis carteles; pestañas ≤ 1,3×.
- [ ] **Step 4:** Commit `feat(ui): poster style for search, filters and saved`.

### Task 9: Onboarding, Ajustes y estados

**Files:**
- Modify: `screens/onboarding/OnboardingScreen.kt`, `screens/settings/SettingsScreen.kt`, `components/StateViews.kt`, `strings.xml`

**Interfaces:**
- Consumes: Task 5 (`RankLegendSheet`, `Route.SpecialRegime`).
- Onboarding: 3 páginas (recompensa con cartel, rangos y estrellas, aviso no oficial); saludo neutro. Ajustes: sección "Recompensas" con "Cómo funcionan las recompensas" (→ régimen) y "¿Qué significan los rangos?" (→ leyenda). Sin conexión: cartel "SIN SEÑAL", Reintentar, "El 0-800 funciona sin internet".

- [ ] **Step 1:** Implementar.
- [ ] **Step 2:** Capturas 1,0 / 2,0 de las tres; `./gradlew check` → OK.
- [ ] **Step 3:** Commit `feat(ui): onboarding, settings and empty states for new design`.

### Task 10: Verificación final

- [ ] **Step 1:** `./gradlew check` y `./gradlew :composeApp:iosSimulatorArm64Test` → OK.
- [ ] **Step 2:** `./gradlew :androidApp:assembleRelease` e instalar: la app no queda en blanco (regla R8).
- [ ] **Step 3:** Matriz de capturas en emulador angosto (≈ 320 dp) e iPhone SE: Ficha, Inicio, Buscar, Régimen, Leyenda × fuente 1,0 / 1,3 / 2,0 × tema claro/oscuro. Revisar los casos de Review Focus.
- [ ] **Step 4:** Actualizar `docs/capturas/` y agregar decisión D17 en `docs/DECISIONES.md` (rediseño aplicado, cartel siempre papel).
- [ ] **Step 5:** Commit `docs: record poster redesign and refresh screenshots`.
