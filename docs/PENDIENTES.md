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
- **Respaldo de Android (PRIV-01):** `android:allowBackup="true"` sin `dataExtractionRules`, así que Android
  respalda la base de datos. Decidir si los guardados se respaldan (parece razonable: son carteles públicos) y
  declararlo con `dataExtractionRules` y `fullBackupContent`, o apagarlo. Anotar la decisión.
- **`CHANGELOG.md` (DOC-02):** no existe. Crearlo con la 2.0.3 (skill `changelog-update`); de ahí salen las notas
  de las tiendas.
- **Medidas (PERF-01):** anotar en `DECISIONES.md` el tamaño del AAB y del IPA, el arranque en frío y los datos
  de la primera apertura de la 2.0.3.
- **Migración de Room v2 → v3:** instalar la 2.0.3 encima de la 2.0.2 de la tienda con carteles guardados y
  comprobar que siguen ahí. Es la primera versión que cambia la base de los guardados.
- Ver en pantalla la marca "Ya no figura en el padrón" con un padrón que excluya a un guardado (hoy solo
  la cubren los tests).
- Correr `.maestro/smoke-guardar.yaml` en el simulador de iOS (solo se probó en Android).
- Build de tienda con `ADS_ENABLED=true` en `local.properties`.
- Al unir: merge normal (sin squash), tag `v2.0.3` y, si la 2.0.2 está publicada, tag `v2.0.2` en `5a9fbc7`.
- Checkout principal: borrar o descartar las copias viejas sin commit de `docs/BASE-CONOCIMIENTO-KMP.md` y
  `docs/PENDIENTES.md` antes de hacer pull de `main`, o el pull se detiene.

## Auditoría contra la base (2026-11-25)

- **Detectado:** primera auditoría con la Parte 7 de la base (`docs/auditorias/2026-11-25-base-kmp.md`). Lo
  urgente quedó en "Antes de publicar la 2.0.3"; esto es lo demás, por prioridad.
- **Media:**
  - **Tablets Android (TIE-04):** con `targetSdk 36`, Android 16 ignora `screenOrientation="portrait"` en
    pantallas de 600 dp o más. Probar en un emulador de tablet en horizontal; si se ve mal, retomar el diseño por
    ancho (TAB-01) de `feat/ipad-adaptativo` sin declarar iPad.
  - **Ajustes (AJU-01, AJU-02):** sumar "Contacto / reportar un problema" (correo con versión, modelo y sistema) y
    "Licencias de código abierto".
  - **Hook `pre-push` (CI-01):** `.githooks/pre-push` con `./gradlew check` y `git config core.hooksPath .githooks`,
    anotado en el README.
  - **Respaldo de llaves (SEC-04):** anotar en un lugar privado dónde están la keystore de subida y su
    contraseña, la `.p8` de App Store Connect, `google-services.json`, `GoogleService-Info.plist` y los IDs de
    AdMob; referenciarlo en `CLAUDE.md`.
  - **`CLAUDE.md` en git (DOC-01):** existe pero no está versionado. No tiene secretos.
  - **Confirmar Play App Signing (SEC-03)** en Play Console.
- **Baja:**
  - **Versión mínima (UPD-01, UPD-02, UPD-04):** un campo `minAppVersion` en el manifest del padrón y una pantalla
    que lleve a la tienda si la versión instalada es menor. Hoy el formato no cambia; hace falta antes del primer
    cambio de formato del padrón.
  - **Secciones de pantallas (COD-05):** pasar las secciones privadas de `DetailScreen.kt` (499 líneas) y
    `SearchScreen.kt` (442) a archivos propios del paquete.
  - **Accesibilidad (A11Y-02, A11Y-03):** revisar los 20 `contentDescription = null` (¿son decorativos?) y las zonas
    táctiles de los íconos sueltos.
- Ya estaban anotados: un solo enlace al compartir (AJU-04, NEK-02), navegador in-app (LNK-01, LNK-02), anuncios
  sin recompilar (ADS-06), tag `v2.0.2` (GIT-01).

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
  - **Sin CI de build ni tests.** El único workflow es `test-api.yml`, que es manual, y los PRs a la release
    no tienen checks. Ver base CI-01 y CI-02: empezar con un hook `pre-push` local y, si se suma CI, que
    corra solo en PRs a `release/**`.
  - Maestro en iOS y la marca "Ya no figura" en pantalla: ver "Antes de publicar la 2.0.3".

## Contraste con la base de conocimiento (2026-11-25)

- **Detectado:** al sumar a la base Ajustes estándar, enlaces y anuncios (AJU-*, LNK-*, ADS-06). Falta la
  auditoría completa con la Parte 7 de la base.
- **Apagar anuncios sin recompilar (ADS-06).** Hoy solo existe `ADS_ENABLED` (D35), que exige recompilar y volver a
  `true` antes de publicar. Sumar, solo en debug, un argumento de arranque `disableAds=true` (para Maestro y
  capturas) y un interruptor oculto en Ajustes. En release se ignoran. Con esto el flujo de Maestro podría
  correr sobre el build normal, sin tener que manejar el formulario de UMP.
- **Un solo enlace al compartir la app.** Hoy `about_share_text` y `share_text` llevan dos enlaces
  (`PLAY_STORE_URL` y `APP_STORE_URL`), y la landing dice que Android "llega pronto": el enlace de Play
  puede no llevar a nada. Crear en `neko-dojo` la ruta `/apps/[slug]/descargar` (redirige según el
  `User-Agent` usando `stores` de `src/content/apps.ts`, y a la landing si falta la tienda) y cambiar los
  dos textos a `https://nekodojo.app/apps/peruxhunter/descargar` (base AJU-04, NEK-02). Al 2026-11-25 la web
  publicada ya muestra el enlace del App Store, pero el `apps.ts` de la copia local tiene las dos tiendas en
  `null`: actualizar la copia (hay una rama `feat/peruxhunter-app-store`) antes de crear la ruta.
- **Navegador in-app para sitios ajenos.** Hoy las fuentes oficiales (recompensas.pe y gob.pe/mininter) se
  abren en el WebView interno **con JavaScript** (D34, D36), y los enlaces a otros dominios salen al
  navegador del sistema, fuera de la app. Según la base (LNK-01), el contenido ajeno va mejor en Custom Tabs
  (Android) y `SFSafariViewController` (iOS): no sale de la app, muestra la URL real y quita el WebView con
  JavaScript cargando páginas de terceros. Privacidad y términos siguen en el WebView (D28). Revisar que
  Play acepte igual la fuente "dentro de la app" (D36) con el navegador in-app.

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
