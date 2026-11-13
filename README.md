# Weather (`com.aistudio.weather.glsmph`)

**Weather** is a clean, ad-free Android weather forecast application built with Kotlin and Jetpack Compose. It provides live local weather conditions, a full 24-hour clickable hourly forecast, a 10-day extended forecast, multi-city search and management, animated sky scenes, optional weather notifications, and an Android home screen widget.

- **App Name:** Weather
- **Package Name:** `com.aistudio.weather.glsmph`
- **Category:** Weather
- **Developer:** Antonee
- **Version:** `5.1.0` (`versionCode = 6`)
- **Minimum Android Version:** Android 8.0 Oreo (API 26)
- **Target Android Version:** API 36
- **Official Repository:** [https://github.com/Antoneeroy/Weather](https://github.com/Antoneeroy/Weather)
- **Privacy Policy:** [https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md](https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md)

---

## How the App Works

1. **Choose a Location (GPS or Manual City Search)**
   - Tap **Use My GPS** to detect your current location (after an on-demand location disclosure prompt), **or** open the **Cities & Search** tab to search for any city worldwide without granting location permission.
   - Save multiple cities, mark favorites (`★`), set a default startup city, or remove saved locations at any time.

2. **Home Weather Dashboard**
   - Displays current temperature, "Feels like" temperature, daily high/low range, WMO weather condition description, and dynamic sky animations (sun, realistic cratered moon, clouds, rain, snow, fog, and thunderstorm effects) that adapt to daytime and nighttime in the selected city.
   - Includes an **Atmospheric Details** grid showing Humidity & Dew Point, Wind Speed & Gusts, Barometric Pressure, Visibility, UV Index, and Sunrise/Sunset times with a visual sun-path arc.

3. **Full 24-Hour Interactive Hourly Forecast (12 AM – 11 PM)**
   - Scroll horizontally across all 24 hours of the day.
   - Tap any hourly card (or use the `<` / `>` buttons) to inspect detailed hourly temperature, feels-like temperature, condition, rain probability (`%`), liquid precipitation (`mm`), humidity, wind speed, and wind direction.

4. **10-Day Extended Forecast & Day Details**
   - View a 10-day forecast with daily condition icons, rain probability badges, sunrise/sunset times, and temperature range bars.
   - Tap any day in the 10-day list to open its dedicated **Forecast Detail Screen** with that day's full 24-hour breakdown and atmospheric metrics.

5. **Estimated Weather Advisories & Notifications**
   - Automatically evaluates Open-Meteo forecast telemetry for heavy rain, thunderstorms, high winds, snow, and extreme temperatures (clearly labeled as estimated meteorological advisories).
   - Optional Android notifications (Daily Weather Summary with custom compact/expanded layout, Rain Alerts, and Severe Weather Alerts) with configurable background refresh intervals via `WorkManager`.

6. **Android Home Screen Widget**
   - Includes a resizable glassmorphic home screen widget displaying the active city, current temperature, weather condition, high/low temperatures, and the next 4 hours of forecast.

7. **Customizable Settings**
   - Switch between **Dark**, **Light**, and **Automatic (System)** themes.
   - Toggle between **Celsius (`°C`)** and **Fahrenheit (`°F`)**.
   - Adjust **Weather Animation Quality** (`High` / `Low`) or enable **Battery Saver Mode**.

---

## Permissions Used

| Permission | Purpose | Required or Optional |
| :--- | :--- | :--- |
| `INTERNET` | Fetches live weather forecasts and city search results over HTTPS from Open-Meteo. | Required |
| `ACCESS_NETWORK_STATE` | Checks network connectivity to switch cleanly to cached offline weather data when disconnected. | Required |
| `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` | Detects the user's current city coordinates when the user taps **Use My GPS**. | Optional (Manual city search works without GPS) |
| `POST_NOTIFICATIONS` | Posts optional daily weather summaries and rain/severe weather alerts on Android 13+. | Optional |

---

## Data Attribution & License

- Weather and geocoding data provided by [Open-Meteo.com](https://open-meteo.com/) under the [CC BY 4.0 License](https://creativecommons.org/licenses/by/4.0/).
- Privacy Policy: [PRIVACY_POLICY.md](https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md)
- Uptodown Submission & Release Guide: [UPTODOWN_SUBMISSION_PACKAGE.md](https://github.com/Antoneeroy/Weather/blob/main/UPTODOWN_SUBMISSION_PACKAGE.md)
