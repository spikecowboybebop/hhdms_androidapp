# HHDMSPatientApp — Agent Guide

## Project

Single-module Android app (Jetpack Compose + Material3). Connects to a NestJS backend on the local network for auth and WebRTC voice calls.

## Quick start

```bash
./gradlew assembleDebug          # build
./gradlew installDebug           # build + install on connected device
./gradlew test                   # unit tests (JUnit4)
./gradlew connectedAndroidTest   # instrumented tests (needs device/emulator)
```

## Architecture

- **Entrypoint**: `MainActivity.kt` — manages a two-screen state machine (`AppScreen.AUTH` / `AppScreen.DASHBOARD`)
- **Auth**: `AuthScreen.kt` — login/signup via Retrofit to `auth/login` and `auth/mobile_signup`. Per-field validation, loading state on submit, professional error messages, BD phone prefix (`+880`).
- **Dashboard**: `DashboardScreen.kt` — triggers WebRTC calls via `CallSignalingManager`
- **Network**: `NetworkService.kt` — Retrofit client, `AuthApiService` interface
- **Signaling**: `CallSignalingManager.kt` — Socket.IO + WebRTC for peer-to-peer voice
- **Theme**: `ui/theme/` — custom medical palette (`MedicalTeal`, `DeepCharcoal` etc.), dynamic color disabled

## Critical conventions

- **Server URL is hardcoded** to `http://192.168.0.109:3001/` in both `NetworkService.kt:61` and `CallSignalingManager.kt:14`. Change before testing on a different network.
- **Cleartext HTTP** is allowed (`AndroidManifest.xml:12` `usesCleartextTraffic="true"`) — required for local LAN.
- **Release build has R8/proguard disabled** (`optimization.enable = false`). Enable before shipping.
- **Configuration cache** is on (`gradle.properties:17`). Use `--no-configuration-cache` if stale cache causes issues.
- **Min SDK 24, target 36, Java 11** source compatibility.
- Dynamic color is **off by default** (`Theme.kt:31` `dynamicColor = false`).

## Socket.IO event contract (with `apps/api/src/gateway/call.gateway.ts`)

| Direction | Event | Payload |
|---|---|---|
| Android → Server | `call-center-dial` | `{ patientEmail, sdpOffer }` |
| Server → Android | `call-routing-connected` | `{ sdpAnswer, agentSocketId }` |
| Android ↔ Server | `relay-ice-candidate` | `{ targetSocketId, candidate }` |
| Android → Server | `end-call` | `{ targetSocketId }` |
| Server → Android | `call-ended` | `{ reason }` |

The web counterpart mirrors this: web emits `end-call` with `{ targetSocketId: patientSocketId }` and listens for `call-ended` to clean up. The `activeCalls` map on the server pairs patient ↔ agent socket IDs for routing.

## Dependencies

- Retrofit 2.11.0 + Gson (REST)
- Socket.IO client 2.1.1 (signaling)
- Stream WebRTC Android 1.3.10 (WebRTC)
- Material Icons Extended (extra icons)
- AGP 9.2.1 / Kotlin 2.2.10 / Compose BOM 2026.02.01

## Tests

Only placeholder tests exist (`ExampleUnitTest`, `ExampleInstrumentedTest`).
