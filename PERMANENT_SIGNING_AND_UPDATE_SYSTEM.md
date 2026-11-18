# Permanent Production Signing & In-Place Update System
*(For All Android App Projects by Antonee)*

This document defines the **Permanent Production Signing & In-Place Update System** for both Android repositories under `https://github.com/Antoneeroy`:

1. **Weather** (`https://github.com/Antoneeroy/Weather`) — `applicationId = "com.aistudio.weather.glsmph"`
2. **Aroy Player** (`https://github.com/Antoneeroy/Aroy-player-v1`) — `applicationId = "com.aistudio.aroyplayer.vxkmp"`

---

## 1. Master Configuration Summary Across All Android Apps

| Requirement | App 1: **Weather** (`Antoneeroy/Weather`) | App 2: **Aroy Player** (`Antoneeroy/Aroy-player-v1`) |
| :--- | :--- | :--- |
| **Package Name (`applicationId`)** | `com.aistudio.weather.glsmph` *(Permanent)* | `com.aistudio.aroyplayer.vxkmp` *(Permanent)* |
| **Release Version Code** | `8` *(Incremented from `7`)* | `222` *(Incremented from `221`)* |
| **Release Version Name** | `5.3.0` | `2.2.2` |
| **Minimum SDK (`minSdk`)** | `26` (Android 8.0 Oreo+) | `26` (Android 8.0 Oreo+) |
| **Target / Compile SDK** | `36` | `36` |
| **Dedicated Keystore File Name (Offline Backup)** | `weather-release-key.jks` | `aroy-player-release-key.jks` |
| **Keystore Key Alias** | `upload` | `upload` |
| **GitHub Actions Secrets** | `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` | `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` |
| **Temporary Keystore Generation (`keytool -genkeypair`)** | **100% Disabled** (Fails fast if secrets are missing) | **100% Disabled** (Fails fast if secrets are missing) |
| **Release Build Task** | `gradle assembleRelease` | `gradle assembleRelease` |
| **Actions Artifact Name** | `app-release-apk` (`app-release.apk`) | `aroy-player-release-apk` (`app-release.apk`) |
| **In-Place Data & Settings Storage** | Jetpack DataStore (`weather_phase5_preferences`) | Room DB (`aroy_player.db`, `version = 5`) + Preferences |

---

## 2. How to Create Separate Permanent Production Keystores (One-Time Only)

Run these commands **once** on your private computer (never inside GitHub Actions and never commit the `.jks` files to Git):

### A. For App 1 — **Weather** (`com.aistudio.weather.glsmph`)
```bash
# 1. Generate permanent Weather production keystore (valid for 10,000 days)
keytool -genkeypair -v \
  -keystore weather-release-key.jks \
  -storetype PKCS12 \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Antonee, OU=Mobile, O=Antonee Weather, L=City, S=State, C=IN"

# 2. Convert keystore to single-line Base64 for GitHub Secret KEYSTORE_BASE64
base64 -w 0 weather-release-key.jks > weather-keystore-base64.txt
```
Add these 3 Repository Secrets in **`https://github.com/Antoneeroy/Weather/settings/secrets/actions`**:
- `KEYSTORE_BASE64` — contents of `weather-keystore-base64.txt`
- `STORE_PASSWORD` — the password you entered when creating `weather-release-key.jks`
- `KEY_PASSWORD` — the key password (same as `STORE_PASSWORD` for PKCS12)

---

### B. For App 2 — **Aroy Player** (`com.aistudio.aroyplayer.vxkmp`)
```bash
# 1. Generate separate permanent Aroy Player production keystore (valid for 10,000 days)
keytool -genkeypair -v \
  -keystore aroy-player-release-key.jks \
  -storetype PKCS12 \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Antonee, OU=Mobile, O=Aroy Player, L=City, S=State, C=IN"

# 2. Convert keystore to single-line Base64 for GitHub Secret KEYSTORE_BASE64
base64 -w 0 aroy-player-release-key.jks > aroy-player-keystore-base64.txt
```
Add these 3 Repository Secrets in **`https://github.com/Antoneeroy/Aroy-player-v1/settings/secrets/actions`**:
- `KEYSTORE_BASE64` — contents of `aroy-player-keystore-base64.txt`
- `STORE_PASSWORD` — the password you entered when creating `aroy-player-release-key.jks`
- `KEY_PASSWORD` — the key password (same as `STORE_PASSWORD` for PKCS12)

> **CRITICAL BACKUP RULE:** Save both `weather-release-key.jks` and `aroy-player-release-key.jks` in a secure offline backup (e.g., encrypted USB drive or password manager) and delete `*-keystore-base64.txt` after pasting into GitHub Secrets.

---

## 3. Ready-to-Apply Files for App 2 (`Antoneeroy/Aroy-player-v1`)

Apply the following 4 updates in your `Antoneeroy/Aroy-player-v1` repository to bring it to the exact same permanent production signing standard as `Weather`:

### 3.1 `.github/workflows/build.yml` (for `Antoneeroy/Aroy-player-v1`)
```yaml
name: Build Signed Release APK

on:
  workflow_dispatch:
  push:
    branches: [main]

jobs:
  build-release:
    name: Build Signed Release APK (API 26+)
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Setup Gradle 9.3.1
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '9.3.1'

      - name: Validate and Restore Persistent Release Keystore from GitHub Secrets
        env:
          SECRET_KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
          SECRET_STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          SECRET_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          if [ -z "$SECRET_KEYSTORE_BASE64" ] || [ -z "$SECRET_STORE_PASSWORD" ] || [ -z "$SECRET_KEY_PASSWORD" ]; then
            echo "::error::Missing required release signing secrets (KEYSTORE_BASE64, STORE_PASSWORD, or KEY_PASSWORD)."
            echo "Refusing to generate a temporary keystore so the permanent production signing identity is never altered."
            echo "Please configure KEYSTORE_BASE64, STORE_PASSWORD, and KEY_PASSWORD in GitHub -> Settings -> Secrets and variables -> Actions."
            exit 1
          fi

          KEYSTORE_FILE="${RUNNER_TEMP}/my-upload-key.jks"
          echo "$SECRET_KEYSTORE_BASE64" | tr -d ' \t\r\n' | base64 --decode > "$KEYSTORE_FILE"

          echo "Verifying persistent release keystore (alias: upload)..."
          keytool -list -v \
            -keystore "$KEYSTORE_FILE" \
            -storepass "$SECRET_STORE_PASSWORD" \
            -alias upload | grep -E "(Alias name:|Owner:|Valid from:|SHA1:|SHA256:)"

          echo "KEYSTORE_PATH=${KEYSTORE_FILE}" >> "$GITHUB_ENV"
          echo "STORE_PASSWORD=${SECRET_STORE_PASSWORD}" >> "$GITHUB_ENV"
          echo "KEY_PASSWORD=${SECRET_KEY_PASSWORD}" >> "$GITHUB_ENV"

      - name: Build Signed Release APK (assembleRelease)
        run: gradle assembleRelease --no-daemon --stacktrace

      - name: Verify APK Signature, Package Name, versionCode & minSdkVersion
        run: |
          APK_PATH="app/build/outputs/apk/release/app-release.apk"
          ls -lh "$APK_PATH"

          echo "=== 1. Verifying APK Signature (SHA-256 Certificate) ==="
          APKSIGNER=$(find "${ANDROID_HOME:-/usr/local/lib/android/sdk}" -name apksigner 2>/dev/null | sort -V | tail -n 1)
          if [ -n "$APKSIGNER" ]; then
            "$APKSIGNER" verify --verbose --print-certs "$APK_PATH"
          else
            jarsigner -verify -verbose -certs "$APK_PATH" | head -n 30
          fi

          echo "=== 2. Verifying Package Name, versionCode, versionName & minSdkVersion ==="
          AAPT2=$(find "${ANDROID_HOME:-/usr/local/lib/android/sdk}" -name aapt2 2>/dev/null | sort -V | tail -n 1)
          if [ -n "$AAPT2" ]; then
            BADGING=$("$AAPT2" dump badging "$APK_PATH")
            echo "$BADGING" | grep -E "^(package:|sdkVersion:|targetSdkVersion:)"
            echo "$BADGING" | grep -q "package: name='com.aistudio.aroyplayer.vxkmp'" || { echo "::error::Package name mismatch!"; exit 1; }
            echo "$BADGING" | grep -q "sdkVersion:'26'" || { echo "::error::minSdkVersion must be 26 (Android 8.0+)!"; exit 1; }
          fi

      - name: Upload Signed Release APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: aroy-player-release-apk
          path: app/build/outputs/apk/release/app-release.apk
          if-no-files-found: error
          retention-days: 90

      - name: Clean Up Temporary Keystore File
        if: always()
        run: rm -f "${RUNNER_TEMP}/my-upload-key.jks"
```

### 3.2 `app/build.gradle.kts` (`defaultConfig` block in `Antoneeroy/Aroy-player-v1`)
```kotlin
  defaultConfig {
    applicationId = "com.aistudio.aroyplayer.vxkmp"
    minSdk = 26
    targetSdk = 36
    versionCode = 222
    versionName = "2.2.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
```

### 3.3 `.gitignore` (in `Antoneeroy/Aroy-player-v1`)
```gitignore
*.iml
.gradle
.kotlin/
local.properties
/.idea/caches
/.idea/libraries
/.idea/modules.xml
/.idea/workspace.xml
/.idea/navEditor.xml
/.idea/assetWizardSettings.xml
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
.env
.env.*
!.env.example
debug.keystore
*.jks
*.keystore
*.p12
*.pfx
keystore.properties
signing.properties
```

### 3.4 Data Preservation in `app/src/main/java/com/example/data/local/AroyDatabase.kt` (`Antoneeroy/Aroy-player-v1`)
Remove `.fallbackToDestructiveMigration()` (or replace with `.fallbackToDestructiveMigrationOnDowngrade()`) and keep `version = 5` unchanged unless providing explicit `Migration(5, 6)` objects, so user playlists, playback history, saved streams, and recycle bin items in `aroy_player.db` are never wiped during an in-place app upgrade.

---

## 4. In-Place Update Verification Checklist (For Every Future Release)

For every new release of **Weather** or **Aroy Player**:
1. **Increment `versionCode` by `+1`** (`Weather`: `8 -> 9 -> 10`; `Aroy Player`: `222 -> 223 -> 224`) and update `versionName`.
2. **Never change `applicationId`** (`com.aistudio.weather.glsmph` / `com.aistudio.aroyplayer.vxkmp`).
3. **Never change `KEYSTORE_BASE64`** in GitHub Actions Secrets.
4. **Verify In-Place Upgrade Compatibility (`apksigner` & `adb install -r`):**
   ```bash
   # Compare SHA-256 certificate digest of previous APK and new APK (must be 100% identical):
   apksigner verify --print-certs previous-app-release.apk | grep "SHA-256"
   apksigner verify --print-certs new-app-release.apk | grep "SHA-256"

   # Test in-place installation over existing app without uninstalling:
   adb install -r new-app-release.apk
   ```
