# HHDMS Android App — Agent Guide

## Project

Multi-module Android project (Jetpack Compose + Material3). Three modules share a common `:core` library. Each module connects to a NestJS backend on the local network for auth and WebRTC voice calls.

## Modules

| Module | Type | Package | Purpose |
|---|---|---|---|
| `:core` | Android Library | `com.example.hhdmspatientapp` | Shared code: Theme, NetworkService, TokenManager, FCM, storage |
| `:app` | Application | `com.example.hhdmspatientapp` | Patient app (dashboard, caregiver, MBBS doctor, nurse screens) |
| `:nurse` | Application | `com.example.hhdmspatientapp.nurse` | Standalone nurse app (nurse-only auth + nurse screens) |

## Quick start

```bash
./gradlew :app:assembleDebug          # build patient app
./gradlew :nurse:assembleDebug        # build nurse app
./gradlew :core:assembleDebug         # build shared library
./gradlew assembleDebug               # build all modules
```

## Architecture

### Core module (`:core`)
- **Theme**: `ui/theme/` — custom medical palette (`TechTeal`, `ClinicalNavy` etc.), `HhdmsTheme` + legacy `HHDMSPatientAppTheme` alias
- **Network**: `NetworkService.kt` — Retrofit client (`RetrofitClient`), `AuthApiService` interface, all data models
- **Auth**: `TokenManager.kt` — JWT token storage
- **Storage**: `NotificationStorage.kt`, `FcmTokenStorage.kt`, `VisitStorage.kt`
- **FCM**: `HhdmsFirebaseMessagingService.kt` — base FCM service (used by `:app`)

### Patient app (`:app`)
- **Entrypoint**: `MainActivity.kt` — state machine (`AppScreen.AUTH` / `AppScreen.DASHBOARD` + 30+ screens)
- **Auth**: `AuthScreen.kt` — login/signup (`role_name: "MOBILE_USER"`)
- **Dashboard**: `DashboardScreen.kt` — triggers WebRTC calls via `CallSignalingManager`
- **Signaling**: `CallSignalingManager.kt` — Socket.IO + WebRTC
- Nurse screens also live here (duplicated from `:nurse` for backward compat)

### Nurse app (`:nurse`)
- **Entrypoint**: `NurseMainActivity.kt` — state machine (`NurseScreen.AUTH` / `NurseScreen.DASHBOARD` + 10 nurse screens)
- **Auth**: `NurseAuthScreen.kt` — login/signup with `role_name: "NURSE"` hardcoded
- **FCM**: `NurseFirebaseMessagingService.kt` — nurse-specific FCM handling
- **Nurse screens**: `NurseDashboardScreen.kt`, `NurseVitalsScreen.kt`, `NurseMedicationScreen.kt`, etc.

## Critical conventions

- **Server URL is hardcoded** to `http://192.168.0.148:4000` in `NetworkService.kt:1053` (`RetrofitClient.BASE_URL`). Change before testing on a different network.
- **Cleartext HTTP** is allowed (`AndroidManifest.xml` `usesCleartextTraffic="true"`) — required for local LAN.
- **Release build has R8/proguard disabled** (`optimization.enable = false`). Enable before shipping.
- **Configuration cache** is on (`gradle.properties`). Use `--no-configuration-cache` if stale cache causes issues.
- **Min SDK 24, target 36, Java 11** source compatibility.
- Dynamic color is **off by default** (`Theme.kt` `dynamicColor = false`).
- **Smart cast warning**: Data classes in `:core` can't be smart-cast from `:app`/`:nurse`. Use local `val` assignments before null checks.

## Socket.IO event contract (with `apps/api/src/gateway/call.gateway.ts`)

| Direction | Event | Payload |
|---|---|---|
| Android → Server | `call-center-dial` | `{ patientEmail, sdpOffer }` |
| Server → Android | `call-routing-connected` | `{ sdpAnswer, agentSocketId }` |
| Android ↔ Server | `relay-ice-candidate` | `{ targetSocketId, candidate }` |
| Android → Server | `end-call` | `{ targetSocketId }` |
| Server → Android | `call-ended` | `{ reason }` |

## Dependencies

- Retrofit 2.11.0 + Gson (REST)
- Socket.IO client 2.1.1 (signaling — `:app` only)
- Stream WebRTC Android 1.3.10 (WebRTC — `:app` only)
- Material Icons Extended (extra icons)
- AGP 9.2.1 / Kotlin 2.2.10 / Compose BOM 2026.02.01

## Tests

Only placeholder tests exist (`ExampleUnitTest`, `ExampleInstrumentedTest`).
