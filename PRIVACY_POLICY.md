# Privacy Policy for Weather (`com.aistudio.weather.glsmph`)

**Effective Date:** November 13, 2026  
**Application Name:** Weather  
**Package Name:** `com.aistudio.weather.glsmph`  
**Developer:** Antonee  
**Official Repository:** [https://github.com/Antoneeroy/Weather](https://github.com/Antoneeroy/Weather)

---

## 1. Overview

**Weather** (`com.aistudio.weather.glsmph`) is a consumer Android weather application developed by **Antonee**. We respect your privacy and are committed to complete transparency regarding how data is handled when you use the Weather application.

This application does **not** require user accounts, does **not** display advertisements, does **not** include third-party tracking or analytics SDKs, and does **not** sell or share personal information with third parties.

---

## 2. Information Accessed and How It Is Used

### A. Device Location (`ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION` – Optional)
- **Why it is requested:** Location access is requested **only when you explicitly tap "Use My GPS" or "Allow Location"** so the app can determine your local coordinates and retrieve live local weather forecasts.
- **How it is processed:** Your latitude and longitude coordinates are sent over an encrypted HTTPS connection directly to the public Open-Meteo Weather Forecast API (`https://api.open-meteo.com`) to fetch meteorological data for your area, and to the Android system `Geocoder` on-device to display your city/locality name.
- **No background tracking or server storage:** Your location is never tracked in the background for advertising or profiling, never stored on developer-owned servers, and never shared with data brokers.
- **Manual use without GPS:** You can decline location permission and use all forecast features of the application by searching for any city worldwide.

### B. Worldwide City Search Queries
- When you search for a city name in the **Cities & Search** screen, the search term is sent over HTTPS to the Open-Meteo Geocoding API (`https://geocoding-api.open-meteo.com`) solely to return matching geographic coordinates and city names.

### C. Local Device Storage (Preferences & Offline Weather Cache)
- Your selected temperature unit (`°C` / `°F`), theme preference (Dark, Light, or Automatic), animation quality settings, saved cities list, and most recent weather snapshot are stored **exclusively locally on your device** using Android Jetpack DataStore and SharedPreferences (for the home screen widget).
- Android automatic cloud backup (`android:allowBackup="false"`) is disabled so your locally saved locations are not uploaded to external cloud backups without your knowledge.
- You can clear all saved locations at any time using the **Clear All** button in the **Cities & Search** screen or by clearing the app's storage in Android Settings.

### D. Notifications (`POST_NOTIFICATIONS` – Optional on Android 13+)
- If you enable weather notifications in the **Settings** screen, the app requests the Android `POST_NOTIFICATIONS` permission to display local Daily Weather Summaries, Rain Alerts, and Severe Weather Advisories on your device.

---

## 3. Data Security

- **Encrypted Network Communication:** All network requests to Open-Meteo endpoints use HTTPS encryption (`android:usesCleartextTraffic="false"` is enforced in the application manifest).
- **No Production Coordinate Logging:** Network request URLs containing coordinates are never logged in release builds.
- **No API Keys or User Credentials:** The app does not collect names, email addresses, phone numbers, device identifiers, or payment data.

---

## 4. Third-Party Meteorological Service

Weather forecasts and geocoding lookups are powered by **Open-Meteo** ([https://open-meteo.com](https://open-meteo.com)) under the Creative Commons Attribution 4.0 International (CC BY 4.0) license. Open-Meteo processes standard HTTPS forecast requests to return meteorological telemetry. You can review Open-Meteo's terms and privacy practices at [https://open-meteo.com/en/terms](https://open-meteo.com/en/terms).

---

## 5. Third-Party Libraries & SDK Disclosures

This application includes the following open-source and platform libraries strictly for core app functionality (no advertising, behavioral analytics, or profiling SDKs are included):

- **Google Play Services Location (`com.google.android.gms:play-services-location`)**: Used on-device via `FusedLocationProviderClient` to retrieve your device coordinates **only** when you explicitly grant location permission and tap **Use My GPS**.
- **Square Retrofit, OkHttp & Moshi (`com.squareup.retrofit2`, `com.squareup.okhttp3`, `com.squareup.moshi`)**: Used to perform HTTPS requests to the Open-Meteo Weather and Geocoding APIs and parse JSON weather responses.
- **AndroidX Jetpack Libraries (`Compose`, `DataStore Preferences`, `WorkManager`, `Core-KTX`, `Lifecycle`)**: Official Android Jetpack libraries used for rendering the user interface, storing your settings and cached weather snapshots locally on-device, and scheduling periodic background forecast updates.

---

## 6. Children's Privacy

The Weather application does not collect personal information from anyone, including children under the age of 13.

---

## 7. Changes to This Privacy Policy

Any updates to this Privacy Policy will be published in the official GitHub repository at:  
[https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md](https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md)

---

## 7. Contact Information

If you have questions about this Privacy Policy or the Weather Android application, please open an issue or contact the developer (**Antonee**) via the official project repository:  
[https://github.com/Antoneeroy/Weather](https://github.com/Antoneeroy/Weather)
