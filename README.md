<h1 align="center">PulseGate</h1>

<p align="center">
  <strong>Production-Grade Android SMS &amp; Notification Gateway</strong><br/>
  Capture. Queue. Deliver. Reliably.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Min%20SDK-26-orange" alt="Min SDK 26"/>
  <img src="https://img.shields.io/badge/Compile%20SDK-36-blue" alt="Compile SDK 36"/>
  <img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-blueviolet" alt="Architecture"/>
  <img src="https://img.shields.io/badge/PRs-welcome-brightgreen" alt="PRs Welcome"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License MIT"/>
</p>

---

> **"Never lose an event."**  
> Every SMS and notification is persisted to local Room DB before any delivery is attempted. PulseGate is not a simple SMS forwarder — it is a lightweight, self-hosted event-processing pipeline running on your Android device.

---

## Table of Contents

- [Overview](#overview)
- [Why PulseGate](#why-pulsegate)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [How It Works](#how-it-works)
- [Database Schema](#database-schema)
- [OEM Battery Optimization](#oem-battery-optimization)
- [Extending PulseGate](#extending-pulsegate)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**PulseGate** captures incoming **SMS messages** and **banking/payment notifications** and forwards them to your configured destinations — Webhooks or Telegram bots — reliably, even when offline or after a device reboot.

- SMS or notification arrives → persisted to Room DB instantly, before anything else
- Worker picks it up and delivers with `Semaphore(3)` concurrency
- Delivery fails → exponential retry, up to 6 attempts
- No internet → queue held locally, flushed automatically when reconnected
- Device reboots → `BootReceiver` restores service and resumes the queue from where it stopped

---

## Why PulseGate

Most SMS-forwarding solutions on Android are either closed-source, fragile when the app is killed, or locked to a single destination. PulseGate is built to not have those problems:

| Problem | How PulseGate solves it |
|---|---|
| Messages dropped when offline | Local persistent queue; `ConnectivityObserver` flushes on reconnect |
| App killed by OEM battery saver | `GatewayForegroundService` + `BootReceiver` restores full state |
| Duplicate events processed twice | SHA-256 hash deduplication with DB unique constraint |
| Only one destination supported | Fan-out — one event queued per active destination, delivered in parallel |
| No visibility into delivery failures | Per-attempt `DeliveryLog` with HTTP code, latency, retry count |
| Hard to extend with new channels | `Sender` interface — add Slack, Discord, Firebase without touching core logic |

---

## Architecture

Clean Architecture with an MVVM presentation layer. No unnecessary abstractions — layers are added only where the complexity actually justifies them.

```
┌─────────────────────────────────────────────────────────┐
│           PRESENTATION  (Compose + ViewModels)           │
└──────────────────────────┬──────────────────────────────┘
                           │  invokes
┌──────────────────────────▼──────────────────────────────┐
│            DOMAIN  (Use Cases + Repo Interfaces)         │
└──────────────────────────┬──────────────────────────────┘
                           │  implemented by
┌──────────────────────────▼──────────────────────────────┐
│         DATA  (Room + WorkManager + OkHttp + Hilt)       │
└─────────────────────────────────────────────────────────┘
```

**Core design decisions:**

- **DB-First** — `SmsReceiver` and `GatewayNotificationListener` never touch the network. They write to Room and call `WorkScheduler`. That is all.
- **Queue-Driven Delivery** — all delivery goes through the persistent `delivery_queue` table, never a direct API call from a receiver
- **Atomic Transactions** — inserting an event and creating its queue rows is a single Room transaction. Either both succeed or both roll back.
- **Stale Lock Recovery** — workers lock rows before processing. If the process is killed mid-flight, `BootReceiver` releases locks older than 5 minutes so items don't get stuck in `PROCESSING` forever.
- **Pluggable Senders** — `Sender` is a pure interface. `SenderEngine` routes by `DestinationType`. Adding a new transport is isolated entirely to `SenderModule` and `SenderEngine`.

---

## Tech Stack

| Category | Library | Version |
|---|---|---|
| Language | Kotlin | 2.3.21 |
| UI | Jetpack Compose + Material 3 | BOM 2026.04.01 |
| Database | Room | 2.8.4 |
| Background Jobs | WorkManager | 2.11.2 |
| Networking | OkHttp | 5.3.2 |
| REST Adapter | Retrofit | 3.0.0 |
| Dependency Injection | Hilt | 2.59.2 |
| Async | Coroutines + StateFlow | 1.10.2 |
| Secure Storage | EncryptedSharedPreferences | 1.1.0 |
| Serialization | Moshi + KSP Codegen | 1.15.2 |
| Navigation | Navigation Compose | 2.9.8 |
| Logging | Timber | 5.0.1 |
| Build | Gradle Kotlin DSL + KSP | AGP 9.2.0 / KSP 2.3.6 |

---

## Project Structure

```
com.aman.pulsegate/
├── PulseGateApp.kt                      # Application — Hilt + WorkManager init
├── MainActivity.kt                      # Single activity, nav host
│
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt               # Room v1, schema export enabled
│   │   ├── entity/                      # IncomingEventEntity, DeliveryQueueEntity,
│   │   │                                # DestinationEntity, DeliveryLogEntity
│   │   ├── dao/                         # 4 DAOs — includes atomic queue lock/unlock SQL
│   │   └── repository/                  # Repository implementations
│   └── security/
│       └── SecurePreferences.kt         # EncryptedSharedPreferences wrapper (AES-256)
│
├── domain/
│   ├── model/                           # Domain models, QueueStatus, SourceType, SendResult
│   ├── repository/                      # 4 repository interfaces
│   └── usecase/
│       ├── SaveIncomingEventUseCase      # SHA-256 dedup + single atomic Room transaction
│       ├── ProcessDeliveryQueueUseCase   # Semaphore(3) — concurrent delivery with drain loop
│       ├── AddDestinationUseCase         # Validation + encrypted credential save
│       ├── GetDeliveryLogsUseCase
│       ├── RetryFailedEventUseCase
│       └── CleanupOldDataUseCase
│
├── sender/
│   ├── Sender.kt                        # interface Sender { suspend fun send(...): SendResult }
│   ├── SenderEngine.kt                  # Routes by DestinationType
│   ├── webhook/WebhookSender.kt         # POST / GET / PUT, bearer token, custom headers
│   └── telegram/TelegramSender.kt       # Telegram Bot API
│
├── background/
│   ├── ImmediateDeliveryWorker.kt       # Expedited, chunk=10, drains full queue in while-loop
│   ├── PeriodicDeliveryWorker.kt        # Safety net every 15 min
│   ├── CleanupWorker.kt                 # Data retention, runs every 24h
│   └── WorkScheduler.kt                 # Single scheduling facade — all enqueue calls go here
│
├── receiver/
│   ├── SmsReceiver.kt                   # SMS_RECEIVED, exported=false, goAsync, priority 999
│   └── BootReceiver.kt                  # BOOT_COMPLETED + MY_PACKAGE_REPLACED
│
├── service/
│   ├── GatewayForegroundService.kt      # foregroundServiceType=dataSync, START_STICKY
│   └── GatewayNotificationListener.kt   # filter → deduplicate → parse → save → schedule
│
├── notification/
│   ├── NotificationFilterManager.kt     # Package-based allowlist
│   ├── NotificationDeduplicator.kt
│   └── parser/                          # BankingAppParser, GenericParser, ParserDispatcher
│
├── connectivity/
│   └── ConnectivityObserver.kt          # NetworkCallback → WorkScheduler on internet restore
│
├── di/                                  # DatabaseModule, RepositoryModule, SenderModule,
│                                        # WorkerModule, NotificationModule, SecurityModule
│
└── ui/
    ├── theme/                           # M3 dark — Primary #4B6EF5, Background #0F1117
    ├── navigation/                      # AppNavGraph + Screen sealed class
    ├── permission/                      # PermissionScreen + PermissionViewModel
    ├── dashboard/                       # Live stats — service status, queue counters
    ├── destinations/                    # Destination CRUD
    └── logs/                            # Delivery logs with status badges + per-row retry
```

---

## Getting Started

### Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11
- Device or emulator running Android 8.0+ (API 26+)
- Physical device strongly recommended for SMS testing — emulator SMS support is unreliable

### Clone and run

```bash
git clone https://github.com/Am24an/PulseGate-android.git
cd PulseGate-android
./gradlew assembleDebug
./gradlew installDebug
```

### Permissions

On first launch, the onboarding screen walks through each permission individually:

| Permission | Required for |
|---|---|
| `RECEIVE_SMS` + `READ_SMS` | SMS capture |
| Notification Listener | Banking app notification capture |
| `POST_NOTIFICATIONS` (API 33+) | Persistent foreground service notification |
| Battery optimization exemption | Service survival on aggressive OEM ROMs |

Partial grants are fine — unavailable features are clearly indicated in the UI.

---

## Configuration

### Webhook destination

Go to **Destinations → +** and fill in:

| Field | Details |
|---|---|
| Name | Any label, e.g. `My Backend` |
| Type | `WEBHOOK` |
| URL | `https://your-server.com/sms-hook` |
| Method | `POST` / `GET` / `PUT` |
| Bearer Token | Optional — sent as `Authorization: Bearer <token>` |
| Headers JSON | Optional — e.g. `{"X-Api-Key": "abc123"}` |
| Timeout | 5–60 seconds (default 15s) |

### Telegram destination

| Field | Details |
|---|---|
| Type | `TELEGRAM` |
| Bot Token | Create a bot via [@BotFather](https://t.me/botfather) |
| Chat ID | Get yours from [@userinfobot](https://t.me/userinfobot) |

Multiple destinations are fully supported. One incoming event creates one queue row per active destination, all processed concurrently.

### Webhook payload format

```json
{
  "eventId": 42,
  "eventHash": "a3f9c1...",
  "sourceType": "SMS",
  "sender": "+919876543210",
  "title": null,
  "message": "Your OTP is 123456. Do not share.",
  "receivedTimestamp": 1714900000000,
  "appPackage": null
}
```

For notifications: `sourceType` → `NOTIFICATION`, `appPackage` → source package (e.g. `com.phonepe.app`), `title` → notification title.

---

## How It Works

### SMS path

```
Incoming SMS
  └─► SmsReceiver.onReceive()                      [goAsync — exits in <100ms]
        └─► SaveIncomingEventUseCase
              ├── Compute SHA-256(sender + body + timestamp)
              ├── Reject if hash already exists in DB
              └── Single Room transaction
                    ├── INSERT → incoming_events
                    └── INSERT → delivery_queue (one row per active destination)
                          └─► WorkScheduler.scheduleImmediateDelivery()
                                └─► ImmediateDeliveryWorker  [Expedited]
                                      └─► ProcessDeliveryQueueUseCase
                                            ├── while(true) loop — drain full queue
                                            ├── Semaphore(3) — max 3 concurrent
                                            ├── Lock row atomically before dispatch
                                            └─► SenderEngine.dispatch()
                                                  ├── WebhookSender  → your server
                                                  └── TelegramSender → Telegram Bot API
```

### Notification path

```
Banking app notification
  └─► GatewayNotificationListener.onNotificationPosted()
        ├── NotificationFilterManager — check package allowlist
        ├── NotificationDeduplicator — reject duplicate keys
        ├── ParserDispatcher → BankingAppParser or GenericParser
        └─► SaveIncomingEventUseCase → same delivery pipeline
```

### Retry schedule

| Attempt | Delay |
|---|---|
| 1 | 1 min |
| 2 | 5 min |
| 3 | 15 min |
| 4 | 30 min |
| 5 | 1 hr |
| 6 | 6 hr |

Retries on network errors, timeouts, and HTTP 5xx.  
**No retry on HTTP 4xx** — if the server rejects the request, fix the config, not the retry count.  
After 6 failures → `FAILED`. A **Retry** button in the Logs screen resets it to `PENDING`.

---

## Database Schema

| Table | Key columns |
|---|---|
| `incoming_events` | `event_hash UNIQUE`, `source_type`, `sender`, `message`, `received_timestamp` |
| `delivery_queue` | `status`, `retry_count`, `next_retry_at`, `locked`, `locked_at`, `worker_id` |
| `destinations` | `type`, `base_url`, `method`, `api_key`, `is_active` |
| `delivery_logs` | `status`, `http_code`, `latency_ms`, `retry_attempt`, `error_message` |

**Queue state machine:**

```
PENDING
  └─► PROCESSING ──► SENT
            ├──► RETRY ──► PROCESSING  (after backoff delay)
            └──► FAILED ──► PENDING    (manual retry from UI)
```

**Retention policy** (auto-cleanup via `CleanupWorker` every 24h):

| Data | Kept for |
|---|---|
| SENT events | 72 hours |
| FAILED events | 7 days |
| Delivery logs | 30 days |
| PENDING events | Never auto-deleted |

---

## OEM Battery Optimization

Android OEMs — especially Xiaomi, Samsung, and Realme — kill background processes aggressively. Without exempting PulseGate, the foreground service may be stopped and new SMS will not be received. The in-app **Settings screen** deep-links you directly to the right page for your device.

| OEM | Path |
|---|---|
| Xiaomi (MIUI) | Security → Battery → PulseGate → No restrictions |
| Samsung (One UI) | Settings → Battery → Background usage limits → Never sleeping apps |
| Realme / Oppo (ColorOS) | Settings → Battery → Battery optimization → Not optimized |
| Vivo (Funtouch OS) | Settings → Battery → High background power consumption → Allow |
| OnePlus (OxygenOS) | Settings → Battery → Battery optimization → Don't optimize |
| Pixel / Stock Android | No extra action needed |

---

## Extending PulseGate

Adding Slack, Discord, or any other channel means implementing one interface. The queue, retry, logging, and cleanup pipeline is completely unchanged.

```kotlin
class SlackSender @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Sender {
    override suspend fun send(payload: EventPayload, destination: Destination): SendResult {
        // build your Slack webhook request here
    }
}
```

Then:

1. Register in `SenderModule.kt`
2. Add `SLACK` to the `DestinationType` enum
3. Add a `when` branch in `SenderEngine.dispatch()`
4. Add the `SLACK` option in `AddEditDestinationScreen`

That is the entire integration surface.

---

## Testing

```bash
./gradlew test                    # unit tests
./gradlew connectedAndroidTest    # instrumented tests
```

**Stack:** JUnit 4 · MockK · Kotlin Coroutines Test · Room in-memory DB · Espresso

| Area | What is tested |
|---|---|
| `SaveIncomingEventUseCase` | Duplicate hash discarded; new hash saves and returns real ID |
| `WebhookSender` | Request construction, header injection, HTTP error → `SendResult` mapping |
| Retry logic | Backoff delay values correct per attempt |
| `DeliveryQueueDao` | Atomic lock/unlock; stale lock release; `PROCESSING` excluded from re-fetch |
| `ProcessDeliveryQueueUseCase` | Semaphore correctly caps concurrent deliveries at 3 |

---

## Roadmap

- [ ] Events screen — browse captured SMS and notifications
- [ ] Notification package manager — manage allowed packages from within the app
- [ ] Test Connection button on Destinations screen
- [ ] Slack sender
- [ ] Discord sender
- [ ] Firebase sender
- [ ] Certificate pinning for webhook connections
- [ ] CSV export for delivery logs
- [ ] Play Store release with proper Room migration strategy

---

## Contributing

```bash
git clone https://github.com/Am24an/PulseGate-android.git
git checkout -b feat/your-change
./gradlew test
git commit -m "feat: your change description"
git push origin feat/your-change
# open a PR on GitHub
```

A few things that keep the codebase consistent:

- Business logic lives in use cases, not ViewModels or Composables
- Every new use case needs a unit test
- Keep PRs focused — one change per PR
- Open an issue before adding a new external dependency

---

## License

```
MIT License — Copyright (c) 2026 Aman

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software to deal in the Software without restriction, including the rights to use, copy,
modify, merge, publish, distribute, and sublicense. The above copyright notice shall be
included in all copies. The software is provided "as is", without warranty of any kind.
```

---

<p align="center">
  Built with ❤️ by <strong>Aman Kumar Gupta</strong><br/><br/>
  <em>Clean architecture is not about following rules. It is about writing code
  you can still read six months later at 2 AM.</em><br/><br/>
  <a href="https://github.com/Am24an/PulseGate-android">github.com/Am24an/PulseGate-android</a>
  &nbsp;·&nbsp;
  Drop a ⭐ if this helped you build something.
</p>
