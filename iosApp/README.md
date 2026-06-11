# iOS Host App

This folder contains the native iOS host for the Kotlin Multiplatform app.

## Google Sign-In setup

The iOS Google button is intentionally hidden until the native Google OAuth
configuration is present. The app currently has the Firebase web client ID, but
still needs the iOS client values for bundle id `org.sudsandshine.app`.

Create or open the Firebase iOS app for that bundle id, then download
`GoogleService-Info.plist`. Copy these two values into
`Configuration/Config.xcconfig`:

```xcconfig
GOOGLE_IOS_CLIENT_ID=<CLIENT_ID from GoogleService-Info.plist>
GOOGLE_IOS_REVERSED_CLIENT_ID=<REVERSED_CLIENT_ID from GoogleService-Info.plist>
```

`GOOGLE_WEB_CLIENT_ID` must stay set to the Firebase web OAuth client because
the shared auth flow exchanges the returned Google ID token with Firebase Auth.

If the button is still hidden in a debug build, check the Xcode console. The
host logs the missing Google Sign-In configuration before disabling the Kotlin
bridge.
