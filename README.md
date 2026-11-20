# ApplyMate

ApplyMate is an offline-first Android app for organizing job applications, tracking progress, and scheduling follow-up reminders. Account and career data are stored locally on the device.

## Features

- Register, log in, change a password, and manage a local profile.
- Create, view, edit, delete, search, filter, and sort job applications.
- Track application status with dashboard summaries and status/monthly charts.
- Create, edit, complete, filter, and delete reminders with local notifications.
- Manage reminder notification preferences and Android notification permissions.
- Keep each account's applications and reminders scoped to that user.

## Technologies and tools

- Java (Android source and application logic)
- XML (layouts, themes, and Android resources)
- SQLite through `SQLiteOpenHelper`
- AndroidX AppCompat, Activity, RecyclerView, and Material Components
- Android Studio, Android Gradle Plugin, and Gradle

## Database

The app stores its SQLite database as `APPLYMATE.db` in Android's private application data directory. It contains `users`, `job_applications`, and `reminders` tables. Passwords are stored as salted PBKDF2 hashes. Foreign-key constraints cascade deletion of a user's applications and reminders; reminder-to-application relationships also cascade when an application is deleted.

The current database schema is version 3. Upgrade migrations preserve data from the earlier schemas, including the version 1 application and reminder fields. Database files are private to each app installation and are not part of this repository.

## Project structure

```text
app/
  src/
    main/
      java/com/example/applymate/       Activities, adapters, charts, and navigation
      java/com/example/applymate/database/ SQLite schema and operations
      java/com/example/applymate/models/   Application, reminder, and user models
      java/com/example/applymate/utils/    Session, password, and preference helpers
      res/layout/                         XML screens and reusable components
      res/values/                         Colors, strings, themes, and options
    test/                                 Local unit tests
    androidTest/                          Android instrumentation tests
gradle/                                   Gradle wrapper and dependency catalog
```

## Installation

1. Install Android Studio with a compatible JDK and Android SDK Platform 36.
2. Clone this repository and open its root directory in Android Studio.
3. Allow Gradle to sync and install any requested Android SDK components.
4. Select an emulator or connected Android device running Android 6.0 (API 23) or later.

Android Studio creates the machine-specific `local.properties` file as needed; it should not be committed.

## How to run

Run the `app` configuration from Android Studio, or build from the project root:

```text
gradlew.bat assembleDebug
```

Run local unit tests with:

```text
gradlew.bat testDebugUnitTest
```

Run instrumentation tests with an available emulator or connected device:

```text
gradlew.bat connectedDebugAndroidTest
```

## Screenshots

Screenshots can be added here after capturing the app on an emulator or device.

## Future improvements

- Add optional encrypted export and import for locally stored data.
- Expand automated UI coverage for reminder editing, notification permissions, and smaller-screen layouts.
- Add user-selectable chart time ranges and accessibility descriptions for chart data.
