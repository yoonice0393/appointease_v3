# AppointEase Mobile

Android mobile application for St. Therese Multi-Specialty Services, Inc.

This repository contains the AppointEase mobile app for patients and doctors. Patients can sign up, find an existing clinic record, book appointments, view calendars/history, manage profile and medical history, and receive notifications. Doctors can view approved appointments, manage availability, update appointment status, and receive notifications.

## Tech Stack

| Area | Technology |
| --- | --- |
| Platform | Android |
| Language | Java |
| Build System | Gradle |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Notifications | Firebase Cloud Messaging + Realtime Database |
| Calendar | Material CalendarView |
| Networking | Volley, OkHttp |

## Main Features

### Patient

- Sign up and email verification
- Find existing clinic record during sign-up
- Login attempt validation and lockout dialogs
- Forgot password / reset password
- Book appointment
- Add medical history during booking
- Save medical history to profile
- View approved appointments on calendar
- View appointment history and details
- View and mark notifications as read/unread
- Browse doctors
- Edit profile and medical history

### Doctor

- Doctor login
- View approved upcoming appointments
- View appointment details and patient medical history
- Mark appointment as completed with doctor note
- Cancel appointment with reason
- Manage availability schedule
- Add/modify/block schedule exceptions
- View doctor calendar, history, and notifications

## Related Repositories

| Repository | Purpose |
| --- | --- |
| `appointease-web` | Laravel clinic staff and manager website |
| `appointease-api` | PHP notification/email API |
| `appointease-mobile` | This Android app |

## Requirements

- Android Studio
- JDK bundled with Android Studio
- Firebase project access
- `google-services.json` from the clinic Firebase project
- Android device or emulator

## Local Setup

1. Open this folder in Android Studio.
2. Place the Firebase configuration file here:

```text
app/google-services.json
```

3. Sync Gradle.
4. Build and run on an emulator or Android phone.

## Build Command

From the repository root:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:compileDebugJavaWithJavac
```

For a debug APK:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

## Firebase Setup

The mobile app uses:

- Firebase Authentication
- Cloud Firestore
- Firebase Realtime Database
- Firebase Cloud Messaging

If the clinic creates a new Firebase project, replace:

```text
app/google-services.json
```

with the new file from Firebase Console.

## Important Firestore Collections

- `users`
- `patients`
- `doctors`
- `appointments`
- `existing_patients`
- doctor schedule collections
- schedule exception collections
- `login_attempts`

## Important Realtime Database Paths

- `notifications/{userId}`
- `fcm_tokens`
- `password_resets`

## Existing Patient Find Record Flow

During patient sign-up, the Find Record page checks the `existing_patients` collection using:

```text
first_name + last_name + dob
```

Expected `existing_patients` document:

```json
{
  "first_name": "string",
  "last_name": "string",
  "dob": "YYYY-MM-DD",
  "created_at": "timestamp",
  "account_created": false
}
```

The app blocks sign-up if:

- no record is found
- multiple matching records are found
- the record already has an account linked

## API Endpoints

The app calls the hosted API for:

- email verification
- password reset email
- FCM token saving
- notification dispatch
- Render warm-up ping

Before turnover, confirm all hardcoded API URLs point to the clinic-owned API service.

## Production Checklist

Before building the final clinic APK:

1. Replace `google-services.json` with the clinic-owned Firebase project config.
2. Confirm all API URLs point to the clinic-owned Render/API service.
3. Remove or archive unused old PHP/email scripts from the Android source tree.
4. Remove personal email credentials from any old scripts.
5. Disable `android:usesCleartextTraffic="true"` if all endpoints use HTTPS.
6. Remove the genetic algorithm simulation screen if it is not part of the final client system.
7. Build a signed APK/AAB with a clinic-owned keystore.
8. Store the keystore and passwords in the clinic password manager.

## Testing Checklist

Test these flows before release:

- Patient sign-up using Find Record
- Duplicate/linked existing patient blocking
- Patient login and lockout dialogs
- Forgot password
- Booking appointment
- Staff approval from website
- Patient and doctor notification after approval
- Doctor appointment list shows approved appointments only
- Doctor cancellation with reason
- Doctor completion with note
- Patient calendar shows approved appointments only
- Patient notification read/unread and mark-all-as-read
- Doctor schedule exception save and calendar display

## Notes

Keep this repository private. It contains application logic for clinic and patient workflows and may reference Firebase/API project identifiers.

