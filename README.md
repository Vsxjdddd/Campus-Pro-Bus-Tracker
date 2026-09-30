# Campus Pro – Real-Time University Shuttle Tracking (Android)

Campus Pro is an Android app for Durban University of Technology (DUT) students and shuttle drivers.
Students see which bus is running on their route, watch it move on a live route map with an ETA
countdown, and get a notification when the bus is arriving soon and when it has arrived. Drivers start
and end trips and post live updates (delays, route changes) to students on that route.

**Module:** MOSA202 – Mobile Operating System Technology and Application (DUT)
**Built with:** Android Studio · Kotlin · Jetpack Compose · MVVM (ViewModel + StateFlow) · Navigation Compose

---

## Features

| Area | What it does |
|---|---|
| Registration & login | Student and driver accounts. DUT email only (`@dut4life.ac.za` or `@dut.ac.za`). Password must be `$$Dut` + 6 digits (e.g. `$$Dut123456`). Passwords are stored as salted SHA-256 hashes, never plain text. |
| Student route | Student picks a residence (64 DUT residences, searchable) and pickup point (Residence or Campus); destination is set automatically. Route can be changed later. |
| Available transport | Student sees only live buses for their residence and direction, with status and ETA. |
| Live tracking | Route map drawn with Canvas, bus marker moving along the road, ETA countdown (mm:ss), progress bar and status card: **En route → Arriving soon → Arrived**. |
| Notifications | High-priority system notifications for *arriving soon* and *arrived*, including when the app is in the background. Runtime permission on Android 13+. |
| Driver portal | Start trip (direction, residence, expected duration), end trip, trip validation, and live driver updates to students. Trips end automatically when the duration is reached. |
| Persistence | Accounts, active trips and alerts survive app restarts; users stay logged in. |

## Project structure

```
app/src/main/java/za/ac/dut/campuspro/
├── CampusProApp.kt              Application: creates repository + notification channel
├── MainActivity.kt              Single activity, Compose entry point, notification permission
├── data/
│   ├── Models.kt                User, Role, Direction, TripSession, TransportAlert
│   ├── Residences.kt            DUT residence list
│   └── CampusRepository.kt      Data layer: storage, trip simulation ticker, alerts
├── domain/
│   ├── Validators.kt            Email / password / phone / bus rules, password hashing
│   └── TripMath.kt              Progress, ETA and status calculations
├── notifications/
│   └── NotificationHelper.kt    NotificationCompat + channel
└── ui/
    ├── CampusProNavHost.kt      Navigation graph
    ├── viewmodel/ViewModels.kt  Auth, Student, Driver, Tracking ViewModels
    ├── components/Components.kt Shared UI (cards, residence picker, alerts)
    ├── screens/                 Welcome, Auth, StudentHome, Tracking, DriverHome
    └── theme/                   Orange/black DUT-inspired theme
app/src/test/.../domain/         JUnit tests for Validators and TripMath (15 tests)
```

## Requirements

- Android Studio Ladybug (2024.2) or newer (bundled JDK 17+)
- Android SDK 35 (the IDE downloads it on first sync)
- Emulator (e.g. Pixel 8, API 34/35) or an Android phone with Android 8.0+ (API 26)

## How to run

1. Open Android Studio → **File › Open** → select the `CampusPro` folder.
2. Wait for **Gradle sync** to finish (first sync downloads dependencies, needs internet).
   If Android Studio offers to upgrade the Android Gradle Plugin, you can accept or skip; both work.
3. Start an emulator from **Device Manager** (or plug in a phone with USB debugging on).
4. Press the green **Run ▶** button.
5. Run the unit tests: right-click `app/src/test/java` → **Run 'Tests in …'** (or `./gradlew test`).

## Demo script (one device)

The prototype stores data on the device, so the driver and student can be demonstrated on one phone:

1. **Register a driver:** Driver Portal → Register → e.g. `driver@dut.ac.za`, `$$Dut123456`, bus `B1033`, phone `0821234567`.
2. **Register a student:** Log out/back → Register as Student → `21234567@dut4life.ac.za`, `$$Dut654321`,
   residence *Astra House*, pickup point *Residence* (destination becomes Campus).
3. **Log in as the driver** → Start trip: *Res → Campus*, residence *Astra House*, duration **1** minute.
4. **Log out and log in as the student** → Bus B1033 appears under *Available transport* → **Track bus**.
5. Watch the bus move, the countdown drop and the status change to *Arriving soon* (last 15 s) and *Arrived*.
   Press Home to background the app – the notifications still arrive.
6. Try a driver update: log in as the driver during a trip and send “Traffic on the N2, 10 min late”;
   the student sees it under *Transport notifications*.

## Upload to GitHub

**Option A – from Android Studio (easiest)**

1. **Settings › Version Control › GitHub** → add your GitHub account.
2. **VCS › Share Project on GitHub** (newer versions: **Git › GitHub › Share Project on GitHub**).
3. Repository name `Campus-Pro-Bus-Tracker`, choose Public (so the lecturer can view it) → **Share**.
4. Android Studio makes the first commit and pushes. Copy the repository URL into the report (section 4.3).

**Option B – command line**

```bash
cd CampusPro
git init
git add .
git commit -m "Campus Pro: Android shuttle tracking app"
git branch -M main
git remote add origin https://github.com/<your-username>/Campus-Pro-Bus-Tracker.git
git push -u origin main
```

(Create the empty repository on github.com first, without a README, then run the commands.)

## Architecture (MVVM)

```
Compose screens  ──observe──▶  ViewModels (StateFlow)  ──call──▶  CampusRepository
      ▲                                                               │
      └────────────── state updates every second ◀── ticker ─────────┤
                                                                      ├─▶ SharedPreferences (JSON)
                                                                      └─▶ NotificationHelper
```

In production the repository would be backed by Firebase Authentication, Firebase Realtime Database
(driver GPS positions and trip status) and Firebase Cloud Messaging, with Google Maps SDK replacing
the Canvas route map. Because screens only observe StateFlows, the UI would not need to change.

## Limitations of the prototype

- Data lives on one device; a real deployment needs a shared backend so students and drivers use separate phones.
- Bus position is simulated from elapsed trip time, not real GPS.
- Password hashing is client-side; production should use institutional single sign-on.
