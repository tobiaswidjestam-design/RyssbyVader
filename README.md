# Ryssby Weather (Linux)

A graphical weather app written in plain Java (Swing) for Ryssby, with data from
[open-meteo.com](https://open-meteo.com). No external libraries needed — just a JDK.

This is a personal side project, built outside my coursework with AI assistance.

## Features

- Current temperature and wind (m/s)
- 5-day forecast with max/min temperature and total precipitation
- Click a day to see the **rain hour by hour** as a bar chart, with mm values above each bar
- Time bands for night, morning, afternoon and evening, with all hourly values together
- Print the band table for the selected day
- Automatic refresh every 10 minutes, plus a manual **Refresh** button
- Console mode: `./run.sh --console`

## Getting started

```bash
./build.sh             # compile into out/
./run.sh               # start the app
./install-desktop.sh   # create desktop and menu shortcuts (Linux Mint)
```

Requires JDK 21 or later (developed and tested with JDK 26).

## Files

| File | Description |
|------|-------------|
| `Main.java` | Entry point (`--console` for console mode) |
| `WeatherService.java` | Fetches and parses the weather data from open-meteo |
| `WeatherGui.java` | Swing interface with the forecast table and the hourly rain chart |
| `build.sh` | Compiles into `out/` |
| `run.sh` | Starts the app (rebuilds first if `out/` is missing) |
| `install-desktop.sh` | Installs a `.desktop` launcher on the desktop and in the menu |
| `icon.png` | App icon (sun and cloud) |

## Android version

The Android version lives in the `android-app/` folder. Open that folder as a project in
Android Studio, wait for the Gradle sync to finish, then press **Run** and pick an emulator
or a connected Android phone.

It shows the current temperature, wind and a five-day forecast from the same Open-Meteo
source. Android Studio may need to download the Gradle plugin the first time.
