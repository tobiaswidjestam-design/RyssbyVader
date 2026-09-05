#!/usr/bin/env bash
# Installerar genvägar så att appen kan startas från skrivbordet
# och programmenyn i Linux Mint (Cinnamon).
set -e
cd "$(dirname "$0")"

# Bygg alltid en färsk version innan vi skapar genvägen
./build.sh

APP_NAME="Väder Ryssby"
INSTALL_DIR="$(pwd)"
DESKTOP_FILE_NAME="vader-ryssby.desktop"

# Hitta skrivbordsmappen (på ett svenskt system heter den "Skrivbord")
DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")"
mkdir -p "$DESKTOP_DIR"

# Använd egen ikon om den finns, annars temats väderikon
ICON="$INSTALL_DIR/icon.png"
if [ ! -f "$ICON" ]; then
    ICON="weather-clear"
fi

skriv_desktop_fil() {
    cat > "$1" <<EOF
[Desktop Entry]
Type=Application
Name=$APP_NAME
Comment=Väderprognos för Ryssby
Exec=$INSTALL_DIR/run.sh
Path=$INSTALL_DIR
Icon=$ICON
Terminal=false
StartupNotify=false
Categories=Network;Utility;
Keywords=väder;weather;prognos;
EOF
}

# 1. Genväg i programmenyn
mkdir -p "$HOME/.local/share/applications"
skriv_desktop_fil "$HOME/.local/share/applications/$DESKTOP_FILE_NAME"

# 2. Genväg på skrivbordet
skriv_desktop_fil "$DESKTOP_DIR/$DESKTOP_FILE_NAME"
chmod +x "$DESKTOP_DIR/$DESKTOP_FILE_NAME"

# Cinnamon frågar annars "Vill du lita på den här programstartaren?"
command -v gio >/dev/null 2>&1 && \
    gio set "$DESKTOP_DIR/$DESKTOP_FILE_NAME" metadata::trusted true 2>/dev/null || true

# Uppdatera programmenyn
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true

echo ""
echo "Klart! '$APP_NAME' kan nu startas från skrivbordet och programmenyn."