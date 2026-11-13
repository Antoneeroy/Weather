# Privacy Policy for Weather (`com.aistudio.weather.glsmph`)

- **Effective Date:** November 13, 2026
- **Last Updated:** November 13, 2026
- **Application Name:** Weather
- **Package Name:** `com.aistudio.weather.glsmph`
- **Version:** `5.1.0` (`versionCode = 6`)
- **Developer:** Antonee
- **Official Repository:** [https://github.com/Antoneeroy/Weather](https://github.com/Antoneeroy/Weather)
- **GitHub Pages Privacy Policy URL:** [https://antoneeroy.github.io/Weather/privacy-policy.html](https://antoneeroy.github.io/Weather/privacy-policy.html)
- **Direct Public Repository Policy URL:** [https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md](https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md)

---

## 1. Introduction & Actual Data Practices Summary

**Weather** (`com.aistudio.weather.glsmph`) is an Android weather forecast application developed by **Antonee**. This Privacy Policy describes how the application handles permissions, location coordinates, city search queries, local device storage, notifications, and third-party services based strictly on the application's actual source code.

- **No Developer Backend Server:** The developer (**Antonee**) does not operate any proprietary backend server or database. The app communicates directly from your Android device to the public Open-Meteo meteorological APIs over HTTPS.
- **No Personal Accounts or Identity Collection:** The app does not ask for or collect your name, email address, phone number, contacts, photos, or payment information.
- **No Advertising or Analytics SDKs:** The app contains zero advertisements, zero behavioral tracking SDKs, and zero third-party analytics or crash-reporting trackers.

---

## 2. Android Permissions Used by the App

The application declares only the following five permissions in `AndroidManifest.xml`:

| Android Permission | Type | Exact Purpose in the App |
| :--- | :--- | :--- |
| `android.permission.INTERNET` | Install-time | Required to fetch live weather forecasts (`https://api.open-meteo.com/v1/forecast`) and city search matches (`https://geocoding-api.open-meteo.com/v1/search`) over encrypted HTTPS. |
| `android.permission.ACCESS_NETWORK_STATE` | Install-time | Used via Android's `ConnectivityManager` to detect whether an internet connection is available and switch cleanly to locally cached weather data when offline. |
| `android.permission.ACCESS_COARSE_LOCATION` | Runtime (Optional) | Requested **only** when you tap **Use My GPS** and confirm the in-app **Local Weather Access** disclosure dialog. Used to obtain approximate device coordinates for local weather lookup. |
| `android.permission.ACCESS_FINE_LOCATION` | Runtime (Optional) | Requested alongside coarse location when you tap **Use My GPS** to obtain accurate foreground GPS coordinates for local weather lookup. |
| `android.permission.POST_NOTIFICATIONS` | Runtime on Android 13+ (Optional) | Requested **only** when you enable weather notifications or tap **Send Test Weather Notification** in Settings on Android 13 (API 33) and newer. |

> **No Background Location Permission:** The app does **not** declare or request `ACCESS_BACKGROUND_LOCATION`.

---

## 3. Location Usage & Manual City Selection

1. **On-Demand Foreground Access Only:**
   - The app never requests location permission automatically on first launch. Instead, when you tap **Use My GPS**, an in-app disclosure dialog (**Local Weather Access**) explains why location is requested and gives you the choice between **Allow Location** and **Search City Instead**.
2. **How Coordinates Are Processed:**
   - When permission is granted, `LocationManager` uses Google Play Services `FusedLocationProviderClient` (with fallback to Android's system `LocationManager`) to read your current latitude and longitude in the foreground.
   - Those coordinates are passed to Android's built-in `android.location.Geocoder` to display your locality/city name on-device, and are sent over HTTPS to the Open-Meteo Forecast API (`https://api.open-meteo.com/v1/forecast`) to retrieve your local weather forecast.
3. **Full Functionality Without Location Permission:**
   - Granting location permission is completely optional. If you deny or skip location permission, you can use the **Cities & Search** tab to search and save any city worldwide manually.

---

## 4. Weather API & Network Communication

- **Open-Meteo Weather Forecast API (`https://api.open-meteo.com/v1/forecast`):**
  - Receives the latitude and longitude of your selected city (or GPS location) over HTTPS to return current temperature, humidity, apparent temperature, WMO weather code, barometric pressure, wind speed/direction/gusts, visibility, UV index, 24-hour hourly forecast, and 10-day daily forecast.
- **Open-Meteo Geocoding API (`https://geocoding-api.open-meteo.com/v1/search`):**
  - Receives the city name text you type into the search bar (trimmed and sanitized, up to 80 characters) over HTTPS to return matching city names, regions/countries, and coordinates.
- **Standard HTTPS Metadata:**
  - As with any direct internet connection, Open-Meteo's servers receive standard HTTP request headers (including your device's IP address) necessary to route the response back to your device.
- **Network Security:**
  - Cleartext HTTP traffic is disabled across the entire application (`android:usesCleartextTraffic="false"`).
  - Network request logging (`HttpLoggingInterceptor`) is restricted strictly to debug development builds (`if (BuildConfig.DEBUG)`) and is completely disabled in release builds.

---

## 5. Local On-Device Data Storage & Cloud Backup Policy

All user preferences and saved locations are stored **locally on your device** in the app's private sandbox:

1. **Jetpack DataStore (`weather_phase5_preferences`):**
   - Stores your appearance setting (Dark, Light, Automatic), temperature unit (`°C` or `°F`), weather animation toggles and quality tier (`High` / `Low`), battery saver setting, notification preferences, refresh interval, saved city records (city ID, name, region/country, latitude, longitude, favorite status), and the most recent cached weather JSON snapshot for offline viewing.
2. **SharedPreferences (`weather_widget_shared_state`):**
   - Stores the latest formatted city name, temperature, condition text, high/low values, and 4-hour forecast labels displayed by the Android Home Screen Widget (`WeatherWidgetReceiver`).
3. **No Automatic Cloud Backup:**
   - `android:allowBackup="false"` is enforced in `AndroidManifest.xml`, and `backup_rules.xml` / `data_extraction_rules.xml` explicitly exclude the app's DataStore and widget SharedPreferences files from cloud backups and device-transfer extraction.
4. **How to Delete Your Stored Data:**
   - You can remove individual saved cities using the delete icon on any city card, erase all saved locations at once using the **Clear All** button in the **Cities & Search** screen, or clear all app data / uninstall the app via Android System Settings.

---

## 6. Notifications, Background Refresh & Home Screen Widget

- **Notification Channels:**
  - `weather_daily_summary_channel` (*Weather Forecast & Notification Widget*): Used for optional Daily Weather Summary and Morning Briefing notifications, including a compact and expanded 4-hour forecast layout.
  - `weather_severe_alerts_channel` (*Rain & Severe Weather Advisories*): Used for optional Rain Alerts and estimated Severe Weather Advisories derived from Open-Meteo forecast metrics.
- **Background Sync via WorkManager:**
  - When periodic refresh (Every 1 Hour, 3 Hours, 6 Hours, or 12 Hours) or notifications are enabled in Settings, `WeatherBackgroundSyncWorker` runs periodically via Android Jetpack `WorkManager` (requiring an active network connection).
  - The background worker refreshes weather using the **stored coordinates** of your active/default saved city in DataStore — it does **not** access live device GPS in the background.
  - Selecting **Manual Only** refresh and turning off **Enable Notifications** cancels all periodic background work.

---

## 7. Third-Party SDKs & Libraries Disclosed

The application uses only the following third-party services and open-source libraries:

1. **Open-Meteo APIs** ([https://open-meteo.com](https://open-meteo.com))
   - **Role:** Provides meteorological forecast data and city geocoding search over HTTPS.
   - **License & Terms:** Data licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/); terms and privacy policy available at [https://open-meteo.com/en/terms](https://open-meteo.com/en/terms).
2. **Google Play Services Location (`com.google.android.gms:play-services-location`)**
   - **Role:** Provides `FusedLocationProviderClient` for on-device foreground location lookup when the user grants optional location permission.
   - **Privacy Policy:** [https://policies.google.com/privacy](https://policies.google.com/privacy)
3. **Square Open-Source Client Libraries (`Retrofit`, `OkHttp`, `Moshi`)**
   - **Role:** Open-source (Apache 2.0) HTTP client and JSON parser running locally on your device to communicate with Open-Meteo over HTTPS.
4. **AndroidX Jetpack Libraries (`Compose`, `DataStore`, `WorkManager`, `Lifecycle`, `Activity`, `Core-KTX`)**
   - **Role:** Official open-source (Apache 2.0) Android Jetpack libraries used for UI rendering, local preference storage, and background work scheduling.

---

## 8. Children's Privacy

The Weather application is a general-audience utility app and does not knowingly collect personal information from children or any other users.

---

## 9. Changes to This Privacy Policy

Any updates to this Privacy Policy will be posted to the official repository and GitHub Pages site:
- **GitHub Pages:** [https://antoneeroy.github.io/Weather/privacy-policy.html](https://antoneeroy.github.io/Weather/privacy-policy.html)
- **GitHub Repository:** [https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md](https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md)

---

## 10. Contact the Developer

If you have any questions or concerns regarding this Privacy Policy or the Weather app, please contact the developer (**Antonee**) by opening an issue on the official GitHub repository:  
[https://github.com/Antoneeroy/Weather/issues](https://github.com/Antoneeroy/Weather/issues)
