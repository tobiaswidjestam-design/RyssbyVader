package se.ryssby.vader;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String DEFAULT_CITY = "Ryssby";
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
            + "?name=%s&count=1&language=en&format=json";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<String> favoriteCities = new ArrayList<>(Arrays.asList("Ryssby", "Boda Glasbruk", "Cebu City", "Bohol"));
    private boolean darkMode = false;
    private LinearLayout forecastContainer;
    private LinearLayout hourlyContainer;
    private LinearLayout favoritesContainer;
    private TextView currentTemperature;
    private TextView currentWind;
    private TextView currentCondition;
    private TextView currentDetails;
    private TextView weatherIcon;
    private TextView locationLabel;
    private TextView subtitleLabel;
    private TextView dayHeading;
    private Button backToNowButton;
    private int selectedForecastDay = -1;
    private final List<LinearLayout> forecastRows = new ArrayList<>();
    private TextView status;
    private ProgressBar progress;
    private EditText cityInput;
    private Button refreshButton;
    private Button darkModeButton;
    private LinearLayout hourlyChips;
    private WeatherData lastData;
    private int selectedHourlyDay = 0;
    private final DecimalFormat numberFormat = new DecimalFormat("0.0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        loadWeatherForCity("Boda Glasbruk");
    }

    private void buildScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(darkMode ? Color.rgb(15, 23, 35) : Color.rgb(239, 246, 252));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(28));
        scrollView.addView(page);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        locationLabel = text("Boda Glasbruk", 28, darkMode ? Color.WHITE : Color.rgb(19, 57, 79));
        locationLabel.setTypeface(null, 1);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        headerRow.addView(locationLabel, titleParams);

        darkModeButton = new Button(this);
        darkModeButton.setText(darkMode ? "Light" : "Dark");
        darkModeButton.setTextColor(Color.WHITE);
        darkModeButton.setBackground(cardBackground(Color.rgb(41, 98, 255), 12));
        darkModeButton.setOnClickListener(view -> toggleDarkMode());
        headerRow.addView(darkModeButton, new LinearLayout.LayoutParams(-2, -2));
        page.addView(headerRow, matchWrap());

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(0, dp(8), 0, dp(14));

        cityInput = new EditText(this);
        cityInput.setHint("Search city");
        cityInput.setText("Boda Glasbruk");
        cityInput.setTextSize(16f);
        cityInput.setSingleLine(true);
        cityInput.setBackground(cardBackground(Color.WHITE, 12));
        cityInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        cityInput.setTextColor(Color.rgb(17, 50, 72));
        cityInput.setHintTextColor(Color.rgb(123, 136, 143));
        cityInput.setOnEditorActionListener((v, actionId, event) -> {
            loadWeather();
            return true;
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, -2, 1);
        inputParams.setMargins(0, 0, dp(8), 0);
        searchRow.addView(cityInput, inputParams);

        Button searchButton = new Button(this);
        searchButton.setText("Search");
        searchButton.setTextColor(Color.WHITE);
        searchButton.setBackground(cardBackground(Color.rgb(25, 116, 126), 12));
        searchButton.setOnClickListener(view -> loadWeather());
        searchRow.addView(searchButton, new LinearLayout.LayoutParams(-2, -2));
        page.addView(searchRow, matchWrap());

        favoritesContainer = new LinearLayout(this);
        favoritesContainer.setOrientation(LinearLayout.HORIZONTAL);
        favoritesContainer.setPadding(0, 0, 0, dp(14));
        renderFavorites();
        page.addView(favoritesContainer, matchWrap());

        subtitleLabel = text("Weather right now", 14, darkMode ? Color.rgb(187, 205, 216) : Color.rgb(96, 116, 130));
        page.addView(subtitleLabel, marginParams(0, 2, 0, 14));

        LinearLayout current = new LinearLayout(this);
        current.setOrientation(LinearLayout.VERTICAL);
        current.setPadding(dp(22), dp(22), dp(22), dp(22));
        current.setBackground(cardBackground(darkMode ? Color.rgb(26, 47, 68) : Color.rgb(213, 235, 245), 26));
        current.setElevation(dp(3));
        page.addView(current, marginParams(0, 0, 0, 20));

        // Heading with the day's name and date when a forecast day is selected
        dayHeading = text("", 20, darkMode ? Color.rgb(255, 214, 102) : Color.rgb(41, 98, 255));
        dayHeading.setTypeface(null, 1);
        dayHeading.setVisibility(View.GONE);
        current.addView(dayHeading, matchWrap());

        LinearLayout tempRow = new LinearLayout(this);
        tempRow.setOrientation(LinearLayout.HORIZONTAL);
        tempRow.setGravity(Gravity.CENTER_VERTICAL);

        weatherIcon = text("☀️", 34, darkMode ? Color.rgb(255, 214, 102) : Color.rgb(17, 50, 72));
        tempRow.addView(weatherIcon, new LinearLayout.LayoutParams(-2, -2));

        currentTemperature = text("-- °C", 58, darkMode ? Color.WHITE : Color.rgb(17, 50, 72));
        currentTemperature.setTypeface(null, 1);
        tempRow.addView(currentTemperature, new LinearLayout.LayoutParams(-2, -2));
        current.addView(tempRow, matchWrap());

        currentCondition = text("Partly cloudy", 15, darkMode ? Color.rgb(193, 219, 246) : Color.rgb(36, 79, 92));
        currentCondition.setPadding(0, dp(2), 0, 0);
        current.addView(currentCondition, matchWrap());

        currentDetails = text("Max --° • Min --° • Rain -- mm", 14, darkMode ? Color.rgb(188, 217, 255) : Color.rgb(44, 118, 185));
        currentDetails.setPadding(0, dp(4), 0, 0);
        current.addView(currentDetails, matchWrap());

        currentWind = text("Wind -- m/s", 16, darkMode ? Color.rgb(220, 234, 245) : Color.rgb(36, 79, 92));
        currentWind.setPadding(0, dp(5), 0, 0);
        current.addView(currentWind, matchWrap());

        // Button to go back to Weather right now
        backToNowButton = new Button(this);
        backToNowButton.setText("Weather now");
        backToNowButton.setAllCaps(false);
        backToNowButton.setTextColor(Color.WHITE);
        backToNowButton.setBackground(cardBackground(Color.rgb(25, 116, 126), 14));
        backToNowButton.setOnClickListener(view -> showLiveWeather());
        backToNowButton.setVisibility(View.GONE);
        current.addView(backToNowButton, marginParams(0, 6, 0, 0));

        TextView forecastTitle = text("5-day forecast", 22, darkMode ? Color.WHITE : Color.rgb(19, 57, 79));
        forecastTitle.setTypeface(null, 1);
        page.addView(forecastTitle, marginParams(0, 0, 0, 10));

        forecastContainer = new LinearLayout(this);
        forecastContainer.setOrientation(LinearLayout.VERTICAL);
        page.addView(forecastContainer, matchWrap());

        TextView hourlyTitle = text("Hour by hour", 22, darkMode ? Color.WHITE : Color.rgb(19, 57, 79));
        hourlyTitle.setTypeface(null, 1);
        page.addView(hourlyTitle, marginParams(0, 18, 0, 10));

        // Day picker: one button per forecast day
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        hourlyChips = new LinearLayout(this);
        hourlyChips.setOrientation(LinearLayout.HORIZONTAL);
        chipScroll.addView(hourlyChips, new HorizontalScrollView.LayoutParams(-2, -2));
        page.addView(chipScroll, matchWrap());

        HorizontalScrollView hourlyScroll = new HorizontalScrollView(this);
        hourlyScroll.setHorizontalScrollBarEnabled(false);
        hourlyContainer = new LinearLayout(this);
        hourlyContainer.setOrientation(LinearLayout.HORIZONTAL);
        hourlyScroll.addView(hourlyContainer, new HorizontalScrollView.LayoutParams(-2, -2));
        page.addView(hourlyScroll, matchWrap());

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(18), 0, 0);
        status = text("Starting...", 14, Color.rgb(79, 96, 103));
        footer.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        footer.addView(progress, new LinearLayout.LayoutParams(dp(28), dp(28)));
        refreshButton = new Button(this);
        refreshButton.setText("Refresh");
        refreshButton.setTextColor(Color.WHITE);
        refreshButton.setBackground(cardBackground(Color.rgb(25, 116, 126), 14));
        refreshButton.setOnClickListener(view -> loadWeather());
        footer.addView(refreshButton, new LinearLayout.LayoutParams(-2, -2));
        page.addView(footer, matchWrap());

        // Signature at the bottom so it is clear who built the app
        TextView signature = text("StudioRyssby 2026", 14,
                darkMode ? Color.rgb(165, 185, 198) : Color.rgb(84, 104, 118));
        signature.setTypeface(null, 1); // bold so it stands out
        signature.setGravity(Gravity.CENTER);
        page.addView(signature, marginParams(0, 20, 0, 8));

        setContentView(scrollView);
    }

    private void loadWeather() {
        String city = cityInput.getText().toString().trim();
        if (city.isEmpty()) {
            city = DEFAULT_CITY;
            cityInput.setText(city);
        }
        loadWeatherForCity(city);
    }

    private void loadWeatherForCity(String city) {
        refreshButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        status.setText("Fetching weather for " + city + "...");
        executor.execute(() -> {
            try {
                GeoLocation location = geocodeCity(city);
                WeatherData data = fetchWeather(location.latitude, location.longitude);
                runOnUiThread(() -> showWeather(data, location.name));
            } catch (Exception exception) {
                runOnUiThread(() -> showError(exception));
            }
        });
    }

    private GeoLocation geocodeCity(String city) throws Exception {
        String encoded = java.net.URLEncoder.encode(city, StandardCharsets.UTF_8.name());
        HttpURLConnection connection = (HttpURLConnection) new URL(String.format(Locale.US, GEOCODING_URL, encoded)).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + connection.getResponseCode());
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            JSONObject root = new JSONObject(body.toString());
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) {
                throw new IllegalArgumentException("Could not find a city named \"" + city + "\".");
            }
            JSONObject place = results.getJSONObject(0);
            return new GeoLocation(
                    place.optString("name", city),
                    place.getDouble("latitude"),
                    place.getDouble("longitude")
            );
        } finally {
            connection.disconnect();
        }
    }

    private WeatherData fetchWeather(double latitude, double longitude) throws Exception {
        String apiUrl = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current_weather=true"
            + "&windspeed_unit=ms"
            + "&hourly=temperature_2m,precipitation,windspeed_10m"
            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weathercode"
                + "&timezone=auto";

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + connection.getResponseCode());
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            JSONObject root = new JSONObject(body.toString());
            JSONObject current = root.getJSONObject("current_weather");
            JSONObject daily = root.getJSONObject("daily");
            JSONObject hourly = root.getJSONObject("hourly");
            JSONArray dates = daily.getJSONArray("time");
            JSONArray max = daily.getJSONArray("temperature_2m_max");
            JSONArray min = daily.getJSONArray("temperature_2m_min");
            JSONArray rain = daily.getJSONArray("precipitation_sum");
            JSONArray codes = daily.getJSONArray("weathercode");
            JSONArray hourlyTimes = hourly.getJSONArray("time");
            JSONArray hourlyTemp = hourly.getJSONArray("temperature_2m");
            JSONArray hourlyRain = hourly.getJSONArray("precipitation");
            JSONArray hourlyWind = hourly.getJSONArray("windspeed_10m");
            WeatherDay[] days = new WeatherDay[dates.length()];
            for (int index = 0; index < dates.length(); index++) {
                days[index] = new WeatherDay(dates.getString(index), max.optDouble(index),
                        min.optDouble(index), rain.optDouble(index), codes.optInt(index));
            }
            int hourCount = hourlyTimes.length();
            WeatherHour[] hours = new WeatherHour[hourCount];
            for (int index = 0; index < hourCount; index++) {
                hours[index] = new WeatherHour(hourlyTimes.getString(index),
                        hourlyTemp.optDouble(index),
                        hourlyRain.optDouble(index),
                        hourlyWind.optDouble(index));
            }
            return new WeatherData(current.getDouble("temperature"),
                    current.getDouble("windspeed"), days, hours);
        } finally {
            connection.disconnect();
        }
    }

    private void showWeather(WeatherData data, String cityName) {
        locationLabel.setText(cityName);
        forecastContainer.removeAllViews();
        lastData = data;
        if (selectedHourlyDay >= data.days.length) {
            selectedHourlyDay = 0;
        }
        selectedForecastDay = -1; // always start with Weather right now
        forecastRows.clear();
        renderHourlySection(data);

        for (WeatherDay day : data.days) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.setBackground(cardBackground(Color.WHITE, 18));
            row.setElevation(dp(2));
            forecastRows.add(row);
            row.setOnClickListener(view -> {
                int index = forecastRows.indexOf(row);
                selectedForecastDay = (selectedForecastDay == index) ? -1 : index;
                updateCurrentCard();
            });

            TextView rowIcon = text(iconForWeatherCode(day.code), 24, Color.rgb(17, 50, 72));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(-2, -2);
            iconParams.setMargins(0, 0, dp(10), 0);
            row.addView(rowIcon, iconParams);

            LinearLayout dayInfo = new LinearLayout(this);
            dayInfo.setOrientation(LinearLayout.VERTICAL);
            dayInfo.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

            TextView dayName = text(weekday(day.date), 17, Color.rgb(17, 50, 72));
            dayName.setTypeface(null, 1);
            dayInfo.addView(dayName, matchWrap());

            TextView dayDate = text(compactDate(day.date), 12, Color.rgb(104, 120, 130));
            dayInfo.addView(dayDate, marginParams(0, 4, 0, 0));

            LinearLayout tempBlock = new LinearLayout(this);
            tempBlock.setOrientation(LinearLayout.VERTICAL);
            tempBlock.setGravity(Gravity.END);

            TextView maxTemp = text("Max " + numberFormat.format(day.max) + "°", 15,
                    Color.rgb(17, 50, 72));
            maxTemp.setGravity(Gravity.END);
            tempBlock.addView(maxTemp, matchWrap());

            TextView minTemp = text("Min " + numberFormat.format(day.min) + "°", 14,
                    Color.rgb(104, 120, 130));
            minTemp.setGravity(Gravity.END);
            tempBlock.addView(minTemp, marginParams(0, 4, 0, 0));

            LinearLayout rainBlock = new LinearLayout(this);
            rainBlock.setOrientation(LinearLayout.VERTICAL);
            rainBlock.setGravity(Gravity.END);
            rainBlock.setPadding(dp(12), 0, 0, 0);

            TextView rainLabel = text("Rain", 11, Color.rgb(39, 116, 136));
            rainLabel.setGravity(Gravity.END);
            rainBlock.addView(rainLabel, matchWrap());

            TextView rainValue = text(numberFormat.format(day.rain) + " mm", 13,
                    Color.rgb(25, 116, 126));
            rainValue.setGravity(Gravity.END);
            rainBlock.addView(rainValue, marginParams(0, 4, 0, 0));

            row.addView(dayInfo);
            row.addView(tempBlock, new LinearLayout.LayoutParams(-2, -2));
            row.addView(rainBlock, new LinearLayout.LayoutParams(-2, -2));
            forecastContainer.addView(row, marginParams(0, 0, 0, 8));
        }

        updateCurrentCard();

        status.setText("Updated now");
        refreshButton.setEnabled(true);
        progress.setVisibility(View.GONE);
    }

    /** Updates the top card: weather right now, or the forecast for the selected day. */
    private void updateCurrentCard() {
        if (lastData == null || lastData.days.length == 0) {
            return;
        }
        boolean liveMode = selectedForecastDay < 0 || selectedForecastDay >= lastData.days.length;
        WeatherDay day = lastData.days[liveMode ? 0 : selectedForecastDay];

        subtitleLabel.setText(liveMode ? "Weather right now" : "Forecast");
        dayHeading.setVisibility(liveMode ? View.GONE : View.VISIBLE);
        backToNowButton.setVisibility(liveMode ? View.GONE : View.VISIBLE);
        if (!liveMode) {
            dayHeading.setText(chipLabel(day.date));
        }

        for (int i = 0; i < forecastRows.size(); i++) {
            LinearLayout row = forecastRows.get(i);
            row.setBackground(cardBackground(i == selectedForecastDay
                    ? Color.rgb(186, 216, 255) : Color.WHITE, 18));
        }

        String detaljer = "Max " + numberFormat.format(day.max) + "° • Min "
                + numberFormat.format(day.min) + "° • Rain " + numberFormat.format(day.rain) + " mm";
        if (liveMode) {
            weatherIcon.setText(weatherIconFor(lastData.temperature, lastData.wind));
            currentTemperature.setText(numberFormat.format(lastData.temperature) + " °C");
            currentCondition.setText(weatherDescription(lastData.temperature, lastData.wind));
            currentCondition.setTextColor(weatherTextColor(lastData.temperature));
            currentDetails.setText(detaljer);
            currentWind.setText("Wind " + numberFormat.format(lastData.wind) + " m/s");
        } else {
            double maxWind = maxWindForDay(day.date);
            weatherIcon.setText(iconForWeatherCode(day.code));
            currentTemperature.setText(numberFormat.format(day.max) + " °C");
            currentCondition.setText("Forecast for the day");
            currentCondition.setTextColor(darkMode ? Color.rgb(193, 219, 246) : Color.rgb(36, 79, 92));
            currentDetails.setText(detaljer);
            currentWind.setText("Wind up to "
                    + (maxWind > 0 ? numberFormat.format(maxWind) : "--") + " m/s");
        }
    }

    /** Goes back to Weather right now. */
    private void showLiveWeather() {
        selectedForecastDay = -1;
        updateCurrentCard();
    }

    /** The highest wind speed during a date (from the hourly data). */
    private double maxWindForDay(String date) {
        double max = 0;
        if (lastData != null) {
            String prefix = date + "T";
            for (WeatherHour hour : lastData.hours) {
                if (hour.time.startsWith(prefix) && !Double.isNaN(hour.wind)) {
                    max = Math.max(max, hour.wind);
                }
            }
        }
        return max;
    }

    /** Weather icon from the WMO weather code from Open-Meteo. */
    private static String iconForWeatherCode(int code) {
        switch (code) {
            case 0: return "☀️";
            case 1: return "🌤️";
            case 2: return "⛅";
            case 3: return "☁️";
            case 45:
            case 48: return "🌫️";
            case 51:
            case 53:
            case 55:
            case 56:
            case 57: return "🌦️";
            case 61:
            case 63:
            case 65:
            case 66:
            case 67:
            case 80:
            case 81:
            case 82: return "🌧️";
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86: return "🌨️";
            case 95:
            case 96:
            case 99: return "⛈️";
            default: return "☁️";
        }
    }

    private String weekday(String date) {
        DayOfWeek dayOfWeek = LocalDate.parse(date).getDayOfWeek();
        return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("en-GB"));
    }

    private String compactDate(String date) {
        return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("yyMMdd"));
    }

    private String shortHour(String dateTime) {
        return LocalDateTime.parse(dateTime).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private GradientDrawable cardBackground(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private String weatherDescription(double temperature, double wind) {
        if (temperature >= 22 && wind < 8) {
            return "Sunny and clear";
        }
        if (temperature >= 16) {
            return "Partly cloudy";
        }
        if (temperature <= 2) {
            return "Snowy";
        }
        if (wind >= 8) {
            return "Windy";
        }
        if (temperature <= 8) {
            return "Cold";
        }
        return "Changeable weather";
    }

    private String weatherIconFor(double temperature, double wind) {
        if (temperature >= 25 && wind < 8) {
            return "☀️";
        }
        if (temperature >= 18) {
            return "⛅";
        }
        if (temperature <= 2) {
            return "❄️";
        }
        if (temperature <= 8) {
            return "🌡️";
        }
        if (wind >= 8) {
            return "💨";
        }
        if (temperature <= 12) {
            return "🌧️";
        }
        return "☁️";
    }

    private int weatherTextColor(double temperature) {
        if (temperature <= 8) {
            return Color.rgb(39, 104, 196);
        }
        if (temperature >= 22) {
            return Color.rgb(210, 106, 39);
        }
        return darkMode ? Color.rgb(193, 219, 246) : Color.rgb(36, 79, 92);
    }

    private void renderFavorites() {
        favoritesContainer.removeAllViews();
        for (String favorite : favoriteCities) {
            Button favoriteButton = new Button(this);
            favoriteButton.setText(favorite);
            favoriteButton.setTextColor(Color.WHITE);
            favoriteButton.setBackground(cardBackground(Color.rgb(92, 121, 151), 18));
            favoriteButton.setOnClickListener(view -> {
                cityInput.setText(favorite);
                loadWeather();
            });
            favoritesContainer.addView(favoriteButton, new LinearLayout.LayoutParams(-2, -2));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) favoriteButton.getLayoutParams();
            params.setMargins(0, 0, dp(8), 0);
        }
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        buildScreen();
        String city = cityInput.getText().toString().trim();
        if (!city.isEmpty()) {
            loadWeatherForCity(city);
        }
    }

    /** Draws the day pickers and the hourly cards (00:00–23:00) for the selected day. */
    private void renderHourlySection(WeatherData data) {
        hourlyChips.removeAllViews();
        hourlyContainer.removeAllViews();
        for (int i = 0; i < data.days.length; i++) {
            final int index = i;
            WeatherDay day = data.days[i];
            boolean selected = index == selectedHourlyDay;
            Button chip = new Button(this);
            chip.setAllCaps(false);
            chip.setTextSize(13f);
            chip.setText(chipLabel(day.date));
            chip.setTextColor(selected ? Color.WHITE
                    : (darkMode ? Color.rgb(193, 219, 246) : Color.rgb(17, 50, 72)));
            chip.setBackground(cardBackground(selected ? Color.rgb(41, 98, 255)
                    : (darkMode ? Color.rgb(26, 47, 68) : Color.WHITE), 14));
            chip.setOnClickListener(view -> {
                selectedHourlyDay = index;
                renderHourlySection(lastData);
            });
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, -2);
            chipParams.setMargins(0, 0, dp(8), 0);
            hourlyChips.addView(chip, chipParams);
        }

        if (selectedHourlyDay >= data.days.length) {
            return;
        }
        String dayPrefix = data.days[selectedHourlyDay].date + "T";
        for (WeatherHour hour : data.hours) {
            if (!hour.time.startsWith(dayPrefix)) {
                continue;
            }
            LinearLayout hourCard = new LinearLayout(this);
            hourCard.setOrientation(LinearLayout.VERTICAL);
            hourCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            hourCard.setBackground(cardBackground(darkMode ? Color.rgb(26, 47, 68) : Color.rgb(213, 235, 245), 18));
            hourCard.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) hourCard.getLayoutParams();
            params.setMargins(0, 0, dp(8), 0);

            TextView hourLabel = text(shortHour(hour.time), 12,
                    darkMode ? Color.rgb(193, 219, 246) : Color.rgb(72, 98, 116));
            hourCard.addView(hourLabel, matchWrap());

            TextView hourTemp = text(numberFormat.format(hour.temperature) + "°", 20,
                    darkMode ? Color.WHITE : Color.rgb(17, 50, 72));
            hourTemp.setTypeface(null, 1);
            hourCard.addView(hourTemp, marginParams(0, 6, 0, 0));

            TextView hourRain = text(numberFormat.format(hour.rain) + " mm", 11,
                    darkMode ? Color.rgb(154, 198, 255) : Color.rgb(25, 116, 126));
            hourCard.addView(hourRain, marginParams(0, 4, 0, 0));

            hourlyContainer.addView(hourCard);
        }
    }

    /** Short label for a day, e.g. "Tue 8/9". */
    private static String chipLabel(String date) {
        Locale sv = new Locale("en", "GB");
        LocalDate dag = LocalDate.parse(date);
        String vardag = dag.getDayOfWeek().getDisplayName(TextStyle.SHORT, sv);
        return vardag.substring(0, 1).toUpperCase(sv) + vardag.substring(1)
                + " " + dag.getDayOfMonth() + "/" + dag.getMonthValue();
    }

    private void showError(Exception exception) {
        status.setText("Could not fetch weather data");
        refreshButton.setEnabled(true);
        progress.setVisibility(View.GONE);
        String message = exception.getMessage() != null ? exception.getMessage() : "Unknown error";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams marginParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private static class GeoLocation {
        final String name;
        final double latitude;
        final double longitude;

        GeoLocation(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static class WeatherData {
        final double temperature;
        final double wind;
        final WeatherDay[] days;
        final WeatherHour[] hours;

        WeatherData(double temperature, double wind, WeatherDay[] days, WeatherHour[] hours) {
            this.temperature = temperature;
            this.wind = wind;
            this.days = days;
            this.hours = hours;
        }
    }

    private static class WeatherHour {
        final String time;
        final double temperature;
        final double rain;
        final double wind;

        WeatherHour(String time, double temperature, double rain, double wind) {
            this.time = time;
            this.temperature = temperature;
            this.rain = rain;
            this.wind = wind;
        }
    }

    private static class WeatherDay {
        final String date;
        final double max;
        final double min;
        final double rain;
        final int code;

        WeatherDay(String date, double max, double min, double rain, int code) {
            this.date = date;
            this.max = max;
            this.min = min;
            this.rain = rain;
            this.code = code;
        }
    }
}
