# Changelog

Todos los cambios relevantes de Cazadores Perú (Android + iOS). La versión es la misma en ambas plataformas
(`gradle/libs.versions.toml`).

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/) y el proyecto usa
[Semantic Versioning](https://semver.org/lang/es/). Fechas en ISO (`AAAA-MM-DD`). Las dependencias se nombran con
su alias del version catalog (`room-version de 2.8.4 a 2.8.5`).

## [Unreleased]

## [2.0.3] - <!-- TODO: fecha de publicación -->

### Added
- Los carteles guardados se actualizan con cada padrón nuevo (monto, datos y foto) y muestran "Ya no figura en el padrón" cuando la persona sale de la lista, sin borrarlos
- Flujo de UI con Maestro: Inicio → Buscar → Ficha → Guardar → Carteles (`.maestro/smoke-guardar.yaml`)
- Ajustes: "Contacto y reportar un problema" abre un correo con la versión, el dispositivo y el sistema ya escritos
- Ajustes: pantalla "Licencias de código abierto" con las bibliotecas, fuentes y SDK que usa la app

- Si el padrón publicado necesita una versión más nueva de la app, se muestra "Actualiza la app" con un botón a la tienda

### Changed
- Si faltan 30 miniaturas o menos, se descargan sueltas en vez del zip completo del padrón
- Base de datos Room v3 (columna `delistedAt` en guardados, migración automática desde la v2)
- `./gradlew check` falla si la versión de `Config.xcconfig` no coincide con el catálogo (`checkIosVersion`)
- Android respalda solo los guardados y los ajustes en la copia de Google y al pasar a un teléfono nuevo (D41)
- La codificación del cartel para compartir sale de la pantalla a una función con tests
- Las fuentes oficiales (recompensas.pe y gob.pe/mininter) se abren en el navegador dentro de la app en vez de la vista web con JavaScript (D42)

## [2.0.2] - <!-- TODO: fecha de publicación de la 2.0.2 -->

- Primera versión registrada en este archivo; los cambios anteriores están en `docs/DECISIONES.md` (D1–D38).
