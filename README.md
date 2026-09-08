# Väderappen – Ryssby (Linux)

Grafisk väderapp skriven i ren Java (Swing) för Ryssby, med data från [open-meteo.com](https://open-meteo.com). Inga externa bibliotek behövs – bara ett JDK.

## Funktioner
- Aktuell temperatur och vind (m/s)
- 5-dagarsprognos med max/min-temperatur och total nederbörd
- Klicka på en dag för att se **nederbörden timme för timme** som stapeldiagram, med mm-värden ovanför varje stapel
- Tidsintervall natt, morgon, eftermiddag och kväll med alla timvärden samlade
- Skriv ut intervalltabellen för den valda dagen
- Automatisk uppdatering var 10:e minut + manuell "Uppdatera"-knapp
- Textläge i terminalen: `./run.sh --console`

## Kom igång

```bash
./build.sh             # kompilera till out/
./run.sh               # starta appen
./install-desktop.sh   # skapa genväg på skrivbordet och i programmenyn (Linux Mint)
```

Krav: JDK 21 eller senare (utvecklad och testad med JDK 26).

## Filer

| Fil | Beskrivning |
|-----|-------------|
| `Main.java` | Startpunkt (`--console` för textläget) |
| `WeatherService.java` | Hämtar och tolkar väderdata från open-meteo |
| `WeatherGui.java` | Swing-gränssnitt med prognostabell och timregndiagram |
| `build.sh` | Kompilera till `out/` |
| `run.sh` | Starta appen (bygger först om `out/` saknas) |
| `install-desktop.sh` | Installerar `.desktop`-startare på skrivbordet/menyn |
| `icon.png` | Appikon (sol + moln) |

## Android-version

Android-versionen finns i mappen `android-app/`. Öppna den mappen som ett projekt i Android Studio, vänta tills Gradle-synkroniseringen är klar och tryck sedan på **Run**. Välj en emulator eller en ansluten Android-telefon.

Den första Android-versionen visar aktuell temperatur, vind och femdagarsprognos från samma Open-Meteo-källa. Android Studio kan behöva ladda ner Gradle-pluginen första gången.
