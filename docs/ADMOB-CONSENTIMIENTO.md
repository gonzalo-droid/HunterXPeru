# Mensajes de consentimiento en AdMob (español)

Los mensajes de UMP se configuran en la consola de AdMob, no en el código. La app ya los pide en el orden
correcto (UMP → ATT → anuncios). Hay que crearlos **una vez por app** (Android e iOS).

Requisito previo: la **URL de la política de privacidad** publicada. AdMob no deja publicar el mensaje GDPR
sin ella, y es la misma que va en `AppConfig.PRIVACY_POLICY_URL`.

## 1. Mensaje GDPR (EEE, Reino Unido y Suiza)

AdMob → Privacidad y mensajería → GDPR → Crear mensaje.

| Campo | Valor |
|---|---|
| Apps | Cazadores Perú (Android) y Cazadores Perú (iOS) |
| Idioma predeterminado | Español |
| Idiomas | Español (agregar Inglés es opcional) |
| Nombre del sitio / app | Cazadores Perú |
| URL de la política de privacidad | la URL publicada |
| Logo | ícono de la app (1024 px) |
| Opciones del usuario | Consentir · Administrar opciones · **No consentir** (activar el botón "No consentir": lo exige la normativa) |
| Socios publicitarios | Lista "Socios de publicidad comúnmente usados" |

El texto del formulario GDPR lo genera Google y no se puede editar; al elegir Español sale traducido.

## 2. Mensaje explicativo de IDFA (solo iOS)

Se muestra justo antes del aviso de rastreo de Apple. AdMob → Privacidad y mensajería → Mensaje
explicativo de IDFA → Crear mensaje → Idioma: Español.

- **Título:** Anuncios que mantienen la app gratis
- **Mensaje:** Cazadores Perú es gratis gracias a los anuncios. En el siguiente aviso puedes permitir que
  usemos un identificador de tu dispositivo para mostrarte anuncios más relevantes. Si no lo permites,
  igual verás anuncios, pero menos relacionados con tus intereses. Puedes cambiarlo cuando quieras en
  Ajustes del iPhone.
- **Botón:** Continuar

Debe ser coherente con `NSUserTrackingUsageDescription` del `Info.plist`.

## 3. Regulaciones estatales de EE. UU. (opcional)

AdMob → Privacidad y mensajería → Regulaciones estatales de EE. UU. → Crear mensaje, idioma Español.
Solo afecta a usuarios en esos estados; no cambia nada en el código.

## Cómo comprobarlo

En debug la app fuerza la geografía EEE, así que el formulario aparece aunque estés en Perú. Los IDs de
prueba muestran el formulario de prueba de Google; el mensaje real se ve con un build **release** con los
IDs reales (cuando el mensaje está publicado).
