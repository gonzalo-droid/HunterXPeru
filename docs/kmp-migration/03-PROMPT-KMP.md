# Prompt para el agente que construirá la versión KMP

> Copia todo lo que está debajo de la línea en una sesión nueva. Si el agente trabaja en otro repo, copia también `01-ANALISIS.md` y `02-DISENO-UX.md` a `docs/` de ese repo.

---

Eres un ingeniero senior de Kotlin Multiplatform. Vas a construir desde cero **Cazadores Perú**, una app Android + iOS con **Kotlin Multiplatform + Compose Multiplatform (UI compartida)**, que reemplaza a una app Flutter existente. Trabaja en español en los textos de la app y en la documentación.

## 0. Contexto

La app consume la API **pública** del Programa de Recompensas del Ministerio del Interior del Perú (la que usa recompensas.pe) para mostrar a las personas requisitoriadas con recompensa, buscarlas y filtrarlas, ver su ficha, guardarlas y orientar al usuario a reportar por la línea gratuita **sin enfrentarse**. La app **no es oficial**: debe decirlo claramente (onboarding, Inicio, Ficha, Ajustes, ficha de tienda).

Documentos de referencia (léelos primero si están en `docs/`): `01-ANALISIS.md` (API, modelos, flujos, bugs de la versión Flutter) y `02-DISENO-UX.md` (diseño, tokens y fichas de vista). Mockups: https://claude.ai/artifact/4K7mQQAxC2JonCtmFKDBKf (si no puedes abrirlo, guíate por `02-DISENO-UX.md`).

## 1. Stack (usa la última versión estable de cada uno y verifícala antes de fijarla en `libs.versions.toml`)

- Kotlin 2.x, Gradle Kotlin DSL, version catalog.
- Compose Multiplatform (Material 3) para toda la UI; `composeResources` para strings, fuentes e íconos.
- Ktor Client (OkHttp en Android, Darwin en iOS) + kotlinx.serialization + ContentNegotiation + Logging (solo debug) + HttpRequestRetry.
- Room KMP (con bundled SQLite) para favoritos y caché. DataStore Preferences KMP para ajustes.
- Koin para DI. `androidx.lifecycle` ViewModel KMP + StateFlow. Navigation Compose multiplataforma con rutas type-safe.
- Coroutines, kotlinx-datetime.
- Tests: kotlin.test, Turbine, Ktor MockEngine; Room en memoria.
- iOS: app SwiftUI mínima (`iosApp/`) que hospeda `MainViewController()` de Compose. Mínimo iOS 15. Android minSdk 24, target el último exigido por Play.

## 2. Estructura

```
composeApp/
  src/commonMain/kotlin/pe/cazadores/
    core/        (config, result, dispatchers, formatters, peligrosidad)
    data/remote/ (ApiService Ktor, DTOs)
    data/local/  (Room DB, DAOs, DataStore)
    data/repository/
    domain/      (modelos, interfaces de repositorio, casos de uso)
    ui/          (theme, components, screens/<pantalla>/{Screen,ViewModel,UiState})
    ads/         (expect/interfaces AdMob)
  src/androidMain/  (engine OkHttp, AdMob Android, UMP, launcher tel/mailto/share)
  src/iosMain/      (engine Darwin, puente AdMob, launcher)
iosApp/  (SwiftUI host, GoogleMobileAds + UMP por SPM, ATT)
```
Arquitectura: Clean Architecture + MVVM unidireccional (UiState inmutable + eventos). Ningún `ViewModel` llama a Ktor ni a Room directo.

## 3. API

Base URL: `https://sispasvehapp.mininter.gob.pe/api-recompensas`

| Método | Ruta | Body | Respuesta |
|---|---|---|---|
| POST | `/requisitoriados/pageandfilter` | `SearchRequest` | Page de Spring con `content: [CriminalSummaryDto]` |
| POST | `/requisitoriados/top5` | `{}` | `[CriminalSummaryDto]` |
| GET | `/requisitoriados/{hash}` | — | `CriminalDetailDto` |
| GET | `/delitos` · `/ubigeo/departamentos` · `/ubigeo/provincias/departamento/{id}` | — | catálogos |

```jsonc
// SearchRequest
{ "pageInfo": { "page": 0, "size": 20, "sortBy": "id", "direction": "desc" },
  "search": { "nombreCompleto": null, "tipoFilter": "F", "alias": null, "idDepartamento": null,
              "idProvincia": null, "idDelito": null, "delito": null, "sexo": null } }
// sortBy: "id" (más reciente, desc), "montoRecompensa" (desc), "departamento" (asc)

// Page:  { content, totalElements, totalPages, number, last, first, numberOfElements, empty }

// CriminalSummaryDto: { id?, idRequisitoriado:Int, hashRequisitoriado:String, nombreCompleto:String="",
//   foto:String?(Base64, puede traer prefijo data:), montoRecompensa:Double=0, montoRecompensaSpace:String="", delito:String="" }

// CriminalDetailDto: { idRequisitoriado, hashRequisitoriado, apellidoPaterno="", apellidoMaterno="", nombres="",
//   sexo:Int=1, montoRecompensa, montoRecompensaSpace, foto?, delitos:[String]=[], departamento="", provincia="" }

// Delito {idDelito:Int, descripcion} · Ubigeo {departamento:"15", provincia:"00"|"01", descripcion}
```

Reglas:
- `Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }`. Todo campo con default.
- **Paginación:** antes de escribir el repositorio, haz una llamada real a `pageandfilter` con `page: 0` y con `page: 1` y confirma si la API es 0- o 1-indexed. La UI es 1-indexed; traduce en el repositorio y cúbrelo con un test. (La app Flutter tenía este punto mal.)
- Cabeceras: `Accept: application/json`, `Content-Type: application/json`, `Accept-Language: es-PE`. **Primero prueba sin** `Origin`/`Referer`/`User-Agent` falsos; solo si la API rechaza la petición, agrega `Origin`/`Referer: https://recompensas.pe` y documenta por qué. Un `User-Agent` propio honesto (`CazadoresPeru/<versión> (<plataforma>)`).
- Timeouts: connect 30 s, request 60 s. Reintentos: 3, solo en timeout/IOException, backoff exponencial.
- Las fotos Base64: decodificar en `commonMain` con `kotlin.io.encoding.Base64` (quitar prefijo `data:…,`) y mostrar con `decodeToImageBitmap()`; cachear el `ImageBitmap` decodificado en memoria (LRU) para que el scroll sea fluido. Fallback: silueta.
- Catálogos: incluye los JSON de departamentos, provincias y delitos en `composeResources/files` como respaldo, pero intenta primero los endpoints y cachea 7 días. `provincias.json` de la versión Flutter está incompleto (86 de 196): complétalo desde `/ubigeo/provincias/departamento/{id}`.

### SSL (bloqueante en Android)
El servidor envía una cadena TLS incompleta (falta la CA intermedia). **No** desactives la validación ni pines el certificado hoja (la app Flutter pinea `b7fc7a697f58e66a5edc42ef4bf5c31698592f2251d0e96a753974dce5ec17ce` y se romperá cuando renueven el cert). Haz esto:
1. `openssl s_client -connect sispasvehapp.mininter.gob.pe:443 -showcerts` → identifica el emisor del cert hoja y descarga su CA intermedia oficial (URL en el campo AIA "CA Issuers").
2. Android: `res/raw/<intermedia>.pem` + `network_security_config.xml` con `<domain-config>` solo para `sispasvehapp.mininter.gob.pe` que confíe en `system` + ese certificado. Sin `<certificates src="user"/>` en release.
3. iOS: probar primero sin nada (suele completar la cadena vía AIA). Si falla, `handleChallenge` en el engine Darwin anclando esa intermedia.
4. Test instrumentado/manual en **release** en ambas plataformas.

## 4. Dominio

- `Criminal(id, hash, displayName, apellidoPaterno?, apellidoMaterno?, nombres?, sexo?, reward: Double, rewardText, photoBase64?, crimes: List<String>, departamento?, provincia?)`. `displayName`: si hay apellidos → `"PATERNO MATERNO, NOMBRES"`, si no → `nombreCompleto` en mayúsculas. **Siempre persistir `displayName`** (la versión Flutter perdía el nombre al guardar desde la lista).
- `SearchFilters(query, alias, departamentoId, provinciaId, delitoId, sexo, sort: Sort, page)`.
- `DangerLevel { EXTREMO, MUY_ALTO, ALTO }`: EXTREMO si algún delito contiene TERRORISMO, HOMICIDIO CALIFICADO, ASESINATO, SICARIATO, FEMINICIDIO; MUY_ALTO si contiene ROBO AGRAVADO, SECUESTRO, TRATA DE PERSONAS, VIOLACIÓN SEXUAL, EXTORSIÓN; si no, ALTO. Normaliza tildes y mayúsculas antes de comparar. Es estimado por la app: la UI lo dice.
- Formato de recompensa: `S/ 50 000` (espacio como separador de miles); `0` → "Sin recompensa".
- Stats de Inicio: total = `totalElements` de una búsqueda vacía; recompensa máxima = primer ítem con `sortBy=montoRecompensa desc, size=1`.
- Casos de uso: `GetTopWanted`, `SearchCriminals` (paginado), `GetCriminalDetail`, `ObserveFavorites`, `ToggleFavorite`, `IsFavorite`, `GetCatalogs`, `GetHomeStats`.

## 5. Persistencia y offline
- Room: `FavoriteEntity` (hash PK, todos los campos de `Criminal` + `savedAt`). Al guardar desde la lista, intenta traer el detalle en segundo plano para completar los campos.
- Caché de búsqueda: tabla `SearchCacheEntity(searchKey, position, hash, json, cachedAt)` con PK compuesta `(searchKey, hash)` — una persona puede estar en varias búsquedas. Máx. 100 filas por clave más usada / TTL 24 h. Caché del detalle por hash (TTL 24 h).
- Offline: `NetworkMonitor` (expect/actual) → si no hay red, mostrar caché con banner "Sin conexión · datos del <fecha>". Si no hay caché, estado de error con reintentar.
- DataStore: `onboardingDone`, `themeMode`.

## 6. Pantallas (detalle en `02-DISENO-UX.md`)
Bottom bar: **Inicio · Buscar · Guardados · Ajustes**. Ficha fuera de la barra. Filtros y Reportar como `ModalBottomSheet`.
1. **Onboarding** (3 pasos; el 3.º: "Nunca te enfrentes. Llama." + aviso no oficial).
2. **Inicio**: header oscuro con buscador (lleva a Buscar con foco), tarjeta "Llamar 0-800-40-007", 2 stats, carrusel top5, banner AdMob, aviso de fuente.
3. **Buscar**: campo con debounce 400 ms, botón filtros con contador, chips activos removibles, "N resultados", selector de orden, lista paginada (cargar más al llegar a 5 del final), pull-to-refresh, banner cada 8 ítems.
4. **Filtros**: departamento → provincia dependiente, delito (chips), sexo (segmentado), CTA "Ver resultados", "Limpiar".
5. **Ficha** `criminal/{hash}`: foto, ID, recompensa, nombre, peligrosidad + nota, delitos, datos, "Si lo reconoces", fuente y fecha; top bar volver/compartir/guardar; CTA fijo **Llamar** + botón **Reportar por escrito**. Sin anuncios. Estados: cargando, error, no encontrado.
6. **Reportar**: llamar (primario) o formulario dónde/cuándo/detalles → `mailto:` con asunto y cuerpo (nombre, ID, texto). Confirmar con el usuario el correo destino (la versión Flutter usaba `denuncias@mininter.gob.pe` sin verificar; déjalo en una constante y márcalo TODO si no se confirma). No se guarda ni se envía nada desde la app.
7. **Guardados**: lista reactiva, swipe para quitar con "Deshacer", estado vacío.
8. **Ajustes**: tema, preferencias de anuncios (UMP), política de privacidad, términos, fuente de datos, enviar comentarios (correo propio configurable, no `@recompensas.pe`), "Sobre esta app" (no afiliada), versión.

Compartir: texto con nombre, recompensa, "Si tienes información llama al 0-800-40-007" y deep link. Deep links: `cazadores://criminal/{hash}` en Android (intent-filter) e iOS (URL scheme) → Ficha.

Línea de denuncias: una sola constante `REPORT_PHONE = "0-800-40-007"`, `tel:080040007` con acción DIAL (no pidas `CALL_PHONE`). La versión Flutter también mostraba "1818": no lo uses salvo que se confirme.

## 7. Diseño (tokens)
Implementa `CazadoresTheme` con estos tokens (claro / oscuro):
`bg #F3F1EC/#121315` · `surface #FFFFFF/#1C1E21` · `ink #16181B/#EDEBE6` · `inkMuted #4A4F57/#B9B5AD` · `line #DAD6CE/#2E3136` · `action #B4121B/#E5484D` · `reward #F2B93B` (texto siempre `#16181B`) · niveles: EXTREMO `#8E1B14`/blanco, MUY_ALTO `#B0480C`/blanco, ALTO `#EADFB8`/`#3D3200`.
Tipografía: **Archivo** variable (condensada 70 %, peso 800, mayúsculas para nombres/montos/títulos; 400/600 cuerpo, mín. 15 sp) e **IBM Plex Mono** para IDs/fechas, empaquetadas en `composeResources/font`. Radios 12–16 dp; botones 52–56 dp; áreas táctiles ≥ 48 dp.
Accesibilidad: contraste ≥ 4.5:1, nivel con texto y no solo color, `contentDescription` en íconos, respetar escala de fuente del sistema hasta 2.0× sin cortar textos, orden de foco lógico, Dynamic Type en iOS.

## 8. AdMob
Hoy **no** existe integración ni claves reales (solo los IDs de prueba de Google). Implementa:
- `expect`/interfaz `AdsController` + `@Composable AdBanner(placement)`. Android: `google-mobile-ads` + UMP con `AndroidView` (banner adaptativo anclado). iOS: GoogleMobileAds + UserMessagingPlatform por SPM en `iosApp`; expón desde Swift una fábrica de `UIView` de banner que Kotlin consume vía `UIKitView` (patrón de factory inyectada desde Swift).
- Orden: UMP → (iOS) ATT con `NSUserTrackingUsageDescription` → `MobileAds.initialize` → cargar anuncios. Reabrir formulario desde Ajustes.
- IDs: en debug usa los IDs de prueba de Google; en release léelos de `local.properties`/`Secrets.xcconfig` **no versionados** (`ADMOB_APP_ID_ANDROID`, `ADMOB_BANNER_ANDROID`, `ADMOB_APP_ID_IOS`, `ADMOB_BANNER_IOS`). Deja `.example` de ambos. Manifest: `com.google.android.gms.ads.APPLICATION_ID`; Info.plist: `GADApplicationIdentifier` + `SKAdNetworkItems`.
- Ubicaciones: Inicio (banner) y Buscar/Guardados (cada 8 ítems). Nunca en Ficha, Reportar ni Onboarding. Sin interstitials.

## 9. Permisos y tienda
- Android: solo `INTERNET` y `ACCESS_NETWORK_STATE` (+ `AD_ID` por AdMob). Nada de cámara, ubicación, almacenamiento, CALL_PHONE ni BOOT_COMPLETED.
- Si la app Flutter ya está publicada en Play: conserva `applicationId = com.cazadores.cazadores_peru` y sube `versionCode`. Los favoritos de Isar no se migran: avisarlo en las notas de versión.
- Nombre en tienda y descripción sin "oficial"; incluir "App independiente, no afiliada al Ministerio del Interior. Fuente: datos públicos del Programa de Recompensas". Política de privacidad propia (necesaria por AdMob).

## 10. Calidad
- Tests unitarios: mapeo DTO→dominio, `displayName`, peligrosidad (con tildes), formato de recompensa, traducción de paginación, repositorio con MockEngine (éxito, 404, timeout, offline→caché), DAO de favoritos/caché, ViewModels con Turbine.
- `./gradlew check` en verde; build de release Android firmado con clave de debug para probar; build iOS en simulador.
- Prueba en emulador Android (compacto + escala 2.0, oscuro) y simulador iOS (pequeño + grande, Dynamic Type XL). Captura cada pantalla.
- Sin `!!`, sin `GlobalScope`, sin lógica en Composables. Strings en `composeResources/values/strings.xml`.

## 11. Entregables y forma de trabajo
1. Primero un plan corto por fases y espera mi OK.
2. Fases sugeridas: (a) esqueleto KMP + tema + navegación; (b) red + SSL + verificación de paginación; (c) dominio + repositorios + Room/DataStore; (d) pantallas en orden Buscar → Ficha → Inicio → Guardados → Ajustes → Onboarding → sheets; (e) AdMob + UMP + ATT; (f) deep links + compartir; (g) tests, accesibilidad y capturas.
3. Un commit por paso lógico con Conventional Commits, **sin** atribución de IA (nada de `Co-Authored-By` ni "Generated with").
4. `README.md` (setup, secretos, cómo correr en ambas plataformas) y `CLAUDE.md` con comandos y decisiones.
5. Al terminar: lista de lo verificado, lo no verificado y los TODO abiertos (correo de denuncias, teléfono 1818, IDs reales de AdMob, permiso de uso de la API).

Criterios de aceptación: en **release** en Android y en iOS se puede buscar, filtrar, paginar, abrir una ficha, llamar, compartir, guardar/quitar favoritos, ver datos offline y ver banners de prueba tras el consentimiento; todos los tests pasan.
