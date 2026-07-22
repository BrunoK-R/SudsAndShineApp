# iOS Host App

This folder contains the native iOS host for the Kotlin Multiplatform app.

## Google Sign-In setup

The iOS Google button is hidden if the native Google OAuth configuration is not
present. The Firebase web, iOS, and reversed iOS client values are currently set
for bundle id `org.sudsandshine.app` in `Configuration/Config.xcconfig`.

If the Firebase iOS app or bundle id changes, download its
`GoogleService-Info.plist` and replace these two values in
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
