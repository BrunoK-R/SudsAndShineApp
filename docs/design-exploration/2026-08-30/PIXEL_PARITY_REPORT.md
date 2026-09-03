# Physical pixel-parity report

## Acceptance target

- Device: Samsung SM-A566B (`R5CY82030AL`), Android 16 / API 36.
- Comparison viewport: 1080 × 2400 at 420 dpi (approximately 411 × 914 dp).
- Fixture: Bruno, Leiria, Lavagem Premium at 10:30, seven of ten stamps; Premium selected in booking.
- References: `concepts/01-modern-home.png` and `concepts/02-modern-booking.png`.

The AI concepts have different source dimensions and omit Android's status-area geometry. Physical measurements therefore use the normalized geometry contract in `PIXEL_PARITY_PLAN.md`; platform-owned pixels are excluded and production photography is reviewed visually rather than treated as UI chrome.

## Physical measurements

| Surface | Physical result | Contract | Result |
| --- | ---: | ---: | --- |
| Home content gutter | 47 px / 17.9 dp | 17–20 dp | Pass |
| Appointment hero | 986 × 659 px / 375.6 × 251 dp | 377 × 251 dp, safe-area aligned | Pass |
| Primary booking action | 986 × 173 px / 375.6 × 65.9 dp | 377 × 66 dp | Pass |
| Home service cards | 357 × 473 px / 136 × 180.2 dp | approximately 136 × 180 dp | Pass |
| Loyalty strip | 986 × 186 px / 375.6 × 70.9 dp | approximately 377 × 73 dp | Pass |
| Booking Standard card | 986 × 475 px / 375.6 × 181 dp | 377 × 181 dp | Pass |
| Booking Premium card | 986 × 512 px / 375.6 × 195 dp | 377 × 195 dp | Pass |
| Sticky booking summary | 1016 × 236 px / 387 × 89.9 dp | approximately 90 dp high | Pass |
| Navigation indicator alignment | 0 dp horizontal and vertical centre delta | centred on selected icon | Pass |

Bounds come from the committed UI hierarchy captures beside the screenshots. The two-pixel horizontal normalization difference is the device's integer rounding of the 17–20 dp safe gutter.

## Behaviour and quality gates

- Native launch is a plain `#142539` handoff. The designed Compose splash appears once; there is no duplicate native logo or black/white flash.
- Home uses the production composables with a debug-only deterministic data fixture. The fixture changes data only; it does not replace the UI.
- The Home header collapses to 64 dp, hero artwork uses 0.35× parallax, and navigation uses the 280 ms shared-axis contract.
- The selected navigation indicator is derived from the five visual slots and the icon/label geometry, with unit coverage for both axes.
- Booking renders Standard, Premium, and Exterior in order, preserves the selected Premium service, advances to `2 de 4`, and restores the selection and `45 min · 32€` summary on back.
- The central booking action and the persistent destinations expose button/tab semantics and Portuguese content descriptions.
- Reduced-motion paths use snap/fade fallbacks and retain the same layout.

## Evidence

- `final/09-home-pixel-parity-android.png` and `.xml` — expanded Home.
- `final/10-home-collapsed-android.png` and `.xml` — 64 dp collapsed header.
- `final/11-booking-pixel-parity-android.png` and `.xml` — selected-service booking state.
- `final/12-splash-continuity-android.png` — chronological cold-launch contact sheet.
- `final/13-splash-compose-android.png` — designed Compose splash.
- `final/14-navigation-alignment-android.png` — selected indicator/icon crop.
- `final/15-reference-actual-contact-sheet.png` — Home and booking reference/physical pairs.

## Verification

- Full multiplatform test suite: passed.
- Android debug lint: passed.
- Android debug and release assemblies: passed.
- iOS simulator targets: compiled and tested.
- Physical Android install, cold launch, expanded/collapsed Home, booking forward/back, and navigation alignment: passed.
- No emulator was used for acceptance.

The iPhone was not used because it was not available as an unlocked development target; no simulator time was spent in its place.
