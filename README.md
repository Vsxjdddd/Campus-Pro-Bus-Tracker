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
| Registration & login | Student and driver accounts. DUT email only (`@dut4life.ac.za` or `@dut.ac.za`). Password must be `$$Dut` + 6 digits (e.g. `$$Dut123456`). Accounts and passwords are handled by Supabase Auth. |
| Student route | Student picks a residence (64 DUT residences, searchable) and pickup point (Residence or Campus); destination is set automatically. Route can be changed later. |
| Available transport | Student sees only live buses for their residence and direction, with status and ETA. |
| Live tracking | Route map drawn with Canvas, bus marker moving along the road, ETA countdown (mm:ss), progress bar and status card: **En route → Arriving soon → Arrived**. |
| Notifications | High-priority system notifications for *arriving soon* and *arrived*, including when the app is in the background. Runtime permission on Android 13+. |
| Driver portal | Start trip (direction, residence, expected duration), end trip, trip validation, and live driver updates to students. Trips end automatically when the duration is reached. |
| Backend | Supabase Auth + Postgres: accounts, trips and alerts shared between phones, refreshed every 2 seconds. |

## Project structure

```
app/src/main/java/za/ac/dut/campuspro/
├── CampusProApp.kt              Application: creates repository + notification channel
├── MainActivity.kt              Single activity, Compose entry point, notification permission
├── data/
│   ├── Models.kt                User, Role, Direction, TripSession, TransportAlert
│   ├── Residences.kt            DUT residence list
│   ├── SupabaseConfig.kt        Supabase project URL and public (anon) key
│   ├── SupabaseApi.kt           Small REST client: sign up, sign in, read tables, call functions
│   └── CampusRepository.kt      Data layer: syncs with Supabase every 2 s, 1 s clock, notifications
├── domain/
│   ├── Validators.kt            Email / password / phone / bus rules
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
supabase/schema.sql              Database tables, security rules and trip functions
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
4. Press the green **Run ** button.
5. Run the unit tests: right-click `app/src/test/java` → **Run 'Tests in …'** (or `./gradlew test`).

## Demo script (two phones)

Both phones need internet. The driver and the student use separate phones.

1. **Phone 1 – register a driver:** Driver Portal → Register → e.g. `driver@dut.ac.za`, `$$Dut123456`, bus `B1033`, phone `0821234567`.
2. **Phone 2 – register a student:** Register as Student → `21234567@dut4life.ac.za`, `$$Dut654321`,
   residence *Astra House*, pickup point *Residence* (destination becomes Campus).
3. **Phone 1:** log in as the driver → Start trip: *Res → Campus*, residence *Astra House*, duration **3** minutes.
4. **Phone 2:** log in as the student → Bus B1033 appears under *Available transport* within about 2 seconds → **Track bus**.
5. Watch the bus move, the countdown drop and the status change to *Arriving soon* and *Arrived*.
   Press Home to background the app – the notifications still arrive.
6. **Phone 1:** send a driver update such as “Traffic on the N2, 10 min late”; the student is notified.

## Supabase backend (live data across phones)

Campus Pro stores accounts, trips and alerts in Supabase, so a driver and students can use different phones.

1. In Supabase › **SQL Editor** › New query: paste `supabase/schema.sql` and click **Run**.
2. Supabase › **Authentication › Sign In / Providers › Email**: turn **off** "Confirm email", then Save.
3. Supabase › **Project Settings › API Keys**: copy the **anon public** key (or the publishable key).
   Paste it into `app/src/main/java/za/ac/dut/campuspro/data/SupabaseConfig.kt` (`ANON_KEY`).
   Never use the service_role / secret key in the app.
4. Run the app. Both phones need internet; they refresh every 2 seconds.

Security: the app can only read data. Starting, ending and updating trips go through database
functions that check the signed-in user (`auth.uid()`), and Row Level Security blocks direct writes.
Only `@dut4life.ac.za` / `@dut.ac.za` emails can register (checked in the app and in the database).



## Testing

- **Unit tests (JUnit 4):** 15 tests in `ValidatorsTest` and `TripMathTest` – all pass.
- **Functional tests:** 9 manual tests on a Pixel 8 emulator and a physical phone (login, role check,
  route filtering, live ETA, status changes, notifications, permission denied, driver update, two-phone sync) – 9/9 pass.

## Limitations

- Bus position is simulated from elapsed trip time, not real GPS.
- Notifications only arrive while the app is running (open or in the background); push notifications would be needed when it is fully closed.
- The route map is drawn with Canvas; a production version would use Google Maps.
