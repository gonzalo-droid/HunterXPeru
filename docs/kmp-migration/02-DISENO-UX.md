# Cazadores Perú — Propuesta UX/UI para la versión KMP

Mockups navegables: https://claude.ai/artifact/4K7mQQAxC2JonCtmFKDBKf (privado; compártelo desde su menú Share si otro lo necesita).

> Auditoría hecha leyendo código, sin correr la app en emulador/simulador. Lo que sigue es propuesta, no hallazgo verificado en pantalla.

## 1. Diagnóstico de la UI actual (Flutter)

| Sev. | Hallazgo | Arreglo en el rediseño |
|---|---|---|
| Alto | La app se presenta como "oficial" (texto, rojo institucional) sin serlo → riesgo de tienda y de confianza | Aviso "app no oficial" en onboarding, Inicio, Ficha y Ajustes; fuente + fecha de consulta |
| Alto | Teléfono distinto en onboarding (1818) y en el resto (0-800-40-007) | Un solo número, en una constante, validado |
| Alto | Peligrosidad calculada por la app se muestra como dato | Etiqueta "Estimado por la app, no oficial" |
| Medio | La acción "llamar" está en mitad de la ficha | CTA rojo fijo abajo en la Ficha + tarjeta en Inicio |
| Medio | Buscador del Home es falso (navega) y los filtros activos no se ven en la lista | Chips de filtros activos removibles + conteo de resultados |
| Medio | Switches de notificaciones sin función | Quitarlos hasta tener backend |
| Medio | Peligrosidad solo por color (rojo/naranja/amarillo) | Texto + nº de marcas ▲, contraste ≥ 4.5:1 |
| Bajo | Escala de texto limitada a 0.8–1.3× | Respetar el tamaño de letra del sistema (mín. hasta 2.0×) |
| Bajo | Sin estados vacíos diseñados en Guardados | Estado vacío con acción |

## 2. Dirección visual: "Expediente cívico"

Sobria y documental, como una ficha impresa. El rojo solo para la acción de llamar; la recompensa en amarillo con texto tinta.

| Token | Claro | Oscuro |
|---|---|---|
| `bg` (papel) | `#F3F1EC` | `#121315` |
| `surface` | `#FFFFFF` | `#1C1E21` |
| `ink` (texto) | `#16181B` | `#EDEBE6` |
| `inkMuted` | `#4A4F57` | `#B9B5AD` |
| `line` | `#DAD6CE` | `#2E3136` |
| `action` (llamar) | `#B4121B` | `#E5484D` |
| `reward` (fondo) | `#F2B93B` + texto `ink` | igual |
| `levelExtremo` | `#8E1B14` / texto blanco | igual |
| `levelMuyAlto` | `#B0480C` / texto blanco | igual |
| `levelAlto` | `#EADFB8` / texto `#3D3200` | igual |

**Tipografía:** Archivo (variable, eje `wdth`): condensada 70 % / 800 para nombres, montos y títulos en mayúsculas; normal 400/600 para cuerpo (mín. 15 sp). IBM Plex Mono para IDs y fechas. Empaquetar las fuentes en `composeResources/font`.

**Forma:** radios 12–16 dp en tarjetas, 14 dp en botones; botones de 52–56 dp de alto; áreas táctiles ≥ 44/48 dp.

## 3. Navegación

Bottom bar de 4 pestañas: **Inicio · Buscar · Guardados · Ajustes**. Ficha a pantalla completa fuera de la barra. Filtros y Reportar como bottom sheets. Deep link `cazadores://criminal/{hash}` → Ficha.

## 4. Fichas de vista

**Onboarding (3 pasos)**
- Objetivo: entender qué es, que no es oficial y que nunca hay que enfrentarse.
- Acción principal: "Entendido, empezar". Secundaria: Omitir.
- Estados: único. Accesibilidad: indicador de paso anunciado ("Paso 3 de 3").

**Inicio**
- Objetivo: ver los más buscados y tener la línea a un toque.
- Jerarquía: header oscuro con buscador → tarjeta "Llamar 0-800" → stats (total, recompensa máxima real) → carrusel "Mayores recompensas" (top5) → banner → aviso de fuente.
- Estados: cargando (skeleton), error (reintentar), offline (datos guardados + fecha).

**Buscar**
- Objetivo: encontrar a una persona por nombre/alias y filtrar.
- Jerarquía: campo de búsqueda (debounce 400 ms) + botón filtros con contador → chips activos → "N resultados" + orden → lista con scroll infinito.
- Tarjeta: foto 72×92, nombre condensado, delito, recompensa, nivel.
- Estados: cargando (skeleton), vacío ("Sin resultados para…" + limpiar filtros), error, offline (caché), cargando más (pie).
- Anuncio: banner cada 8 ítems, marcado "Anuncio".

**Filtros (sheet)**
- Departamento → Provincia (dependiente), Delito (chips), Sexo (segmentado). CTA "Ver N resultados".

**Ficha**
- Objetivo: identificar y actuar seguro.
- Jerarquía: foto + ID + recompensa → nombre → peligrosidad (con nota) → delitos → datos (departamento, provincia, sexo) → "Si lo reconoces" → fuente/fecha.
- Barra superior: volver, compartir, guardar. CTA fijo: **Llamar 0-800-40-007** + botón "Reportar por escrito".
- Estados: cargando, error, no encontrado (hash inválido / deep link viejo).
- Sin anuncios.

**Reportar (sheet)**
- Llamar (primario) / por escrito: dónde, cuándo, detalles → abre el correo con el texto. Aviso de que la app no guarda ni envía nada. Sin anuncios.

**Guardados**: lista igual a Buscar, deslizar para quitar con "Deshacer". Estado vacío con "Ir a Buscar".

**Ajustes**: Apariencia (Sistema/Claro/Oscuro), Preferencias de anuncios (formulario UMP), Privacidad, Términos, Fuente de datos, Enviar comentarios, "Sobre esta app" (no afiliada), versión.

## 5. Anuncios (AdMob)

- Banner adaptativo anclado en Inicio y nativo/banner inline cada 8 resultados en Buscar y Guardados.
- Ficha: un banner al final, después de la fuente, lejos de "Llamar" y de "Reportar" (cambio del 2026-11-19).
- Compartir el cartel: anuncio bonificado antes de compartir, con aviso previo; si no carga, se comparte igual.
- **Nunca** en Reportar ni Onboarding (contexto sensible y de seguridad). Sin interstitials ni app-open.
- Flujo de consentimiento: UMP al iniciar (EEA/UK) → en iOS, ATT después de UMP → recién entonces `MobileAds.initialize`. Entrada en Ajustes para reabrir el formulario de privacidad.
