# HHDMSPatientApp — Agent Guide

## Project

Single-module Android app (Jetpack Compose + Material3). Supports three roles: **patient**, **MBBS doctor**, and **caregiver**. Connects to a NestJS backend on the local network for auth, data, payments, WebRTC voice calls, and teleconsult video calls.

## Quick start

```bash
./gradlew assembleDebug          # build
./gradlew installDebug           # build + install on connected device
./gradlew test                   # unit tests (JUnit4)
./gradlew connectedAndroidTest   # instrumented tests (needs device/emulator)
```

## Architecture

- **Entrypoint**: `MainActivity.kt` — manages a 22-screen state machine via `AppScreen` enum
- **Three roles**: `MOBILE_USER` (patient), `MBBS_DOCTOR`, `CAREGIVER` — each has dedicated dashboard and screen flows
- **Auth**: `AuthScreen.kt` — login/signup via Retrofit to `auth/login` and `auth/mobile_signup`. Per-field validation, loading state on submit, BD phone prefix (`+880`).
- **Patient screens**: Dashboard, appointments, booking detail, notifications, doctor tracking (real-time GPS via osmdroid), teleconsult incoming call modal, video room (WebView-based)
- **Doctor screens**: MBBS dashboard, booking detail, patient assignments, patient detail, vitals/diagnosis/prescription/test orders/referral entry
- **Caregiver screens**: Dashboard, patient list, patient detail, activity log, condition report, GPS check-in/check-out
- **Network**: `NetworkService.kt` — Retrofit client (~45 endpoints), `AuthApiService` interface, 60+ data models
- **Signaling**: `CallSignalingManager.kt` — Socket.IO + WebRTC for peer-to-peer voice
- **Teleconsult**: Socket.IO events for video call lifecycle (`register:user`, `call:ringing`, `call:ready`, `call:ended`, `call:accept`, `call:decline`, `call:terminate`)
- **Storage**: `TokenManager`, `NotificationStorage`, `FcmTokenStorage`, `VisitStorage` — all SharedPreferences-based
- **Firebase**: `HhdmsFirebaseMessagingService.kt` for push notifications
- **Payments**: Stripe PaymentSheet integration
- **Theme**: `ui/theme/` — palette uses `TechTeal (#00D4B2)` and `ClinicalNavy (#0A2540)` as canonical names (legacy aliases `MedicalTeal`/`DeepCharcoal` still exist in `Color.kt`). Dynamic color disabled.

## Critical conventions

- **Server URL is hardcoded** to `http://192.168.0.101:3001` in both `NetworkService.kt:708` and `CallSignalingManager.kt:14`. Change before testing on a different network.
- **Cleartext HTTP** is allowed (`AndroidManifest.xml:19` `usesCleartextTraffic="true"`) — required for local LAN.
- **Release build has R8/proguard disabled** (`optimization.enable = false`). Enable before shipping.
- **Configuration cache** is on (`gradle.properties:17`). Use `--no-configuration-cache` if stale cache causes issues.
- **Min SDK 24, target 36, Java 11** source compatibility.

## Socket.IO event contracts

### WebRTC voice calls (with `apps/api/src/gateway/call.gateway.ts`)

| Direction | Event | Payload |
|---|---|---|
| Android → Server | `call-center-dial` | `{ patientEmail, sdpOffer }` |
| Server → Android | `call-routing-connected` | `{ sdpAnswer, agentSocketId }` |
| Android ↔ Server | `relay-ice-candidate` | `{ targetSocketId, candidate }` |
| Android → Server | `end-call` | `{ targetSocketId }` |
| Server → Android | `call-ended` | `{ reason }` |

### Teleconsult video calls (WebView/Daily.co-based)

| Direction | Event | Payload |
|---|---|---|
| Android → Server | `register:user` | user registration |
| Server → Android | `call:ringing` | incoming call info |
| Server → Android | `call:ready` | room ready |
| Server → Android | `call:ended` | termination info |
| Android → Server | `call:accept` | accept call |
| Android → Server | `call:decline` | decline call |
| Android → Server | `call:terminate` | end call |

## Dependencies

- Retrofit 2.11.0 + Gson (REST)
- OkHttp 4.12.0 (HTTP)
- Socket.IO client 2.1.1 (signaling)
- Stream WebRTC Android 1.3.10 (WebRTC)
- Firebase Messaging KTX 24.1.1 (push notifications)
- Stripe Android 20.48.1 (payments)
- osmdroid 6.1.18 (maps / doctor tracking)
- Play Services Location 21.4.0 (GPS)
- Material Icons Extended (extra icons)
- AGP 9.2.1 / Kotlin 2.2.10 / Compose BOM 2026.02.01

## Tests

Only placeholder tests exist (`ExampleUnitTest`, `ExampleInstrumentedTest`).
