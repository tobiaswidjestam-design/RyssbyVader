#!/usr/bin/env bash
# Kompilera projektet till mappen out/
set -e
cd "$(dirname "$0")"

rm -rf out
mkdir -p out

javac -encoding UTF-8 -d out Main.java WeatherService.java WeatherGui.java

echo "Klart! Kör appen med: ./run.sh"