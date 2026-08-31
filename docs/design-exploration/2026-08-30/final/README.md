# Final mobile design baseline

These reference captures were recorded from the final Android implementation on the API 35 emulator. They cover the native and Compose launch hand-off, onboarding, authentication, the two Home scroll states, services, booking, and every persistent customer destination.

| Journey | Reference |
| --- | --- |
| Launch | [Native launch](01-native-launch.png), [Compose continuation](02-compose-splash.png) |
| Entry | [Onboarding](03-onboarding.png), [Authentication](04-auth.png) |
| Home | [Expanded](05-home.png), [Collapsed](06-home-scrolled.png) |
| Booking | [Services](07-services.png), [Booking flow](08-booking.png) |
| Destinations | [Bookings](09-bookings.png), [Rewards](10-rewards.png), [Profile](11-profile.png) |

## Release-hardening record

- Automated WCAG checks cover brand body text, action, status, and essential-icon colour pairs. Body text is at least 4.5:1 and essential icons are at least 3:1.
- Visible interactive semantics were inspected on onboarding, authentication, Home, and services; targets are at least 48 dp and carry meaningful labels. The Home loyalty card exposes its progress without a nested click target.
- With Android's animator duration scale disabled, two Compose splash captures taken 300 ms apart were byte-identical. The continuous loading pulse is not composed when reduced motion is enabled.
- On the physical Samsung SM-A566B, a warm Home scroll rendered 692 frames with 3.61% jank, a 10 ms median, 13 ms p95, and 25 ms p99. No visible stutter or slow bitmap uploads were observed. Debug-flow total PSS was 219,976 KB.
- The Android validation build was installed side-by-side under a temporary application id, exercised on the physical device, then removed. The original installed app and its data were not changed.
- A signed arm64 iOS build succeeded for the network-connected iPhone and the side-by-side validation bundle installed successfully. The phone was locked when launch was requested, so iOS rejected the launch before app execution; the temporary bundle was then removed without touching the existing app.
- Production splash artwork was losslessly compressed and visually compared with its source before capture.

The final automated gate, `./gradlew allTests :composeApp:lintDebug :composeApp:assembleRelease`, passed after all implementation and hardening changes.
