# Cazadores Perú — Análisis del proyecto Flutter (base para migrar a KMP)

> Fecha: 2026-11-16 · Fuente: lectura del código en `lib/`, `android/`, `ios/`, `assets/`.
> No verificado contra la API en vivo (la prueba con `curl` fue bloqueada en esta sesión): los contratos salen del código y de los comentarios de los DTO.

## 1. Qué es

App móvil que consume la API pública del **Programa de Recompensas del MININTER** (la misma que usa `recompensas.pe`) para listar, buscar y ver la ficha de personas requisitoriadas con recompensa, guardarlas como favoritas y orientar al usuario a reportar por la línea oficial.

- Flutter, Clean Architecture + MVVM, Riverpod (generator), Freezed, Dio, Isar 3, GoRouter.
- ~5 900 líneas Dart propias (sin generados). Tests prácticamente vacíos (`test/widget_test.dart`).
- `applicationId`: `com.cazadores.cazadores_peru` · nombre visible: **CazadoresPe** / "Cazadores Perú" · versión `1.0.0+1`.

## 2. API (URLs y contratos)

**Base URL:** `https://sispasvehapp.mininter.gob.pe/api-recompensas`

Cabeceras que envía la app (imitan al navegador de recompensas.pe — ver riesgos):
```
Content-Type: application/json; charset=UTF-8
Accept: application/json, text/plain, */*
Accept-Language: es-PE,es;q=0.9,en;q=0.8
Origin: https://recompensas.pe
Referer: https://recompensas.pe/
User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148
```
Timeouts: connect 30 s · receive 60 s · send 30 s. Reintento: hasta 3 veces solo en connect/receive timeout, backoff lineal (1 s, 2 s, 3 s).

| Método | Ruta | Request | Response | Usado en |
|---|---|---|---|---|
| POST | `/requisitoriados/pageandfilter` | `SearchRequest` | `SearchResponse` (Page de Spring) | Buscar, stats del Home, (mapa, deshabilitado) |
| POST | `/requisitoriados/top5` | `{}` | `List<CriminalSummary>` | Home "Más buscados" |
| GET | `/requisitoriados/{hash}` | — | `CriminalDetail` | Ficha |
| GET | `/delitos` | — | `List<Delito>` | **No se usa** (se lee de assets) |
| GET | `/ubigeo/departamentos` | — | `List<Ubigeo>` | **No se usa** (assets) |
| GET | `/ubigeo/provincias/departamento/{id}` | — | `List<Ubigeo>` | **No se usa** (assets) |

### Modelos (JSON)

```jsonc
// SearchRequest
{
  "pageInfo": { "page": 1, "size": 20, "sortBy": "id", "direction": "desc" },
  "search": {
    "nombreCompleto": null, "tipoFilter": "F", "alias": null,
    "idDepartamento": null, "idProvincia": null,
    "idDelito": null, "delito": null, "sexo": null
  }
}
// sortBy usados: "id" (más reciente), "montoRecompensa" (mayor recompensa), "departamento" (asc)

// SearchResponse (Spring Page)
{ "content": [CriminalSummary], "totalElements": 0, "totalPages": 0,
  "number": 0, "last": false, "first": true, "numberOfElements": 0, "empty": false }

// CriminalSummary (lista y top5)
{ "id": 1, "idRequisitoriado": 123, "hashRequisitoriado": "abc…",
  "nombreCompleto": "hugo soria flores", "foto": "<base64 o data-url>",
  "montoRecompensa": 50000.0, "montoRecompensaSpace": "50 000", "delito": "SICARIATO" }

// CriminalDetail (GET /{hash})
{ "idRequisitoriado": 123, "hashRequisitoriado": "abc…",
  "apellidoPaterno": "", "apellidoMaterno": "", "nombres": "",
  "sexo": 1, "montoRecompensa": 50000.0, "montoRecompensaSpace": "50 000",
  "foto": "<base64>", "delitos": ["…"], "departamento": "LIMA", "provincia": "LIMA" }

// Delito          { "idDelito": 82, "descripcion": "ABORTO" }
// Departamento    { "departamento": "15", "provincia": "00", "descripcion": "LIMA" }
// Provincia       { "departamento": "15", "provincia": "01", "descripcion": "LIMA" }
```

- **Fotos:** vienen en **Base64** dentro del JSON (pueden traer prefijo `data:image/...;base64,`). No hay URL de imagen.
- **`sexo`**: int (1 por defecto). En el filtro se manda como string.
- **Paginación:** Spring es 0-indexed. Ver bug B1.

### Catálogos locales (`assets/data/`)
`delitos.json` (20 ítems), `departamentos.json` (25), `provincias.json` (86 — **incompleto**: Perú tiene 196 provincias).

### Otras URLs
| Uso | Valor |
|---|---|
| MININTER | `https://www.mininter.gob.pe` |
| PNP | `https://www.gob.pe/pnp` |
| Recompensas | `https://recompensas.pe` |
| Línea de denuncias | `0-800-40-007` (`tel:080040007`) |
| Reporte por correo | `mailto:denuncias@mininter.gob.pe` (no verificado) |
| Feedback | `mailto:app@recompensas.pe` (dominio de terceros — no es tuyo) |
| Deep link | `cazadores://criminal/{hash}` |

## 3. AdMob

**No hay AdMob integrado.** Solo existen constantes en `lib/core/constants/app_constants.dart` y son los **IDs de prueba públicos de Google**, no claves reales:

| Clave | Android | iOS |
|---|---|---|
| App ID | `ca-app-pub-3940256099942544~3347511713` | `ca-app-pub-3940256099942544~1458002511` |
| Banner | `ca-app-pub-3940256099942544/6300978111` | `ca-app-pub-3940256099942544/2934735716` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` | `ca-app-pub-3940256099942544/4411468910` |

No hay `google_mobile_ads` en `pubspec.yaml`, ni `com.google.android.gms.ads.APPLICATION_ID` en el Manifest, ni `GADApplicationIdentifier`/`SKAdNetworkItems`/`NSUserTrackingUsageDescription` en `Info.plist`, ni UMP/ATT. En KMP hay que crear las unidades reales en la consola de AdMob e integrarlo desde cero.

## 4. Funciones actuales

| Pantalla | Qué hace | Estado |
|---|---|---|
| Onboarding (3 slides) | Programa · Buscador · Reporta seguro. Guarda `onboarding_done` | OK |
| Home | Header con buscador falso (lleva a Buscar), stats (total + "recompensa máxima"), Top 5 en carrusel, info del programa, accesos rápidos (MININTER, PNP, Recompensas.pe), disclaimer | Stats con bug B2 |
| Buscar | Búsqueda por nombre, orden (reciente / recompensa / región), hoja de filtros (departamento → provincia, delito, sexo), scroll infinito, pull-to-refresh, banner offline, shimmer, favorito en tarjeta | Ver B1, B3 |
| Ficha | Foto (Hero), recompensa, nivel de peligrosidad (calculado localmente por palabras clave del delito), delitos, info general, consejos de seguridad, llamar 0-800, compartir texto, guardar, reportar avistamiento | OK |
| Reportar avistamiento (bottom sheet) | Textarea → arma correo `mailto:` con nombre, ID y descripción; botón llamar | Sin foto ni ubicación |
| Guardados | Lista reactiva de favoritos (Isar), quitar | B3 |
| Perfil / Ajustes | Tema (claro/oscuro/sistema), switches de notificaciones, links legales (sin destino real), feedback por mail, versión | Notificaciones no hacen nada |
| Mapa de calor | Agrupa por departamento | **Deshabilitado** (y la lista no trae departamento) |

**Persistencia:** SharedPreferences (`onboarding_done`, `theme_mode`, `notifications_enabled`, `selected_departamento`, `language`). Isar: `FavoriteSchema` y `CachedCriminalSchema` (máx. 100, TTL 24 h, clave = filtros concatenados con `|`).

**Peligrosidad (regla local):** EXTREMO si algún delito contiene TERRORISMO, HOMICIDIO CALIFICADO, ASESINATO, SICARIATO, FEMINICIDIO; MUY ALTO si contiene ROBO AGRAVADO, SECUESTRO, TRATA DE PERSONAS, VIOLACIÓN SEXUAL, EXTORSIÓN; si no, ALTO. **No es un dato oficial** → la UI debe decirlo.

## 5. Flujos

```
Arranque → ¿onboarding_done? ─no→ Onboarding (3) → Home
                              └sí→ Home
Home → buscador / "ver todos" → Buscar
Home → tarjeta Top 5 ─────────┐
Buscar → filtros/orden → lista → tarjeta ─→ Ficha /criminal/{hash}
Deep link cazadores://criminal/{hash} ────┘
Ficha → Llamar 0-800 | Compartir | Guardar | Reportar → mailto
Guardados → Ficha
Perfil → tema · links · feedback
Sin red → Buscar usa caché de la 1.ª página para esos filtros
```

## 6. Bugs y riesgos encontrados (llevar corregidos a KMP)

| # | Sev. | Problema | Dónde |
|---|---|---|---|
| B1 | Alto | Paginación: se envía `page: filters.page` (1-indexed) sin restar 1 y se devuelve `currentPage: response.number` sin sumar 1. Contradice el CLAUDE.md. Si la API es 0-indexed, **la primera página nunca se muestra**. Verificar con la API real | `criminal_repository_impl.dart` |
| B2 | Medio | "Recompensa máxima" se calcula con 1 solo registro ordenado por `id` → muestra la recompensa del último, no la máxima. Usar `sortBy=montoRecompensa&size=1` | `home_provider.dart` |
| B3 | Alto | Favoritos y caché guardados desde la **lista** pierden el nombre: se guardan apellidos vacíos y luego se reconstruye `nombreCompleto` desde ellos → nombre en blanco offline/en Guardados. Guardar `nombreCompleto` siempre | `criminal_repository_impl.dart` |
| B4 | Medio | Caché con `@Index(unique: true)` en `hash` compartido entre búsquedas: si una persona aparece en dos búsquedas el `putAll` choca y el error se traga | `cached_criminal_schema.dart` |
| B5 | Medio | Teléfono inconsistente: onboarding dice **1818**, el resto **0-800-40-007**. Confirmar cuál es la línea vigente del programa | `onboarding_screen.dart` |
| B6 | Medio | Switches de notificaciones ("alerta cuando un favorito es capturado") no tienen backend: promesa falsa | `profile_screen.dart` |
| B7 | Medio | Permisos sin uso: CAMERA, READ/WRITE_EXTERNAL_STORAGE, ACCESS_FINE/COARSE_LOCATION, CALL_PHONE (`tel:` con DIAL no lo necesita), RECEIVE_BOOT_COMPLETED. Play los observa | `AndroidManifest.xml` |
| B8 | Alto | SSL: pin del certificado **hoja** (`b7fc7a69…17ce`). Cuando MININTER renueve el cert, la app deja de funcionar en todos los teléfonos hasta publicar update. Mejor: incluir la CA intermedia que falta como trust anchor solo para ese dominio | `dio_client.dart` |
| B9 | Alto (legal/tienda) | `pubspec` dice "App oficial del Programa…" y la UI dice "Información oficial del Estado Peruano". Si no eres el MININTER, Google Play/App Store lo tratan como **suplantación de entidad gubernamental**. Además se falsifican `Origin`/`Referer`/`User-Agent` de recompensas.pe. Hace falta: aviso claro "app no oficial, datos de fuente pública", citar la fuente y, idealmente, permiso o confirmación de que la API puede consumirse | varios |
| B10 | Bajo | Correo de feedback en `app@recompensas.pe` (dominio ajeno). Links de Términos/Privacidad sin destino; Play exige política de privacidad real (y más con AdMob) | `profile_screen.dart` |
| B11 | Bajo | `provincias.json` con 86 de 196 provincias | `assets/data` |
| B12 | Bajo | Mapa de calor depende de `departamento`, que la lista no trae → requeriría N llamadas al detalle | `map_provider.dart` |
| B13 | Bajo | Clave de Google Maps en el Manifest vía placeholder; ya no se usa (mapa apagado) | Manifest / AppDelegate |
