# Pendientes

Mejoras detectadas que quedaron para después. Las decisiones tomadas van en `DECISIONES.md`.

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
- El interruptor oculto de D30 no es esto: solo sirve para pruebas y capturas en un equipo.

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
  1. **Se baja el zip entero aunque cambien pocas fotos.** Si en la semana cambian 5 miniaturas, se bajan
     igual los 3,8 MB. Arreglo: si faltan pocas (por ejemplo menos de 30), bajarlas una a una con
     `download(key)`; si faltan muchas, el zip.
  2. **Los guardados son una copia.** `FavoritesRepositoryImpl` guarda la persona como estaba (monto,
     foto en Base64). Por confirmar: si el padrón cambia el monto o la foto, o la persona sale del padrón,
     Guardados sigue mostrando lo viejo. Arreglo posible: al llegar una versión nueva, actualizar cada
     guardado por su `hash`. Si la persona ya no está, marcarla "ya no figura en el padrón" en vez de borrarla.
  3. **Sin aviso de datos nuevos.** Solo se revisa con la app abierta. Basta con eso (la fecha de los
     datos ya se ve en Inicio). Una tarea en segundo plano (WorkManager / BGTaskScheduler) solo hace falta
     si más adelante se quieren notificaciones.
