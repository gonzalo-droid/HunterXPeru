# Pendientes

Mejoras detectadas que quedaron para después. Las decisiones tomadas van en `DECISIONES.md`.

## Antes de publicar la 2.0.3 (PR 12, `release/2.0.3` → `main`)

- **Detectado:** 2026-11-25, al abrir el PR de la release y en la auditoría contra la base
  (`docs/auditorias/2026-11-25-base-kmp.md`). Los IDs son los de la base.
- Probar a mano el release de Android (R8) y de iOS: claro, oscuro, letra grande y modo avión, más una pasada
  con TalkBack y VoiceOver por el flujo principal (PUB-02, A11Y-04). Probar también negar el permiso de fotos
  y ATT (PERM-02).
- **Política de privacidad y Crashlytics (PRIV-03, CRA-06):** D38 dejó como tarea quitar "No usamos
  herramientas de analítica ni de reportes de errores" y explicar el informe de crash. Confirmar que la página
  publicada ya lo dice, y que Data safety y la privacidad de App Store declaran Crashlytics. Si no, actualizarlas
  antes de subir.
  La política también debe decir que, en Android, los guardados entran en la copia de Google si el usuario la tiene
  activa (D41).
- **`CHANGELOG.md`:** poner la fecha de la 2.0.3 (y de la 2.0.2) al publicar.
- **Medidas (PERF-01):** anotar en `DECISIONES.md` el tamaño del AAB y del IPA, el arranque en frío y los datos
  de la primera apertura de la 2.0.3.
- **Migración de Room v2 → v3:** instalar la 2.0.3 encima de la 2.0.2 de la tienda con carteles guardados y
  comprobar que siguen ahí. Es la primera versión que cambia la base de los guardados.
- Ver en pantalla la marca "Ya no figura en el padrón" con un padrón que excluya a un guardado (hoy solo
  la cubren los tests).
- Correr `.maestro/smoke-guardar.yaml` en el simulador de iOS (solo se probó en Android) y otra vez en Android con
  el argumento `disableAds` (ya no hace falta `ADS_ENABLED=false`).
- **Probar en Android lo del 2026-11-26** (el emulador se cayó y solo se vio en iOS): filas Contacto y Licencias, el
  correo de contacto con sus datos, Custom Tabs para las fuentes oficiales, `adb shell am start --ez disableAds true`
  y 7 toques en la versión, y el respaldo con `adb shell bmgr backupnow com.nekodojo.peruxhunter`.
- Build de tienda con `ADS_ENABLED=true` en `local.properties`.
- Al unir: merge normal (sin squash), tag `v2.0.3` y, si la 2.0.2 está publicada, tag `v2.0.2` en `5a9fbc7`.
- Checkout principal: borrar o descartar las copias viejas sin commit de `docs/BASE-CONOCIMIENTO-KMP.md` y
  `docs/PENDIENTES.md` antes de hacer pull de `main`, o el pull se detiene.

## Auditoría contra la base (2026-11-25)

- **Detectado:** primera auditoría con la Parte 7 de la base (`docs/auditorias/2026-11-25-base-kmp.md`). Lo
  urgente quedó en "Antes de publicar la 2.0.3".
- **Resuelto el 2026-11-26** (PRs 17 a 22, en `release/2.0.3`):
  - CI-01 hook `pre-push`; DOC-01 `CLAUDE.md` en git; DOC-02 `CHANGELOG.md`; PRIV-01 reglas de respaldo (D41).
  - COD-05 secciones de `DetailScreen` y `SearchScreen` en archivos propios; A11Y-02 revisado (los 20 `null` son
    decorativos o su contenedor ya describe la acción).
  - AJU-01/AJU-02 filas "Contacto y reportar un problema" y "Licencias de código abierto".
  - ADS-06 anuncios apagables en debug sin recompilar; Maestro usa `disableAds`.
  - LNK-01/LNK-02 fuentes oficiales en el navegador in-app; el WebView ya no usa JavaScript (D42).
  - UPD-01/02/04 `minAppVersionCode` en el manifest y pantalla "Actualiza la app" (D43).
- **Sigue pendiente:**
  - **Tablets Android (TIE-04):** con `targetSdk 36`, Android 16 ignora `screenOrientation="portrait"` en
    pantallas de 600 dp o más. Probar en un emulador de tablet en horizontal; si se ve mal, retomar el diseño por
    ancho (TAB-01) de `feat/ipad-adaptativo` sin declarar iPad.
  - **Respaldo de llaves (SEC-04):** anotar en un lugar privado dónde están la keystore de subida y su
    contraseña, la `.p8` de App Store Connect, `google-services.json`, `GoogleService-Info.plist` y los IDs de
    AdMob; referenciarlo en `CLAUDE.md`.
  - **Confirmar Play App Signing (SEC-03)** en Play Console.
  - **Zonas táctiles (A11Y-03):** revisar los íconos sueltos con Accessibility Scanner / Accessibility Inspector.
  - **Un solo enlace al compartir (AJU-04, NEK-02):** ver "Contraste con la base".
  - **Tag `v2.0.2` (GIT-01):** ver "Antes de publicar la 2.0.3".

## Revisión de arquitectura (2026-11-25)

- **Detectado:** al armar `docs/BASE-CONOCIMIENTO-KMP.md`. Lo general pasó a ese documento; aquí quedan
  solo los puntos propios de esta app.
- **Resuelto el 2026-11-25** (ya en `release/2.0.3`):
  - Guardados desactualizados: `FavoritesRepositoryImpl.reconcile` los actualiza con cada padrón nuevo y
    marca "Ya no figura en el padrón" a quien salió (Room v3, columna `delistedAt`).
  - Fotos: si faltan ≤ 30 miniaturas se bajan sueltas; si no, el zip (`PadronStore.syncThumbs`).
  - Lógica en Composables: compartir el cartel pasó a `PosterShare.kt`, con tests. `DetailScreen` y
    `SearchScreen` ya estaban en secciones privadas; no se dividieron más.
  - Versión de iOS: `./gradlew check` falla si `Config.xcconfig` no coincide con el catálogo
    (`checkIosVersion`). El desfase anotado antes era falsa alarma: ambos estaban en 2.0.2 (8).
  - Tests de UI: `.maestro/smoke-guardar.yaml` (Inicio → Buscar → Ficha → Guardar → Carteles) pasa en el
    emulador Android con `ADS_ENABLED=false`.
- **Sigue pendiente:**
  - **Sin CI en la nube.** Hay hook `pre-push` local (CI-01). Si se suma CI, que corra solo en PRs a
    `release/**` (CI-02).
  - Maestro en iOS y la marca "Ya no figura" en pantalla: ver "Antes de publicar la 2.0.3".

## Contraste con la base de conocimiento (2026-11-25)

- **Detectado:** al sumar a la base Ajustes estándar, enlaces y anuncios (AJU-*, LNK-*, ADS-06). La auditoría
  completa se hizo el 2026-11-25 (ver "Auditoría contra la base").
- **Un solo enlace al compartir la app.** Hoy `about_share_text` y `share_text` llevan dos enlaces
  (`PLAY_STORE_URL` y `APP_STORE_URL`), y la landing dice que Android "llega pronto": el enlace de Play
  puede no llevar a nada. Crear en `neko-dojo` la ruta `/apps/[slug]/descargar` (redirige según el
  `User-Agent` usando `stores` de `src/content/apps.ts`, y a la landing si falta la tienda) y cambiar los
  dos textos a `https://nekodojo.app/apps/peruxhunter/descargar` (base AJU-04, NEK-02). Al 2026-11-25 la web
  publicada ya muestra el enlace del App Store, pero el `apps.ts` de la copia local tiene las dos tiendas en
  `null`: actualizar la copia (hay una rama `feat/peruxhunter-app-store`) antes de crear la ruta.
- Resueltos el 2026-11-26: anuncios sin recompilar (ADS-06) y navegador in-app (LNK-01, D42).

## iPad (retirado de la 2.0.3)

- **Detectado:** 2026-11-25. El PR 9 (`feat/ipad-adaptativo`: contenido centrado a 720 dp, 3 columnas de
  carteles, hoja de compartir anclada, `TARGETED_DEVICE_FAMILY = "1,2"`) se unió a `release/2.0.3` y se
  revirtió el mismo día: la 2.0.3 sigue solo para iPhone.
- **Por qué:** publicar con iPad no tiene vuelta atrás (App Store no deja retirarlo) y faltaba la prueba
  manual en iPad (letra 2×, oscuro, Stage Manager) y las capturas de iPad 13".
- **Para retomarlo en una release propia (p. ej. `release/2.1.0`):** como el merge se revirtió, volver a unir
  la rama no trae nada. Hay que revertir el commit de reversión ("Revert \"Merge pull request #9…\"")
  en una rama nueva desde la release, o hacer cherry-pick de los commits del PR. La decisión iba como D40 y
  el plan en `docs/superpowers/plans/2026-11-19-ipad-adaptativo.md` (ambos vuelven con la reversión).

## Compilar iOS con Xcode 27 antes de abril de 2027

- **Detectado:** 2026-11-18, al subir el primer build (2.0.0 (2)) a TestFlight.
- **Aviso de App Store Connect:** el build se hizo con el SDK de iOS 26.2. Desde abril de 2027, Apple solo
  acepta builds hechos con el SDK de iOS 27 o posterior (Xcode 27). Hoy no bloquea nada.
- **Qué hacer:** instalar Xcode 27 cuando salga, compilar y probar en el simulador, y subir los builds
  siguientes con esa versión. No requiere cambios de código previstos.

## Versión premium con comunidad (idea)

- **Detectado:** 2026-11-18, al hablar de ocultar anuncios.
- **Idea:** una opción de pago que quite los anuncios y abra una sección de comunidad, donde los usuarios
  agreguen información sobre cada persona del padrón.
- **Antes de diseñarla, decidir:**
  - Moderación y riesgo legal: es información sobre personas reales buscadas. Hace falta revisar qué se
    publica, cómo se reporta y quién responde por ello.
  - Backend para los aportes (cuentas, almacenamiento y moderación); hoy la app solo lee.
  - Cobro: compras dentro de la app en Google Play y App Store, y cómo se valida la compra.
- El flag `ADS_ENABLED` de D35 no es esto: solo sirve para pruebas y capturas en un equipo.

## Sincronización semanal: huecos al cambiar datos o fotos

- **Detectado:** 2026-11-18, revisando cómo llega una versión nueva del padrón.
- **Lo que ya funciona:**
  - `sync/sync_requisitoriados.py` sube la versión solo si cambió algo (`dataHash`) y publica
    `data/thumbs-v<N>.zip`.
  - Las fotos se nombran por contenido (`thumbs/<hash>-<sha>.jpg`). Si cambia la foto, cambia el nombre:
    nunca se muestra una foto vieja con un nombre que ya existía.
  - La app revisa el manifest cada 6 h al usarse (`PadronStore.refresh`), reemplaza todos los datos si la
    versión es mayor, borra las miniaturas que ya no están y baja las que faltan.
  - Si el zip falla, se reintenta al volver la red (D31).
- **Huecos:**
  1. **(Resuelto 2026-11-25)** **Se baja el zip entero aunque cambien pocas fotos.** Si en la semana cambian 5 miniaturas, se bajan
     igual los 3,8 MB. Arreglo: si faltan pocas (por ejemplo menos de 30), bajarlas una a una con
     `download(key)`; si faltan muchas, el zip.
  2. **(Resuelto 2026-11-25)** **Los guardados son una copia.** `FavoritesRepositoryImpl` guarda la persona como estaba (monto,
     foto en Base64). Por confirmar: si el padrón cambia el monto o la foto, o la persona sale del padrón,
     Guardados sigue mostrando lo viejo. Arreglo posible: al llegar una versión nueva, actualizar cada
     guardado por su `hash`. Si la persona ya no está, marcarla "ya no figura en el padrón" en vez de borrarla.
  3. **Sin aviso de datos nuevos.** Solo se revisa con la app abierta. Basta con eso (la fecha de los
     datos ya se ve en Inicio). Una tarea en segundo plano (WorkManager / BGTaskScheduler) solo hace falta
     si más adelante se quieren notificaciones.
