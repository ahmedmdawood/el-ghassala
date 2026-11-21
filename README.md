# El Ghassala — الغسّالة

Offline-first shared-transport & shared-expense manager for Android.
Track trips, split costs, manage driver refunds, and settle up — fully offline, in Arabic and English.

## Features

- **Days** — group a day's trips and expenses, close and reopen days; closed days drop out of settlement scope by default
- **Shared transport** — per-person fare, who actually paid what, automatic driver-refund detection when the driver collects more than the total fare (refunds stay outstanding until you mark them collected — never auto-settled)
- **Saved routes** — origin/destination/default fare, usage counts, one-tap trip start
- **Cash expenses** — multiple payers, equal or custom shares, exact-to-the-piaster distribution
- **Transfers & settlements** — person-to-person payments, smart settle-up suggestions that minimize the number of payments
- **Balances** — live per-person net balances with a full ledger breakdown (transport / expenses / transfers / settlements / refunds)
- **Reports & stats** — daily spending chart, period comparison, averages; export to branded PDF or CSV; full JSON backup & restore
- **Search** — across days, trips, expenses and transfers
- **Arabic (RTL) + English (LTR)**, light & dark themes, per-app language

## Tech

Kotlin · Jetpack Compose (Material 3) · Room · DataStore · MVVM · Offline-first
All financial math is centralized in a pure-Kotlin engine (`domain/FinanceEngine`, `domain/BalanceCalculator`) with unit tests, including the acceptance case from the product spec (`SPEC.txt`).

## Build

Open in Android Studio (Ladybug or newer) and run, or:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Requires JDK 17, Android SDK 35.

---
صنع بواسطة أحمد داود · Made by Ahmed Dawood
