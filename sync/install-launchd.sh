#!/bin/zsh
# Instala la sincronización semanal como tarea de launchd del usuario.
# Uso: sync/install-launchd.sh <bucket>        p. ej. cazadores-peru.firebasestorage.app
#      sync/install-launchd.sh --uninstall
set -euo pipefail

LABEL="pe.cazadores.sync"
HOME_DIR="$HOME/Library/Application Support/CazadoresSync"
PLIST="$HOME/Library/LaunchAgents/$LABEL.plist"
LOG_DIR="$HOME/Library/Logs/CazadoresSync"
CREDENTIALS="$HOME/.config/cazadores-sync/service-account.json"

if [[ "${1:-}" == "--uninstall" ]]; then
  launchctl bootout "gui/$(id -u)/$LABEL" 2>/dev/null || true
  rm -f "$PLIST"
  echo "Tarea desinstalada. El estado sigue en: $HOME_DIR"
  exit 0
fi

BUCKET="${1:?Uso: $0 <bucket de Firebase Storage>}"
UV="$(command -v uv)" || { echo "Falta uv: brew install uv"; exit 1; }
[[ -f "$CREDENTIALS" ]] || { echo "Falta la cuenta de servicio en $CREDENTIALS"; exit 1; }

# Copia del script fuera del repo: launchd no siempre puede leer volúmenes externos.
mkdir -p "$HOME_DIR" "$LOG_DIR" "$(dirname "$PLIST")"
cp "$(dirname "$0")/sync_requisitoriados.py" "$HOME_DIR/sync_requisitoriados.py"

cat > "$PLIST" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key><string>$LABEL</string>
  <key>ProgramArguments</key>
  <array>
    <string>$UV</string><string>run</string><string>--quiet</string>
    <string>$HOME_DIR/sync_requisitoriados.py</string>
  </array>
  <key>EnvironmentVariables</key>
  <dict>
    <key>CZ_SYNC_BUCKET</key><string>$BUCKET</string>
    <key>CZ_SYNC_CREDENTIALS</key><string>$CREDENTIALS</string>
    <key>PATH</key><string>/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin</string>
  </dict>
  <!-- Dos intentos diarios; el script solo publica si pasaron 6 días desde la última vez. -->
  <key>StartCalendarInterval</key>
  <array>
    <dict><key>Hour</key><integer>10</integer><key>Minute</key><integer>0</integer></dict>
    <dict><key>Hour</key><integer>20</integer><key>Minute</key><integer>0</integer></dict>
  </array>
  <key>StandardOutPath</key><string>$LOG_DIR/sync.log</string>
  <key>StandardErrorPath</key><string>$LOG_DIR/sync.log</string>
  <key>ProcessType</key><string>Background</string>
  <key>LowPriorityIO</key><true/>
</dict>
</plist>
PLIST

launchctl bootout "gui/$(id -u)/$LABEL" 2>/dev/null || true
launchctl bootstrap "gui/$(id -u)" "$PLIST"
echo "Instalada. Corre a las 10:00 y 20:00; publica como máximo una vez por semana."
echo "Probar ahora:  launchctl kickstart -k gui/$(id -u)/$LABEL"
echo "Ver el log:    tail -f \"$LOG_DIR/sync.log\""
