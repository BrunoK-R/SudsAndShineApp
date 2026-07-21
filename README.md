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
./gradlew :feature:profile:allTests :data:allTests :composeApp:assembleDebug
```
