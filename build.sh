#!/usr/bin/env bash
# Compile the project into the out/ directory
set -e
cd "$(dirname "$0")"

rm -rf out
mkdir -p out

javac -encoding UTF-8 -d out Main.java WeatherService.java WeatherGui.java

echo "Done! Run the app with: ./run.sh"