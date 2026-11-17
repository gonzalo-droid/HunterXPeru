package pe.cazadores.platform

import androidx.compose.ui.graphics.ImageBitmap

/** PNG sin pérdida del bitmap (para compartir el cartel). Costoso: llamarlo fuera del hilo principal. */
expect fun ImageBitmap.encodePng(): ByteArray
