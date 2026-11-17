# Decisiones y verificaciones

Registro de lo que se verificó contra la API real y de las decisiones que cambian o precisan
`docs/kmp-migration/`. La más reciente va arriba.

## 2026-11-17 · Padrón desde Firebase

### D21. La app lee el padrón semanal de Firebase; la API queda de respaldo
- Fuente principal: `data/manifest.json` del bucket `peruxhunter.firebasestorage.app` (URL pública
  `…/o/<ruta>?alt=media`, sin SDK de Firebase). Lo publica `sync/sync_requisitoriados.py` (D15).
- `PadronStore` lo guarda en Room (`padron_meta`, esquema **v2**, AutoMigration 1→2) y lo tiene en memoria.
  Primera vez: espera el manifest hasta 15 s; si falla, sigue con la API y no reintenta antes de 60 s.
  Después responde al instante con lo guardado y revisa si hay versión nueva cada 6 h, en segundo plano.
  Un manifest roto, vacío o de esquema mayor se ignora y se queda el anterior.
- Búsqueda, top 5, totales y ficha salen del padrón (`PadronQuery`): el texto ignora tildes y busca cada
  palabra; los filtros traducen ids del catálogo a nombres. Sin padrón (o sin catálogos) se usa la API como antes.
- Fotos: el zip de miniaturas (sin compresión) se baja una vez por versión y se guarda en `padron_image`;
  la foto grande se baja al abrir la ficha y se guardan las últimas 60. Tarjetas y mini carteles usan solo
  la miniatura; ficha y cartel a compartir muestran la miniatura al instante y la cambian por la grande.
  Un favorito guarda la miniatura en Base64, como antes.
- Fecha de los datos: la de publicación del padrón (`dataAtMillis`), en Inicio y en la fuente de la Ficha.
- Ahora sí (pendientes de D16/D18): **posición nacional** en la Ficha ("Puesto N de T · top X % del país",
  con empates) y **filtro por clase S/A/B/C** en Filtros. Con la API de respaldo, el filtro por clase solo
  filtra la página recibida y la posición no aparece. Los cortes de clase siguen provisionales.
- Corrige D19: la app ya no consulta la API en cada pantalla; los datos siguen viniendo de la web oficial.

## 2026-11-17 · Identidad V3 (logo e íconos)

### D20. Logo e íconos de la identidad V3
- Fuente: SVG de diseño en `docs/brand/` (lupa crema con silueta y marco de enfoque vino sobre tinta #16181B).
- Android: primer plano y capa monocroma como **VectorDrawable** (no los PNG de 1024 del paquete). El
  primer plano del paquete (símbolo a 0,82) **se salía de la zona segura**: con la máscara circular de Pixel
  se cortaban el aro y el mango. Se reescala a 0,676 centrado en el círculo que envuelve el símbolo, que
  cabe entero en los 66 dp. El fondo sigue siendo `ic_launcher_background` (#16181B).
- iOS: el ícono claro es el PNG de diseño (opaco). El oscuro y el tintado de iOS 18 no venían en el paquete:
  se derivan del mismo SVG sin fondo (colores originales; todo blanco para el tintado) con
  `iosApp/scripts/render-app-icon.sh`, que reemplaza al generador Swift del ícono anterior.
- En la app, el símbolo (`BrandMark`) va en el onboarding y en "Sobre este proyecto", siempre sobre la
  cabecera oscura. Se usa la variante **crema** (papel + `onPaper` + `posterRed`): el vino sobre tinta
  da ~1,7:1 y casi no se ve, y el papel es igual en ambos temas, como el cartel (D18). `ContrastTest`
  exige papel/cabecera ≥ 3:1. El logotipo con texto no se usa en la app: depende de fuentes no incluidas.

## 2026-11-17 · Rediseño "caza recompensas"

### D19. Pantalla "Sobre este proyecto"
- Desde Ajustes → Información. Explica por qué existe la app, la idea de comunidad atenta (reconocer y
  avisar; **la captura la hace la Policía**, nunca te acerques), hacia dónde va y cómo apoyar.
- Financiamiento: solo se mencionan los anuncios (no hay suscripción en la app). Datos: "vienen
  directamente de la web oficial" (vía el padrón semanal en Firebase, D21; la API es el respaldo).
- Comentarios vía "Calificar y comentar" (reseña en tienda); "Compartir la app" comparte el enlace de Play.
- La tarjeta del aviso en Ajustes pasa a titularse "App no oficial" para no confundirse con esta pantalla.
- Verificado en iPhone 16 (oscuro, normal y XL) y Android (claro, oscuro y letra 2×), incluida la hoja de compartir.

### D18. Rediseño "caza recompensas" aplicado
- Todas las pantallas usan el estilo cartel (Inicio tablón, Buscar, Filtros, Mis carteles, Ficha, Régimen
  especial, leyenda, onboarding, Ajustes, estados vacíos). El cartel es **siempre papel**, igual en claro y
  oscuro; solo el resto de la pantalla cambia de tema (`ContrastTest` cubre ambos).
- Rango por monto de recompensa: clases S/A/B/C con estrellas; cortes **provisionales** (S ≥ 300 000,
  A ≥ 100 000, B ≥ 50 000, C > 0). Sin monto no hay sello ni "S/ 0".
- Peligrosidad estimada **solo por delito** (palabras clave; sin coincidencia: "Nivel base"), nunca por monto.
- Postergados (ver D16): filtro por clase y posición nacional.
- "Cerca de ti" usa el departamento elegido por la persona (DataStore); sin elegir muestra la invitación.
- Verificación final: Android a 360 dp (release, R8) con letra 1,0/1,3/2,0 y una pasada en oscuro.
  Defecto hallado y corregido: botón "Cerrar" de la leyenda ilegible en oscuro.
- iOS (iPhone 16, release): Inicio, Ficha, Leyenda, Régimen, Buscar y Mis carteles con letra normal y
  accesibilidad XL, más Inicio/Ficha en oscuro con letra ≈ 1,3× y la hoja de compartir (`ios-06`…`ios-15`).
  Sin textos cortados ni encimados; a XL el tablón pasa a una columna y el sello baja bajo el monto.
- **No probado:** Android a ≈ 320 dp (solo se midió 360 dp) ni iPhone SE para todas las pantallas (solo
  Inicio; el resto se probó en iPhone 16, 393 pt de ancho). Se borraron `ios-01-att.jpg` e
  `ios-03-ficha-deep-link.jpg` (mostraban el diseño anterior).

### D17. Compartir el cartel como imagen
- El botón Compartir de la ficha dibuja `SharePosterContent` fuera de pantalla (1080 × 1350 px, densidad
  fija 2 y `fontScale = 1`, tema claro forzado: sale igual con cualquier tema o tamaño de letra) en un
  `GraphicsLayer` que nunca se pinta, y comparte el PNG con el texto de siempre. Si el dibujo o la
  codificación fallan, cae al texto solo. Si el cartel no cabe en alto, se reduce (nunca se corta).
- La imagen lleva "No te acerques · Llama gratis 0-800-40-007" y "Imagen no oficial · verifica en recompensas.pe".
  El Compartir espera a que la foto esté decodificada (`ensurePhotoDecoded`) antes de componer el cartel.
- Android: `cache/share/cartel.png` (se sobrescribe) vía `FileProvider` `${applicationId}.share`.
- iOS: `UIImage` en `UIActivityViewController`. Se agregó `NSPhotoLibraryAddUsageDescription`: sin esa
  clave, "Guardar imagen" en la hoja cierra la app.

### D16. Posición nacional de la recompensa: postergada a una versión siguiente
El rediseño muestra el **rango de recompensa** (clase S/A/B/C con estrellas, calculado solo con
`montoRecompensa`) y su leyenda. La línea de posición nacional ("#2 de 649 · Top 1 % del país")
**no entra en esta versión**. Queda mapeada así para retomarla:
- **Origen del dato:** `sync/sync_requisitoriados.py` ya baja los ~649 montos. Al armar el manifest,
  ordenar por monto y agregar por perfil `rewardRank`, `rewardPercentile` y `rewardClass`; a nivel
  raíz, `rewardClasses` (cortes y cantidad por clase) para que la leyenda muestre conteos reales.
  Costo: milisegundos, sin llamadas extra a la API.
- **Empates:** varios perfiles comparten S/ 500 000. "#N" es engañoso; usar "Top X %" o
  "#1 · empatado con N más".
- **Requisito:** la app aún consulta la API directamente y no lee `manifest.json`. La posición llega
  cuando la app consuma los datos de Firebase; hasta entonces solo se muestra la clase.
- **Filtro por clase:** también postergado. La API no filtra por monto; con el padrón local se filtra
  al instante y con totales correctos.
- **Cortes de clase:** provisionales (≥ S/ 300 mil, 100 mil, 50 mil). Calibrarlos con los montos
  reales para que la clase S quede en torno al 5 % de los perfiles.

## 2026-11-16 · Ajustes tras la revisión

### D15. La sincronización corre en una Mac en Perú
La API no respondió en 60 s desde GitHub Actions (Azure, EE. UU.) y responde en ~2 s desde Perú: no se
puede usar una nube extranjera para consultarla. `sync/` corre con launchd en una Mac, publica en
Firebase Storage como máximo una vez por semana y solo pide las fichas nuevas o cambiadas
(2 en paralelo con pausa: con 3 el servidor dejó de aceptar conexiones unos minutos).
Firebase solo sirve archivos; la app nunca depende de que la Mac esté encendida para funcionar.

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
