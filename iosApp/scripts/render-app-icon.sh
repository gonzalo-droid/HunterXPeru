#!/bin/sh
# Genera el AppIcon de iOS desde los SVG de marca (docs/brand). Requiere rsvg-convert (brew install librsvg).
# iOS exige el ícono claro de 1024 px sin canal alfa; el oscuro y el tintado (iOS 18) van sin fondo.
set -eu
root=$(cd "$(dirname "$0")/../.." && pwd)
brand="$root/docs/brand"
out="$root/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
# El claro es el PNG exportado por diseño (opaco, sin alfa): se copia tal cual.
cp "$brand/ios_app_icon_1024.png" "$out/app-icon-1024.png"
rsvg-convert -w 1024 -h 1024 "$brand/ios_app_icon_dark.svg" -o "$out/app-icon-1024-dark.png"
rsvg-convert -w 1024 -h 1024 "$brand/ios_app_icon_tinted.svg" -o "$out/app-icon-1024-tinted.png"
