# Pendientes

Mejoras detectadas que quedaron para después. Las decisiones tomadas van en `DECISIONES.md`.

## Las fotos del padrón no se reintentan solas

- **Detectado:** 2026-11-17, probando en el emulador de Android recién encendido.
- **Síntoma:** en la primera apertura, las tarjetas de Inicio se quedaron con la silueta.
  Las fotos solo aparecieron al cambiar de pestaña y volver. Para entonces las 644 miniaturas
  ya estaban guardadas en `padron_image`.
- **Causa probable:** cada tarjeta pide su foto una sola vez, al dibujarse. Si en ese momento
  no hay red, o el zip de miniaturas sigue bajando, `PhotoRepository.image()` devuelve `null`
  y la tarjeta no vuelve a pedirla.
  - Esa única petición está en el `produceState` de `CriminalPhoto`
    (`composeApp/src/commonMain/kotlin/pe/cazadores/ui/components/CriminalPhoto.kt`).
  - Las miniaturas las baja y guarda `PadronStore.syncThumbs`
    (`composeApp/src/commonMain/kotlin/pe/cazadores/data/padron/PadronStore.kt`).
- **Quién lo vería:** un usuario que abre la app por primera vez con mala señal.
  Con la app instalada desde cero y la red lista, las fotos cargan bien.
- **Arreglo propuesto:**
  1. `PadronStore` expone un contador de imágenes (`StateFlow<Int>`) que sube al terminar
     `syncThumbs`, y también cuando vuelve la red.
  2. `CriminalPhoto` agrega ese contador a las claves de su `produceState`. Así, una tarjeta
     sin foto vuelve a intentar cuando cambia el contador. Las que ya tienen foto no cambian:
     la sacan de `PhotoCache`.
  3. Test en `PadronStoreTest`: el contador sube después de guardar las miniaturas.
- **Cómo reproducirlo:** el caso es que el manifest llega (300 KB) y el zip de miniaturas (3,8 MB)
  todavía no. Borrar los datos de la app (`adb shell pm clear com.nekodojo.peruxhunter`), bajar la
  velocidad de red del emulador (en la consola del emulador, `network speed gsm`), abrir la app y mirar
  Inicio cuando terminen de bajar las miniaturas.

## Compilar iOS con Xcode 27 antes de abril de 2027

- **Detectado:** 2026-11-18, al subir el primer build (2.0.0 (2)) a TestFlight.
- **Aviso de App Store Connect:** el build se hizo con el SDK de iOS 26.2. Desde abril de 2027, Apple solo
  acepta builds hechos con el SDK de iOS 27 o posterior (Xcode 27). Hoy no bloquea nada.
- **Qué hacer:** instalar Xcode 27 cuando salga, compilar y probar en el simulador, y subir los builds
  siguientes con esa versión. No requiere cambios de código previstos.
