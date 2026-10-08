# 🌱 TerraLink — IoT Smart Farm Android Application

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-blue?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-orange)](https://developer.android.com/topic/architecture)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**TerraLink** is a modern, reactive Android control center and telemetry monitoring dashboard designed for IoT smart farms, automated greenhouses, and hydroponic installations powered by microcontrollers such as the **ESP32**, **ESP8266**, or **Arduino**.

Built entirely with **Jetpack Compose**, **Kotlin Coroutines**, **Hilt**, and **Clean Architecture**, TerraLink provides real-time bidirectional communication over TCP sockets, automated network device discovery via UDP, autonomous agricultural automation rules, home screen widgets with Jetpack Glance, and an embedded **Buzzer Studio** with audio tone conversion.

---

## 📸 Overview & Features

### 📡 1. Real-Time Bidirectional Telemetry (TCP Sockets)
* Direct low-latency TCP communication with IoT control units (default port `8080`).
* Automatic reconnection engine featuring **exponential backoff** and network recovery.
* Live sensor data streaming and actuator commands with immediate feedback.

### 🔍 2. Zero-Config UDP Auto-Discovery
* Instant discovery of IoT boards on the local WiFi network using UDP broadcast (`255.255.255.255` & Android emulator gateway `10.0.2.2` on port `8888`).
* Handles Android `WifiManager.MulticastLock` to ensure packets are received reliably across all Android OS versions.

### 📊 3. Interactive Sensor Dashboard & Historical Trends
* **Climate & Soil**: Air temperature & humidity (DHT11), soil moisture level with optimal range indicators.
* **Liquid & Rain Management**: Ultrasonic water tank level sensor and steam/rain detector.
* **Environment & Security**: Ambient light (LDR), PIR motion detection, distance sensors, and solar panel voltage metrics.
* **Historical Canvas Charts**: Real-time interactive graphs tracking soil humidity and air temperature over selectable timeframes.

### ⚙️ 4. Autonomous Agricultural Rules Engine
* In-app background logic evaluating real-time farm conditions:
  * **Smart Irrigation**: Automatically engages the water pump (`PUMP:ON`) when soil humidity drops below 30% without rain, and stops when soil reaches 40% or rain is detected.
  * **Thermal Regulation**: Automatically turns on ventilation fans (`FAN:ON`) when temperature exceeds 35°C and shuts off when stabilized below 30°C.
* Toggleable automation mode allowing manual override at any time.

### 🎵 5. Buzzer Studio & MP3-to-Tone Pitch Converter
* Compose and send custom musical sequences to the farm's piezo buzzer.
* Built-in classical melody presets and note scale generator.
* **MP3 to Tone Converter**: Upload custom audio clips; the onboard digital signal processor extracts fundamental frequencies and durations and transmits Arduino-compatible tone sequences over TCP.

### 📱 6. Home Screen Widgets (Jetpack Glance)
* Modern Android AppWidgets powered by **Jetpack Glance & Material 3**:
  * **Water Reservoir Widget**: Real-time tank capacity gauge.
  * **Soil Moisture Widget**: Soil hydration monitor and status.
  * **Multi-Sensor & Quick Actuators Widget**: At-a-glance status with direct toggle shortcuts.

### 🛠️ 7. Live Network Packet Inspector & Debugger
* Integrated diagnostic console (`DebugLogScreen`) logging:
  * Inbound TCP streams (`TCP_RX`)
  * Outbound TCP commands (`TCP_TX`)
  * UDP discovery handshakes (`UDP`)
  * System events & automation rule triggers (`SYSTEM`, `AUTOMATION`)
* Filterable, searchable, and exportable logs directly within the app.

---

## 🏛️ Architecture & Tech Stack

TerraLink is engineered using **Clean Architecture** principles and Android best practices:

```
app/
├── data/
│   ├── debug/         # Real-time packet and event logging
│   ├── local/         # Room Database (TelemetryEntity, TelemetryDao)
│   ├── network/       # TcpSocketManager, UdpDiscoveryManager
│   ├── notification/  # NotificationHelper & status alerts
│   ├── repository/    # FarmRepositoryImpl
│   ├── security/      # EncryptedSharedPreferences (SecurePreferencesHelper)
│   └── worker/        # WorkManager (TelemetrySyncWorker)
├── di/                # Hilt Dependency Injection modules (AppModule)
├── domain/
│   ├── model/         # TelemetryState & domain entities
│   ├── repository/    # FarmRepository contract
│   ├── rule/          # AutomationRuleManager
│   └── usecase/       # ConnectFarmUseCase
├── ui/
│   ├── buzzer/        # BuzzerStudioScreen, Mp3ToToneConverter
│   ├── dashboard/     # DashboardScreen, DashboardViewModel, Canvas Charts
│   ├── debug/         # DebugLogScreen
│   └── theme/         # Material 3 Color palette, Typography, Theme
└── widget/            # Glance AppWidgets (SoilWidget, WaterWidget, SensorWidget)
```

### Libraries & Tools

* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (BOM `2024.12.01`) + [Material 3](https://m3.material.io/)
* **Dependency Injection**: [Hilt](https://dagger.dev/hilt/) `2.51.1`
* **Concurrency**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [StateFlow / SharedFlow](https://developer.android.com/kotlin/flow)
* **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) `2.6.1`
* **Secure Storage**: [AndroidX Security-Crypto](https://developer.android.com/topic/security/data) (MasterKeys & EncryptedSharedPreferences)
* **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) `2.10.0`
* **Widgets**: [Jetpack Glance](https://developer.android.com/jetpack/compose/glance) `1.1.0`
* **Testing**: JUnit 4, MockK, Turbine, Robolectric, Coroutines Test

---

## 🔌 Communication Protocol

TerraLink communicates with microcontrollers using straightforward comma-separated ASCII key-value pairs.

### 1. Telemetry Inbound Stream (IoT Board ➔ Android App)

The IoT board streams telemetry packets over a TCP socket at regular intervals (e.g., every 500ms - 1000ms):

```text
TEMP:24.5,HUM:60.0,SOIL:45.0,RAIN:0,WATER:75.0,LIGHT:650,MOTION:0,DISTANCE:12.5,SOLAR:4.2,BUTTON:0,LED:1,FAN:0,RELAY:0,PUMP:0,BUZZER:0,SERVO:90,LCD:System OK
```

| Key | Description | Type / Range |
| :--- | :--- | :--- |
| `TEMP` / `AIR_TEMP` | Ambient Air Temperature | Float (°C) |
| `HUM` / `AIR_HUM` | Ambient Relative Humidity | Float (%) |
| `SOIL` | Soil Moisture Content | Float (%) |
| `RAIN` / `STEAM` | Rain / Condensation Detected | Boolean (`0` / `1`) |
| `WATER` | Water Tank Level | Float (%) |
| `LIGHT` | Light Intensity (LDR) | Integer (Lux / Raw ADC) |
| `MOTION` / `PIR` | Infrared Motion Detected | Boolean (`0` / `1`) |
| `DISTANCE` | Ultrasonic Distance | Float (cm) |
| `SOLAR` / `SOLAR_VOLT` | Solar Panel Output Voltage | Float (V) |
| `BUTTON` | Physical Button State | Boolean (`0` / `1`) |
| `LED` | Main Illumination LED | Boolean (`0` / `1`) |
| `FAN` / `MOTOR` | Ventilation Fan State | Boolean (`0` / `1`) |
| `RELAY` | Auxiliary Power Relay | Boolean (`0` / `1`) |
| `PUMP` | Submersible Water Pump | Boolean (`0` / `1`) |
| `BUZZER` | Piezo Buzzer State | Boolean (`0` / `1`) |
| `SERVO` | Greenhouse Vent Angle | Integer (`0` - `180`°) |
| `LCD` / `LCD_TEXT` | Display Message | String |

### 2. Commands Outbound Stream (Android App ➔ IoT Board)

TerraLink issues direct plain-text action commands followed by a newline:

```text
LED:ON          # Turn LED on
LED:OFF         # Turn LED off
PUMP:ON         # Activate water pump
PUMP:OFF        # Deactivate water pump
FAN:ON          # Turn ventilation fan on
FAN:OFF         # Turn ventilation fan off
RELAY:ON        # Toggle relay on
SERVO:120       # Rotate servo motor to 120 degrees
BUZZER:TONE:440:500 # Play tone: 440 Hz for 500 ms
LCD:Greenhouse Normal # Update LCD 1602/2004 text
```

### 3. UDP Auto-Discovery Handshake

1. **Android App**: Broadcasts `DISCOVER_TERRALINK_FARM` to port `8888`.
2. **IoT Board**: Responds with `TERRALINK_ACK` (or `TERRALINK_ACK:<board_name>`).
3. **Android App**: Detects the sender's IP address and initiates the TCP connection on port `8080`.

---

## 🛠️ Hardware Interfacing (ESP32 Example)

TerraLink is compatible with any standard microcontroller board. Below is a suggested pin configuration for an **ESP32 DevKit**:

| Component | ESP32 Pin | Protocol / Type |
| :--- | :--- | :--- |
| **DHT11 / DHT22** (Air Temp & Hum) | GPIO 4 | One-Wire Digital |
| **Soil Moisture Sensor** | GPIO 34 (ADC1) | Analog In |
| **Rain / Steam Sensor** | GPIO 35 (ADC1) | Digital / Analog In |
| **Water Level Sensor** | GPIO 32 (ADC1) | Analog In |
| **Photoresistor (LDR)** | GPIO 33 (ADC1) | Analog In |
| **HC-SR04** (Trig / Echo) | GPIO 13 / GPIO 12 | Ultrasonic Pulse |
| **PIR Motion Sensor** | GPIO 14 | Digital In |
| **Water Pump Relay** | GPIO 25 | Digital Out (Active High/Low) |
| **Cooling Fan Motor** | GPIO 26 | Digital / PWM Out |
| **Illumination LED** | GPIO 27 | Digital / PWM Out |
| **Piezo Buzzer** | GPIO 19 | PWM (LEDC Tone) |
| **Servo Motor (SG90)** | GPIO 18 | PWM (50 Hz) |
| **I2C LCD Display (1602/2004)** | GPIO 21 (SDA), GPIO 22 (SCL) | I2C |

---

## 🚀 Building & Running the App

### Requirements
* **Android Studio Ladybug (2024.2+)** or newer.
* **JDK 17** or **JDK 21**.
* **Android SDK**: `minSdk 26` (Android 8.0 Oreo), `targetSdk 35` (Android 15).

### Build from Command Line

```bash
# Clone the repository
git clone git@github.com:sayoridev/TerraLink.git
cd TerraLink

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Install to connected device or emulator
./gradlew installDebug
```

---

## 🧪 Testing

The project includes unit tests for clean architecture use cases and ViewModels using MockK and Turbine:

```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
