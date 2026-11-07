#!/usr/bin/env bash
# Installs shortcuts so the app can be started from the desktop
# and from the application menu in Linux Mint (Cinnamon).
set -e
cd "$(dirname "$0")"

# Always build a fresh version before creating the shortcut
./build.sh

APP_NAME="Ryssby Weather"
INSTALL_DIR="$(pwd)"
DESKTOP_FILE_NAME="vader-ryssby.desktop"

# Find the desktop directory (on a Swedish system it is called "Skrivbord")
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")"
mkdir -p "$DESKTOP_DIR"

# Use the custom icon if present, otherwise the theme's weather icon
ICON="$INSTALL_DIR/icon.png"
if [ ! -f "$ICON" ]; then
    ICON="weather-clear"
fi

skriv_desktop_fil() {
    cat > "$1" <<EOF
[Desktop Entry]
Type=Application
Name=$APP_NAME
Comment=Weather forecast for Ryssby
Exec=$INSTALL_DIR/run.sh
Path=$INSTALL_DIR
Icon=$ICON
Terminal=false
StartupNotify=false
Categories=Network;Utility;
Keywords=weather;forecast;
EOF
}

# 1. Shortcut in the application menu
mkdir -p "$HOME/.local/share/applications"
skriv_desktop_fil "$HOME/.local/share/applications/$DESKTOP_FILE_NAME"

# 2. Shortcut on the desktop
skriv_desktop_fil "$DESKTOP_DIR/$DESKTOP_FILE_NAME"
chmod +x "$DESKTOP_DIR/$DESKTOP_FILE_NAME"

# Cinnamon otherwise asks "Do you trust this launcher?"
command -v gio >/dev/null 2>&1 && \
    gio set "$DESKTOP_DIR/$DESKTOP_FILE_NAME" metadata::trusted true 2>/dev/null || true

# Update the application menu
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true

echo ""
echo "Done! '$APP_NAME' can now be started from the desktop and the application menu."