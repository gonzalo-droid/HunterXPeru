# Decisiones y verificaciones

Registro de lo que se verificó contra la API real y de las decisiones que cambian o precisan
`docs/kmp-migration/`. La más reciente va arriba.

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
