# Pendientes

Mejoras detectadas que quedaron para después. Las decisiones tomadas van en `DECISIONES.md`.

## Revisión de arquitectura (2026-11-25)

- **Detectado:** al armar `docs/BASE-CONOCIMIENTO-KMP.md`. Lo general pasó a ese documento; aquí quedan
  solo los puntos propios de esta app.
- **Resuelto el 2026-11-25** (rama `worktree-pendientes-arquitectura`):
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
  - **Sin CI de build ni tests.** El único workflow es `test-api.yml`, que es manual. Ver "CI con poco
    consumo" en la base de conocimiento: empezar con un hook `pre-push` local.
  - Correr el flujo de Maestro en el simulador de iOS.
  - Ver en pantalla la marca "Ya no figura en el padrón" con un padrón que excluya a un guardado (hoy solo
    la cubren los tests).
  - Que el flujo de Maestro maneje el formulario de UMP para poder correrlo con anuncios activos.

## Contraste con la base de conocimiento (2026-11-25)

- **Detectado:** al sumar a la base Ajustes estándar, enlaces y anuncios (§1.16, §1.17, §2.1). Contrastar el
  resto de la base con la app más adelante.
- **Apagar anuncios sin recompilar.** Hoy solo existe `ADS_ENABLED` (D35), que exige recompilar y volver a
  `true` antes de publicar. Sumar, solo en debug, un argumento de arranque `disableAds=true` (para Maestro y
  capturas) y un interruptor oculto en Ajustes. En release se ignoran. Con esto el flujo de Maestro podría
  correr sobre el build normal.
- **Un solo enlace al compartir la app.** Hoy `about_share_text` y `share_text` llevan dos enlaces
  (`PLAY_STORE_URL` y `APP_STORE_URL`), y la landing dice que Android "llega pronto": el enlace de Play
  puede no llevar a nada. Crear en `neko-dojo` la ruta `/apps/[slug]/descargar` (redirige según el
  `User-Agent` usando `stores` de `src/content/apps.ts`, y a la landing si falta la tienda) y cambiar los
  dos textos a `https://nekodojo.app/apps/peruxhunter/descargar` (base §1.16). Al 2026-11-25 la web
  publicada ya muestra el enlace del App Store, pero el `apps.ts` de la copia local tiene las dos tiendas en
  `null`: actualizar la copia (hay una rama `feat/peruxhunter-app-store`) antes de crear la ruta.
- **Navegador in-app para sitios ajenos.** Hoy las fuentes oficiales (recompensas.pe y gob.pe/mininter) se
  abren en el WebView interno **con JavaScript** (D34, D36), y los enlaces a otros dominios salen al
  navegador del sistema, fuera de la app. Según la base (§1.17), el contenido ajeno va mejor en Custom Tabs
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
