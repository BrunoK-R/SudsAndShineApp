# Premium booking and account validation

Validated on 2026-08-31 using the connected physical Samsung SM-A566B
(`R5CY82030AL`, Android API 36). No emulator was used.

## Acceptance evidence

| Screenshot | Acceptance point |
| --- | --- |
| `final/01-home-generated-services.png` | Home uses generated service photography and the centered generated booking mark. |
| `final/02-service-selection.png` | Home **Ver todos** opens the same premium photographic service selector as the booking action. |
| `final/03-extras-second-step.png` | Extras are a distinct second step after choosing a service. |
| `final/04-generated-vehicle-selection.png` | Passenger and SUV categories use generated automotive imagery. |
| `final/05-profile-without-fake-map.png` | The unsupported address/map preview is absent from Profile. |
| `final/06-functional-login.png` | Login exposes email/password and Google sign-in with guest fallback. |

## Interaction checks

- The Home brand mark opens Profile.
- The Home notification bell opens notification preferences.
- Home exits an in-progress booking instead of restoring the screen it just popped.
- Email/password authentication reaches the live Firebase backend and returns the expected localized invalid-credentials response for a dummy account.
- Google authentication opens Android's system account chooser; the check was cancelled before selecting an account.
- Firebase email/password authentication was enabled for the project.
- The connected debug signing SHA-1 was registered with the existing Firebase Android app, and the downloaded Android OAuth client entry is tracked in `composeApp/google-services.json`.

The generated asset prompts and conversion details are recorded in
`../2026-08-30/ASSET_PROVENANCE.md`.
