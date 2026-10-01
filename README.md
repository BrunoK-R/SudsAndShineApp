# Suds mobile app

This repository contains the customer and admin mobile application.

## Backend ownership

The canonical production Firebase backend lives in the sibling `../SudsAndShineFirebase` repository. Its `functions/`, Firestore rules, indexes, and Storage rules define the production contract used by this app and the website.

The local `functions/` directory is retained temporarily for emulator and migration compatibility. Production function deployments from this repository are intentionally blocked by `firebase.json`; deploy from `../SudsAndShineFirebase` instead.

Before releasing the app, run the mobile/backend contract check from the Firebase repository:

```bash
cd ../SudsAndShineFirebase
npm run test:consumer-contract
```

## Mobile verification

```bash
./gradlew allTests :composeApp:lintDebug :composeApp:assembleRelease
```

Crashlytics collection is disabled in debug builds and enabled in release builds. This keeps local testing out of production crash reports while preserving launch observability.

The current release recommendation, external account gates, manual smoke matrix, and operational launch tasks are tracked in [the production launch checklist](docs/PRODUCTION_LAUNCH_CHECKLIST.md).

## Android production bundle

The release APK may be assembled unsigned for local verification. The Play Store App Bundle intentionally refuses to build until all four signing values are supplied as Gradle properties or environment variables:

```text
SUDS_RELEASE_STORE_FILE
SUDS_RELEASE_STORE_PASSWORD
SUDS_RELEASE_KEY_ALIAS
SUDS_RELEASE_KEY_PASSWORD
```

With signing configured, create the upload bundle with:

```bash
./gradlew :composeApp:bundleRelease
```

Keep the keystore and passwords outside this repository. Back them up securely before the first production upload because future updates must use the same upload identity.

## Notification setup

The home screen checks the app's native notification settings on startup and resume. When notifications are disabled, its banner opens the device's notification settings. Signed-in devices register automatically after native permission is allowed. Admin booking policy includes the “Autoaceitar marcações” switch; it defaults to enabled and keeps the existing slot capacity and blocking checks.

Android uses the normal `org.sudsmobile.app` package and production Firebase by default. iOS uses the existing `com.sudseshine.app` Firebase app, Firebase Messaging installation IDs, explicit APNs registration, and native notification tap routing. The iOS host includes its production Firebase SDK configuration.

On 2026-10-01, topic-specific APNs keys were configured in Firebase project `sudsandshine-bd3e2` for Apple team `7MR6LD3GZC`: Sandbox `HC8MVAJSLG` and Production `DLPN3AJV9K`. Both keys are restricted to `com.sudseshine.app`, whose Apple App ID has Push Notifications enabled. Private `.p8` files are stored outside Git and are never bundled with the app. Debug and Release both use production Firebase; their APNs environments are development and production respectively. Signed provisioning and actual delivery still need verification on a physical iPhone.
