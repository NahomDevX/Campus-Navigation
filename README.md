# Campus Navigation System

Android mobile application in Java for Android Studio that provides:

- Google Maps based outdoor campus navigation
- Firebase-backed building directory and event listings
- Offline caching with Room
- Simulated Bluetooth indoor navigation
- Event reminders with WorkManager and Firebase Cloud Messaging support
- MVVM architecture with repository and data layers

## Project Structure

- `app/src/main/java/com/example/campusnavigation`
- `ui/` activities, fragments, adapters
- `viewmodel/` presentation state
- `data/local/` Room entities and DAOs
- `data/repository/` Firebase + offline repositories
- `service/` FCM service
- `worker/` local event reminder scheduling
- `assets/sample/` seed data for offline mode and first launch

## Tech Stack

- Java
- Android Studio / Gradle
- Google Maps SDK for Android
- Google Play Services Location
- Firebase Auth
- Firebase Firestore
- Firebase Cloud Messaging
- Room
- WorkManager
- Material 3

## Setup

### 1. Open the project

1. Open Android Studio.
2. Choose `Open`.
3. Select `C:\one drive document\OneDrive\Documents\New project`.
4. Let Gradle sync finish.

### 2. Configure Google Maps

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select a project.
3. Enable:
   - `Maps SDK for Android`
   - `Places API` if you want future place autocomplete
4. Create an Android API key.
5. Restrict it to your Android app package and SHA-1 where possible.
6. Replace the key in `app/build.gradle`:

```gradle
buildConfigField "String", "MAPS_API_KEY", "\"YOUR_REAL_GOOGLE_MAPS_API_KEY\""
```

7. Sync Gradle again.

### 3. Configure Firebase

1. Open [Firebase Console](https://console.firebase.google.com/).
2. Create a Firebase project.
3. Add an Android app with package name:

```text
com.example.campusnavigation
```

4. Download `google-services.json`.
5. Place it inside:

```text
app/google-services.json
```

6. In Firebase Console enable:
   - Authentication
   - Firestore Database
   - Cloud Messaging

### 4. Enable Firebase Auth

1. Firebase Console → Authentication → Sign-in method.
2. Enable:
   - Email/Password
   - Anonymous

### 5. Create Firestore collections

Create two collections:

- `buildings`
- `events`

Sample documents are provided below and mirrored in the local asset files.

#### `buildings` sample

```json
{
  "id": "main_library",
  "name": "Main Library",
  "description": "Library with reading halls, digital catalog, and study rooms.",
  "type": "Library",
  "latitude": 9.5941,
  "longitude": 41.8669,
  "favorite": false
}
```

#### `events` sample

```json
{
  "id": "library_workshop",
  "name": "Research Skills Workshop",
  "buildingId": "main_library",
  "buildingName": "Main Library",
  "eventTimeMillis": 1778225400000,
  "description": "Digital library search and citation training."
}
```

### 6. Run the app

1. Use an Android 8.0+ device or emulator with Google Play services.
2. Build and run from Android Studio.
3. Grant:
   - Location
   - Bluetooth
   - Notifications on Android 13+

## Core Features

### Interactive Campus Map

- Centers on Dire Dawa University coordinates by default
- Displays custom building markers from Room/Firebase data
- Shows the current user location when permission is granted
- Supports normal, satellite, and terrain map types
- Draws a simple route preview to a selected destination

### Building Directory

- Real-time local search using Room-backed data
- Tap a building to open it on the map
- Long-press a building to toggle favorite status
- Swipe to refresh to sync from Firebase

### Indoor Navigation Simulation

- Uses a simulated beacon model in Java
- Mimics Bluetooth indoor positioning flow
- Generates step-by-step route instructions between rooms

### Event Notifications

- Firestore-backed event list
- Offline fallback from local JSON assets
- Local reminder notifications scheduled with WorkManager
- FCM service included for remote push expansion

### Offline Mode

- Room stores buildings and events for offline viewing
- Local sample assets seed the database if Firebase is unavailable
- Searches continue to work without internet after local seed/sync

## Notes

- Firestore is used as the source of truth when available.
- Local assets act as an offline bootstrap and demo dataset.
- The map route preview is a campus-scale straight-line preview, which is production-friendly as a base and can later be replaced with a full Directions API integration.
- Indoor navigation intentionally simulates BLE beacons so the app remains demoable without physical hardware.

## Recommended Next Improvements

- Add Firestore security rules for authenticated read access
- Replace straight-line route preview with walkable campus paths
- Add per-building indoor maps stored in Firestore
- Add topic-based FCM subscriptions for departments and clubs
