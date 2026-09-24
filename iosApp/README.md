# iOS Host App

This folder contains the native iOS host for the Kotlin Multiplatform app.

## Google Sign-In setup

The iOS Google button is hidden if the native Google OAuth configuration is not
present. The Firebase web, iOS, and reversed iOS client values are currently set
for the production bundle ID `com.sudseshine.app` in `Configuration/Config.xcconfig`.

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

## Apple Sign-In setup

Apple Sign-In is implemented for iOS only. The production configuration uses
bundle ID `com.sudseshine.app`, Apple team `7MR6LD3GZC`, and
`APPLE_SIGN_IN_ENABLED=YES`. That switch exposes Apple's native button and selects
`iosApp/iosApp.apple-sign-in.entitlements` for Debug and Release builds. The App ID
must retain the **Sign in with Apple** capability and the Firebase project
`sudsandshine-bd3e2` must retain the Apple provider. A Services ID and OAuth code
flow configuration are not required for this native token flow. No Apple private
key belongs in the app or repository.

Test on an iPhone signed into iCloud with a two-factor-enabled Apple account.

The Swift coordinator generates a fresh 256-bit nonce for each attempt, sends its
SHA-256 hash to Apple, and passes the ID token and original nonce to shared Kotlin.
Ktor exchanges them for a Firebase session using `accounts:signInWithIdp` and
`apple.com`. Only Firebase session tokens go into the existing session store;
Apple tokens and nonces remain in memory. The existing iOS session store uses
UserDefaults; migrating session credentials to Keychain remains separate work.

Apple supplies the name only on the initial authorization. When supplied and the
Firebase profile is unnamed, the repository saves it through `accounts:update`.
If that optional update fails, login succeeds and the captured name is retained
in the local session; users can save their profile later to synchronize it.
Existing names are preserved, absent names are accepted, and private relay email
aliases are not used as display names. Account conflicts ask users to sign in
using their existing method; this flow does not implement account linking.

### Verification

Run the mocked Firebase/repository/ViewModel tests and the real iOS bridge tests:

```sh
./gradlew :data:testDebugUnitTest :feature:auth:testDebugUnitTest \
  :feature:auth:iosSimulatorArm64Test :composeApp:assembleDebug
```

The native nonce helper can be tested without signing or an iOS test host:

```sh
xcrun swiftc iosApp/iosApp/AppleSignInNonce.swift \
  iosApp/tests/AppleSignInNonceTests.swift -o /tmp/suds-apple-nonce-tests
/tmp/suds-apple-nonce-tests
```

The simulator can be used to inspect the button layout, but real Apple
authentication should be verified on a signed-in physical device. No fake
sign-in bypass is included in the application.

### End-to-end checks

- First login with Share My Email and Hide My Email; verify the Firebase user has
  provider `apple.com`, the expected name, and a working customer profile.
- Returning login when Apple supplies no name; sign out, relaunch, and refresh an
  expired Firebase session.
- Cancel the Apple sheet, retry, test offline recovery, and verify repeated taps
  cannot start concurrent requests.
- Try an email already associated with Google/password and verify the existing
  account is handled correctly without losing customer data.
- Confirm Google/password login on both platforms and no Apple option on Android.

Public release still requires the account-deletion flow tracked in
`docs/PRODUCTION_LAUNCH_CHECKLIST.md`, including Apple reauthentication and token
revocation. Firebase's revocation flow needs the Services ID and OAuth code flow
configuration (Team ID, Key ID, private key) even though native token sign-in can
be configured without them. Configure Apple's private email relay for any sender
that emails relay addresses. Backend deletion work must use the canonical
production backend in `../SudsAndShineFirebase`.

References: [Firebase Apple authentication](https://firebase.google.com/docs/auth/ios/apple),
[Firebase identity-provider REST API](https://docs.cloud.google.com/identity-platform/docs/reference/rest/v1/accounts/signInWithIdp).
