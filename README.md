# Suds mobile app

This repository contains the customer and admin mobile application.

## Backend ownership

The canonical production Firebase backend lives in the sibling `../FirebaseSuds` repository. Its `functions/`, Firestore rules, indexes, and Storage rules define the production contract used by this app and the website.

The local `functions/` directory is retained temporarily for emulator and migration compatibility. Production function deployments from this repository are intentionally blocked by `firebase.json`; deploy from `../FirebaseSuds` instead.

Before releasing the app, run the mobile/backend contract check from the Firebase repository:

```bash
cd ../FirebaseSuds
npm run test:consumer-contract
```

## Mobile verification

```bash
./gradlew allTests :composeApp:lintDebug :composeApp:assembleRelease
```

Crashlytics collection is disabled in debug builds and enabled in release builds. This keeps local testing out of production crash reports while preserving launch observability.

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
