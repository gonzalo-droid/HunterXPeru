# Decisiones y verificaciones

Registro de lo que se verificó contra la API real y de las decisiones que cambian o precisan
`docs/kmp-migration/`. La más reciente va arriba.

## 2026-11-16 · Ajustes tras la revisión

### D12. Denuncias: 0-800-40-007, 1818 y correo confirmados
El 1818 y `denuncias@mininter.gob.pe` quedan confirmados como vigentes. El 0-800-40-007 sigue siendo la
acción principal (gratuita); Reportar ofrece el 1818 como segunda opción.

### D13. "Calificar en la tienda" reemplaza "Enviar comentarios"
- Android abre `market://details?id=<paquete>` y, si no hay Play Store, la URL web de Play.
- iOS abre `itms-apps://…/id<ID>?action=write-review` cuando `AppConfig.IOS_APP_STORE_ID` tiene valor.
  Mientras esté vacío (la app aún no existe en App Store Connect) muestra la ventana de reseña del
  sistema (`SKStoreReviewController`), que Apple limita a 3 veces al año por usuario.

### D14. IDs reales de AdMob solo en release
`local.properties` (Android) y `Secrets.xcconfig` con `[config=Release]` (iOS). Debug sigue con los IDs
de prueba de Google: AdMob sanciona las impresiones y los toques propios sobre anuncios reales.

## 2026-11-16 · Fase 7: calidad

### D9. Sin red no se reintenta
El cliente reintenta 3 veces los errores transitorios (~14 s). Si `NetworkMonitor` dice que no hay red, el
repositorio va directo a la caché (o al error "sin conexión"). Verificado en emulador con modo avión.

### D10. Texto grande (Android 2×, iOS Accesibilidad XL)
- Las etiquetas de la barra de pestañas escalan hasta 1,3× (a 2× "Guardados" no cabe); el ícono y la
  descripción accesible identifican la pestaña.
- Filas "texto + acción" usan `FlowRow`; las cifras de Inicio se apilan con letra grande.

### D11. Release Android
R8 en modo completo eliminaba los campos de `GeneratedMessageLite` que DataStore lee por reflexión y la app
quedaba en blanco: regla de keep en `proguard-rules.pro`. Release verificado en emulador (búsqueda, ficha,
guardado, banner). Firmado con la clave de debug: falta la clave de subida real.

## 2026-11-16 · Fase 5: anuncios

### D7. Consentimiento antes de cualquier anuncio
- Orden: UMP → (iOS) ATT → `MobileAds` → anuncios. Se dispara al mostrar Inicio por primera vez
  (después del onboarding y con una vista en pantalla), nunca en el arranque de la app.
- `AdBanner` no ocupa espacio ni pide anuncios mientras `canRequestAds` sea `false`.
- Debug fuerza la geografía EEE para ver el formulario desde Perú. Ajustes muestra
  "Preferencias de anuncios" solo cuando UMP lo exige (`privacyOptionsRequirementStatus == REQUIRED`).
- iOS: GoogleMobileAds 13.13.0 y UMP 3.1.0 por SPM; Swift implementa `IosAdsBridge` y Kotlin pinta la
  vista con `UIKitView`. Android: play-services-ads 25.5.0 y UMP 4.0.0.
- Verificado en emulador y simulador: formulario UMP → ATT (iOS, texto en español) → banner de prueba,
  y reapertura del formulario desde Ajustes (Android).

### D8. Permisos que agrega el SDK de anuncios
La app declara `INTERNET`, `ACCESS_NETWORK_STATE` y `AD_ID`. El manifiesto final además trae, por
fusión con play-services-ads, `ACCESS_ADSERVICES_*` (Privacy Sandbox), `WAKE_LOCK`,
`FOREGROUND_SERVICE` y `ACCESS_LOCAL_NETWORK`. Son normales del SDK (sin diálogo para el usuario) y
quitarlos con `tools:node="remove"` puede romper la medición de anuncios: se dejan.

## 2026-11-16 · Fase 2: API y TLS (verificado con `curl` y en Android/iOS)

### D1. Paginación: 1-indexed al pedir, 0-indexed al responder
- `pageInfo.page = 0` → HTTP 500 `"Page index must not be less than zero!"`.
- `page = 1` → primera página, y la respuesta trae `number: 0`, `first: true`.
- La UI y el dominio usan páginas 1-indexed: se envía tal cual y la página actual es `number + 1`.
- El bug B1 de Flutter no estaba en lo que enviaba sino en devolver `number` sin sumarle 1 (pedía dos veces la página 1).

### D2. Cabeceras honestas
La API responde igual sin `Origin`/`Referer`/`User-Agent` de recompensas.pe. Se envía
`User-Agent: CazadoresPeru/<versión> (<plataforma>)`, `Accept: application/json`, `Accept-Language: es-PE`.

### D3. Filtros que funcionan y filtros que no
| Campo | Resultado |
|---|---|
| `nombreCompleto` | Funciona, sin distinguir mayúsculas |
| `idDepartamento` (`"15"`) | Funciona |
| `idProvincia` (`"01"`, junto con el departamento) | Funciona; `"1501"` da 0 |
| `idDelito` (número) o `delito` (texto) | Funcionan |
| `sexo` (`"1"`, `"2"`, `1`, `"M"`, `"F"`) | **Siempre 0 resultados** |
| `alias` (con `tipoFilter` `F` o `A`) | **Siempre 0 resultados** |
| `sortBy` `id`/`montoRecompensa` desc, `departamento` asc | Funcionan |

Decisión: la hoja de filtros **no** ofrece sexo y la búsqueda es solo por nombre. Un filtro que siempre
devuelve "0 resultados" engaña al usuario. La lista no trae el sexo, así que tampoco se puede filtrar en el cliente.

### D4. Otros contratos
- `GET /requisitoriados/{hash}` con hash inválido → 404 `{"message":"Datos no válidos."}` → estado "no encontrado".
- `top5` pesa ~830 KB (fotos Base64 JPEG/PNG; algunas sin prefijo `data:`).
- Catálogos: 105 delitos, 25 departamentos, 195 provincias. Se empaquetan completos en
  `composeResources/files/catalogs/` como respaldo (Flutter tenía 20 delitos y 86 provincias).

### D5. TLS sin pinning
- Hoy el servidor envía la cadena completa (hoja → Sectigo OV R36 → R46 → USERTrust RSA) y valida.
- El certificado hoja se **renovó el 2026-11-09** (SHA-256 `F6:0B:96:BE:…:E6:6E:E7:FD`). La app Flutter pinea
  `b7fc7a69…`, así que la versión publicada probablemente ya no conecta (no verificado con la app Flutter).
- Android: `network_security_config.xml` confía en las CA del sistema **y** en la intermedia oficial
  "Sectigo Public Server Authentication CA OV R36" (`res/raw/sectigo_ov_r36.pem`, vence 2036-03-21,
  descargada de `http://crt.sectigo.com/SectigoPublicServerAuthenticationCAOVR36.crt`), solo para ese dominio.
  Si el servidor vuelve a omitir la intermedia, la cadena sigue validando. Nada que renovar con cada certificado hoja.
- iOS: sin `handleChallenge`; el engine Darwin conecta sin cambios.

## 2026-11-16 · Fase 0

### D0. Estructura `androidApp` + `composeApp` + `iosApp`
AGP 9 no permite `com.android.application` y Kotlin Multiplatform en el mismo módulo: el punto de entrada
Android vive en `androidApp` y todo el código compartido en `composeApp` (biblioteca KMP). Se conserva
`applicationId = com.cazadores.cazadores_peru` para reemplazar a la app Flutter en Play; versión 2.0.0 (2).
