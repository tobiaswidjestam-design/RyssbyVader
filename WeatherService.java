import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hämtar och tolkar väderdata för Ryssby från open-meteo.com.
 */
public class WeatherService {

    private static final String URL =
            "https://api.open-meteo.com/v1/forecast?latitude=56.93&longitude=14.33"
            + "&current_weather=true"
            + "&windspeed_unit=ms"
            + "&hourly=precipitation"
            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum&timezone=auto";

    /** Prognosen för en enskild dag. */
    public record DagsPrognos(LocalDate datum, double maxTemp, double minTemp, double regn) {}

    /** Nederbörden för en enskild timme. */
    public record TimmarsNederbord(LocalDateTime tid, double mm) {}

    /** All väderdata som GUI:t/konsolen visar. */
    public record VaderData(double temperatur, double vindhastighet,
                            List<DagsPrognos> dagar, List<TimmarsNederbord> timmar) {}

    /** Hämtar väderdatat från API:et och tolkar svaret. */
    public static VaderData hamtaVader() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("API:et svarade med HTTP " + response.statusCode());
        }
        String json = response.body();

        // 1. Hämta aktuell temperatur och vind (under "current_weather")
        double temp = hamtaTal(json, "current_weather", "temperature");
        double vind = hamtaTal(json, "current_weather", "windspeed");

        // 2. Hämta daglig data (under "daily")
        int dailyIdx = json.indexOf("\"daily\":");
        String[] dates = hamtaArray(json, dailyIdx, "time");
        String[] maxDays = hamtaArray(json, dailyIdx, "temperature_2m_max");
        String[] minDays = hamtaArray(json, dailyIdx, "temperature_2m_min");
        String[] precipDays = hamtaArray(json, dailyIdx, "precipitation_sum");

        List<DagsPrognos> dagar = new ArrayList<>();
        for (int i = 0; i < dates.length; i++) {
            dagar.add(new DagsPrognos(
                    LocalDate.parse(dates[i].trim()),
                    hamtaTalEller(maxDays, i),
                    hamtaTalEller(minDays, i),
                    hamtaTalEller(precipDays, i)));
        }

        // 3. Hämta nederbörd per timme (under "hourly")
        int hourlyIdx = json.indexOf("\"hourly\":");
        String[] hourlyTider = hamtaArray(json, hourlyIdx, "time");
        String[] hourlyRegn = hamtaArray(json, hourlyIdx, "precipitation");

        List<TimmarsNederbord> timmar = new ArrayList<>();
        for (int i = 0; i < hourlyTider.length; i++) {
            double mm = i < hourlyRegn.length ? Double.parseDouble(hourlyRegn[i].trim()) : 0;
            timmar.add(new TimmarsNederbord(LocalDateTime.parse(hourlyTider[i].trim()), mm));
        }

        return new VaderData(temp, vind, dagar, timmar);
    }

    /** Hittar ett tal efter en nyckel, t.ex. "temperature": 12.4 */
    private static double hamtaTal(String json, String sektion, String nyckel) {
        int sektionIdx = sektion == null ? 0 : json.indexOf("\"" + sektion + "\":");
        int nyckelIdx = json.indexOf("\"" + nyckel + "\":", sektionIdx);
        if (nyckelIdx == -1) {
            throw new IllegalStateException("Kunde inte hitta \"" + nyckel + "\" i svaret.");
        }
        int start = nyckelIdx + nyckel.length() + 3; // hoppa förbi citattecken och kolon
        int slut = json.indexOf(",", start);
        if (slut == -1) {
            slut = json.indexOf("}", start);
        }
        return Double.parseDouble(json.substring(start, slut).trim());
    }

    /** Hittar en JSON-array efter en nyckel, t.ex. "time": ["2026-09-05", ...] */
    private static String[] hamtaArray(String json, int franIndex, String nyckel) {
        int nyckelIdx = json.indexOf("\"" + nyckel + "\":", franIndex);
        if (nyckelIdx == -1) {
            return new String[0];
        }
        int start = json.indexOf("[", nyckelIdx);
        int slut = json.indexOf("]", start);
        // Tar bort citattecken kring värdena (t.ex. datumen)
        return json.substring(start + 1, slut).replace("\"", "").split(",");
    }

    private static double hamtaTalEller(String[] arr, int i) {
        return i < arr.length ? Double.parseDouble(arr[i].trim()) : 0;
    }

    /** Skriver ut vädret som text (gamla konsolläget, --console). */
    public static void skrivKonsol(VaderData vader) {
        Locale svLocale = Locale.of("sv", "SE");

        System.out.println("=== VÄDER FÖR RYSSBY ===");
        System.out.println("Just nu: " + formatera(vader.temperatur(), svLocale) + " °C"
                + ", Vind: " + formatera(vader.vindhastighet(), svLocale) + " m/s\n");
        System.out.println("Prognos kommande dagar:");

        List<DagsPrognos> dagar = vader.dagar();
        for (int i = 0; i < 7 && i < dagar.size(); i++) {
            DagsPrognos dag = dagar.get(i);
            System.out.println(formateraDag(dag.datum(), svLocale)
                    + " -> Max: " + formatera(dag.maxTemp(), svLocale) + " °C"
                    + ", Min: " + formatera(dag.minTemp(), svLocale) + " °C"
                    + ", Regn: " + formatera(dag.regn(), svLocale) + " mm");
        }
    }

    /** Formaterar till veckodag och datum (t.ex. "Torsdag 05/09") med stor begynnelsebokstav. */
    private static String formateraDag(LocalDate datum, Locale svLocale) {
        String formattedDay = datum.format(DateTimeFormatter.ofPattern("EEEE dd/MM", svLocale));
        return formattedDay.substring(0, 1).toUpperCase(svLocale) + formattedDay.substring(1);
    }

    /** Formaterar tal med svensk decimal (t.ex. 12,4). */
    private static String formatera(double tal, Locale svLocale) {
        return String.format(svLocale, "%.1f", tal);
    }
}