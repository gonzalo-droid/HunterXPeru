# Sincronización de datos (Mac → Firebase Storage)

La API del Programa de Recompensas no responde a servidores fuera del Perú (probado desde GitHub
Actions, ver `docs/DECISIONES.md` D15). Por eso la sincronización corre en una Mac en Perú y publica
en Firebase Storage, desde donde la app descarga los datos.

```
launchd (10:00 y 20:00) → sync_requisitoriados.py → API MININTER
                                   │  publica como máximo 1 vez por semana
                                   ▼
          Firebase Storage: data/manifest.json · data/thumbs-vN.zip · thumbs/ · photos/
```

## Qué hace
- Baja la lista completa (4 páginas de 200) y la ficha solo de los perfiles nuevos o cambiados.
- Genera miniaturas (240 px) y fotos de ficha (720 px) en JPEG.
- Si nada cambió, no sube nada. Si la API falla, entrega 0 perfiles o desaparece más del 30 %,
  **no publica** y la versión anterior sigue vigente. Avisa con una notificación de macOS.
- Sube primero las imágenes y al final `manifest.json`; borra las bajas después.

## Puesta en marcha (una vez)
1. Firebase: crear el proyecto, activar **Blaze** y crear el bucket de Storage en **US-CENTRAL1**
   (fuera de EE. UU. no aplica el tramo gratis). Poner una alerta de presupuesto en Google Cloud.
2. Reglas: pegar `sync/storage.rules` en Storage → Reglas.
3. Cuenta de servicio: Google Cloud → IAM → Cuentas de servicio → crear una con el rol
   **Administrador de objetos de Storage** → Claves → JSON. Guardarla en
   `~/.config/cazadores-sync/service-account.json` (nunca en el repo) con `chmod 600`.
4. Primera publicación a mano:
   ```bash
   uv run sync/sync_requisitoriados.py --bucket <proyecto>.firebasestorage.app --force
   ```
5. Tarea automática: `sync/install-launchd.sh <proyecto>.firebasestorage.app`

## Operación
- Log: `~/Library/Logs/CazadoresSync/sync.log`
- Estado local (versión, fichas ya descargadas, imágenes): `~/Library/Application Support/CazadoresSync/`
- Forzar ahora: `launchctl kickstart -k gui/$(id -u)/pe.cazadores.sync`
- Probar sin subir: `uv run sync/sync_requisitoriados.py --dry-run --out /tmp/cz --state-dir /tmp/cz-state`
- Desinstalar: `sync/install-launchd.sh --uninstall`

Tras editar el script, volver a correr `install-launchd.sh` (launchd usa una copia).

## Formato de `manifest.json` (schema 1)
`version`, `dataHash` (cambia solo si cambian los datos), `generatedAt`, `count`, `thumbsZip` y
`criminals[]` con `id`, `hash`, `nombreCompleto`, apellidos, `nombres`, `sexo`, `montoRecompensa`,
`delito`, `delitos[]`, `departamento`, `provincia`, `thumb` y `photo` (rutas dentro del bucket).

URL pública de cualquier archivo:
`https://firebasestorage.googleapis.com/v0/b/<bucket>/o/<ruta con / como %2F>?alt=media`
