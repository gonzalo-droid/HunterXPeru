# Rediseño "caza recompensas" — diseño

Fecha: 2026-11-17 · Estado: borrador para aprobación

## Intención
Cazadores Perú muestra a los buscados del Programa de Recompensas del MININTER. El diseño actual
("Expediente cívico", `docs/kmp-migration/02-DISENO-UX.md`) es correcto pero se siente como un trámite.
El rediseño debe transmitir **caza recompensas**: buscar a los requisitoriados, ver cuánto vale la
información y **publicar** (difundir) los carteles. La difusión es el valor central: un cartel compartido
por WhatsApp llega a quien puede reconocer a la persona.

## Objetivos
- La ficha se ve como un cartel "SE BUSCA" con el delito como titular.
- Se ve de inmediato cuán alta es una recompensa (rango por clase con estrellas) y se puede leer su
  significado (leyenda).
- Se puede compartir el cartel como imagen.
- Se informa el régimen especial de recompensas por extorsión y sicariato.
- **Texto ordenado y contenido**: tamaños moderados, nada se desborda ni se corta mal en Android ni iOS,
  con letra normal, 1,3× y 2×.

## No objetivos (esta versión)
- Posición nacional "#N de 649 · Top X %": postergada, mapeada en `docs/DECISIONES.md` D16.
- Que la app lea `manifest.json` de Firebase.
- Filtro por Clase de recompensa: la API no filtra por monto. Llega con D16, cuando la app tenga el
  padrón completo y pueda filtrar en local con totales correctos.
- Ubicación por GPS ("Cerca de ti" usa un departamento elegido por el usuario).
- Integraciones directas con redes (se usa la hoja de compartir del sistema).
- Un segundo tema "clásico" opcional.

## Anti-objetivos (sería un fracaso aunque funcione)
- Que el tono empuje a hacer justicia por mano propia: nada de miras, "objetivo", "captura tú mismo".
  Reportar mantiene el tono serio y el "no te acerques".
- Que el cartel o la imagen compartida parezcan un documento oficial del MININTER.
- Prometer montos: el régimen especial dice "hasta"; el rango describe el monto ofrecido, no lo garantiza.
- Texto gigante, cortado o encimado; romper `ContrastTest` o la regla de letra 2×.

## Restricciones
- UI 100 % compartida en Compose Multiplatform; colores y tipos solo vía `Cz.colors` / `Cz.type`;
  textos solo en `composeResources/values/strings.xml`.
- Aviso "no oficial" en onboarding, Inicio, Ficha y Ajustes, y también en la imagen compartida.
- Contraste ≥ 4.5:1 en ambos temas (`ContrastTest` se amplía con los colores de papel).
- Filas "texto + acción" con `FlowRow`; etiquetas de pestaña escalan ≤ 1,3×.
- Sin fuentes nuevas: el estilo cartel usa **Archivo condensada 800** (ya incluida).

## Enfoques considerados
1. **Por etapas sobre lo existente (elegido).** Tokens de papel + componentes nuevos; una pantalla por PR.
   Revisable, cada etapa publicable, las pruebas protegen cada paso.
2. Todo de una vez: más rápido de ver terminado, pero un PR enorme y difícil de depurar en contraste y 2×.
3. Tema opcional "clásico/cartel": doble mantenimiento y doble prueba; solo sirve para experimentar.

## Decisiones tomadas
### Dirección visual: híbrido
Se conserva la base actual (fondo crema, encabezado oscuro, rojo solo para llamar). El **cartel en
papel** es la pieza protagonista: ficha, tablón de Inicio, Mis carteles e imagen compartida.
**El cartel es siempre papel**, también en modo oscuro (objeto físico; la imagen compartida sale igual).

### Rango de recompensa (dato oficial: solo `montoRecompensa`)
| Clase | Estrellas | Monto (provisional) |
|---|---|---|
| S | ★★★★ | ≥ S/ 300 000 |
| A | ★★★ | ≥ S/ 100 000 |
| B | ★★ | ≥ S/ 50 000 |
| C | ★ | menor |
Cálculo en `core` (`RewardClassCalculator`, junto a `DangerLevelCalculator`) con tests de bordes.
Antes de fijar cortes, revisar la distribución real (meta: Clase S ≈ 5 %).

### Nivel de peligro
Se mantiene el cálculo actual **solo por delito** (3 niveles). Como el rango ya expresa el monto, no se
mezcla. Se muestra como medidor de 3 barras con la línea "Estimado por: <delito que lo determinó>" y
la aclaración de no oficial.

### Delito
Titular del cartel: "SE BUSCA / POR <DELITO MÁS GRAVE>" en franja roja; si hay más, "+ N delito(s) más";
la lista completa sigue en la ficha. En listas, el delito va en rojo bajo el nombre.

*Decisión del diseñador (corregible):* "más grave" = el delito que coincide con el nivel más alto de
`DangerLevelCalculator` (EXTREMO > MUY ALTO); a igual nivel, el primero en el orden de la API. El cálculo
pasa a devolver nivel + delito determinante. Si ninguno coincide (nivel ALTO de respaldo, p. ej.
"LAVADO DE ACTIVOS"), el titular es el primer delito de la API y el medidor dice
"Nivel base: todos los buscados tienen delitos graves" en lugar de "Estimado por".

### Pantallas
1. **Onboarding:** recompensa, rangos y aviso no oficial. Saludo neutro (no "Bienvenido, cazador").
2. **Inicio · tablón:** encabezado "649 SE BUSCAN" + búsqueda; 0-800; banner régimen especial;
   carteles clavados de Clase S; "Cerca de ti" por departamento elegido; aviso no oficial.
3. **Buscar:** lista (no carteles) con nombre, delito, sello de rango y monto; chips de filtros activos.
4. **Filtros:** sin cambios funcionales (nueva piel). El filtro por Clase **se pospone**: la API no filtra
   por monto. Para ver primero la Clase S se usa el orden "Mayor recompensa", que ya existe.
5. **Ficha:** cartel papel; sello de rango con ⓘ → leyenda; medidor de peligro; aviso del régimen
   especial **solo** si algún delito es extorsión o sicariato; "Si lo reconoces" y botón Llamar se mantienen.
6. **Leyenda de rangos:** hoja inferior; también desde Ajustes.
7. **Régimen especial (nueva):** D. S. N.º 097-2025-PCM, "hasta S/ 500 000", los 5 puntos resumidos,
   Llamar 0-800, enlace al comunicado oficial, aviso de resumen no oficial. Contenido (texto dado por el
   usuario el 2026-11-17, a contrastar con el comunicado antes de publicar):
   - Intro: régimen especial aprobado por el D. S. N.º 097-2025-PCM; beneficios económicos a quien
     brinde información útil para capturar a implicados en extorsión y sicariato.
   - 1 Pago acelerado: en hasta 32 días hábiles.
   - 2 Protección del informante: anonimato y confidencialidad mediante una clave reservada.
   - 3 Procedimiento estructurado: recepción, verificación, formulación del expediente, evaluación y pago.
   - 4 Criterios claros: el monto depende del nivel de participación del colaborador en la captura.
   - 5 Coordinación institucional: DIRNIC de la PNP con la Comisión Evaluadora de Recompensas contra la
     Criminalidad.
   - Cierre: "Tu recompensa puede ser hasta S/ 500 000".
8. **Reportar:** estructura actual, nueva piel, suma "No te acerques a la persona".
9. **Publicar:** genera imagen del cartel (con 0-800 y "Imagen no oficial · verifica en recompensas.pe")
   y la abre en la hoja de compartir del sistema.
10. **Mis carteles** (antes Guardados): mini tablón de carteles.
11. **Ajustes:** suma "Cómo funcionan las recompensas" y "¿Qué significan los rangos?".
12. **Sin conexión / vacíos / error:** cartel "SIN SEÑAL", reintentar, "el 0-800 funciona sin internet".

### Reglas de texto (pedido explícito: ordenado, sin desbordes)
- **No crear tamaños nuevos mayores** que la escala actual: `display` 34 sp es el techo y solo para el
  "SE BUSCA" de la ficha; el resto de títulos de cartel usa `headline`/`title`; montos usan `amount`.
  En miniaturas (tablón, Mis carteles) usar `amountSmall`/`caption`, nunca `display`.
- Ningún contenedor de texto con **alto fijo**; las alturas fijas solo para fotos.
- Nombres: máximo 2 líneas con elipsis en listas y miniaturas; completos en la ficha.
- Franja de delito: máximo 2 líneas; "+ N más" en línea aparte.
- Montos nunca se parten: una línea, y si no cabe a 2× se reduce el estilo (de `amount` a `amountSmall`)
  en vez de cortar.
- Sellos (rango, peligro) con `FlowRow`, nunca en una `Row` rígida junto al texto.
- La imagen compartida se renderiza a tamaño fijo (1080×1350) con escala de fuente 1,0, independiente
  del ajuste del usuario.
- Verificación: capturas en Android e iOS a ancho pequeño (≈ 320 dp / iPhone SE) con fuente 1,0, 1,3 y
  2,0 en ficha, Inicio, Buscar, Régimen y Leyenda; ningún texto cortado ni encimado.

### Plan por etapas (un PR cada una)
1. Dominio: `RewardClassCalculator` + "estimado por" en peligro, con tests.
2. Tokens de papel en `Cz.colors` (+ `ContrastTest`) y componentes `WantedPoster`, `RankBadge`,
   `DangerMeter`.
3. Ficha + leyenda + pantalla Régimen especial (el aviso de la ficha depende de ella).
4. Publicar: imagen del cartel y `ExternalActions.shareImage(...)` (Android: FileProvider + ACTION_SEND;
   iOS: UIActivityViewController).
5. Inicio tablón (+ departamento "Cerca de ti" en DataStore).
6. Buscar, Filtros (solo nueva piel) y Mis carteles.
7. Onboarding, Ajustes, estados vacíos/sin conexión.
Cada etapa cierra con la auditoría rápida de `ux-audit` y las capturas de la regla de texto.

## Lo que queda al constructor
- Cómo renderizar el composable a imagen en ambas plataformas (p. ej. `GraphicsLayer.toImageBitmap()`).
- Nombres exactos de colores de papel y su valor final, siempre que pasen 4.5:1.
- Si existe auto-ajuste de tamaño de texto en la versión de Compose del proyecto, puede usarse para los
  montos; si no, la regla de bajar de estilo.
- Lista exacta de palabras clave de extorsión/sicariato para el aviso de la ficha (normalizada como en
  `DangerLevelCalculator`).

## Pendientes del usuario
- Enlace exacto al comunicado oficial del régimen especial (los recibidos apuntan a la portada).
