# Modern mobile UI implementation plan

## Goal

Modernize the customer-facing Suds & Shine experience without changing booking, authentication, loyalty, notification, or admin business rules. The generated concepts define the visual direction; existing ViewModels, repositories, routes, and backend contracts remain authoritative.

## Decisions made before implementation

1. **Keep the existing brand mark and Manrope font.** The current type family already supports the desired hierarchy. The redesign changes weights, scale, spacing, and colour roles rather than introducing another font.
2. **Use cyan for actions and motion, champagne for premium accents.** Champagne must not carry primary-action meaning.
3. **Keep four persistent destinations plus one central booking action.** Início, Marcações, Recompensas, and Perfil remain tabs. Booking still uses `Routes.Products`, but it is presented as the raised primary action.
4. **Do not use generated mockup photography in production.** The catalog currently exposes `iconKey` but no image URL. Version one will use code-native artwork or separately approved, licensed, bundled assets. A backend image-field migration is not required for this redesign.
5. **Preserve light and dark semantic palettes.** New customer components use the premium brand-dark system. Legacy/admin screens continue using semantic Material colours until audited; a global theme flip is deliberately avoided.
6. **Treat booking as four input steps plus review.** Service, vehicle, date/time, and contact fill the four progress segments. Confirmation is labelled as review, and success has no progress control.
7. **Correct mockup-only pricing assumptions.** Before vehicle selection, prices must say `A partir de`; the exact price appears only after the vehicle/category is known.

## Target architecture

### Shared design system

Extend `shared/src/commonMain/kotlin/com/sudsmobile/shared/theme` with semantic brand tokens rather than scattering colour literals through features:

- `SudsColors`: ink, navy, cyan, cyan-muted, champagne, glass, glass-border, success, warning, and scrim roles.
- `SudsShapes`: capsule, control, card, hero, and sheet shapes.
- `SudsSpacing`: 4 dp base grid and named content gutters.
- `SudsMotion`: 120/200/280/420 ms durations, shared easing, and selection spring.
- `SudsMotionPreferences`: common reduced-motion contract with Android/iOS implementations.

Add reusable stateless components under `shared/.../ui`:

- `SudsBrandBackground`
- `SudsCompactTopBar` and `SudsCollapsingHeader`
- `SudsPrimaryButton`
- `SudsGlassCard`
- `SudsStatusCard`
- `SudsSectionHeader`
- `SudsProgressSegments`
- `SudsServiceArtwork`

Feature modules keep ownership of business-specific cards. Shared components must not import feature or data models.

### Navigation shell

Split the current `mainDestinations` contract into four tab destinations and one booking action. Keep existing route names and `saveState`/`restoreState` behavior so notification routing and deep links do not change.

The shell owns:

- floating navigation capsule and safe-area positioning;
- selected-tab indicator motion;
- the central booking action and its selected state on `Routes.Products`;
- peer-route shared-axis transitions;
- reduced-motion fallbacks;
- semantics and minimum 48 dp targets.

The booking feature continues to own its sticky summary/continue control. Its bottom offset is derived from the navigation shell padding so the two controls never overlap.

### Screen motion

- Derive home collapse progress from `LazyListState` using `derivedStateOf`.
- Apply parallax through translation and alpha only; do not animate layout size on every scroll frame.
- Use a 24 dp shared-axis slide plus fade for peer destinations, `280 ms` fast-out-slow-in.
- Use forward/backward direction for booking-step transitions and a fade fallback when reduced motion is enabled.
- Use one supported light haptic for destination change, service selection, and completed booking steps.

## Ticket sequence

Each ticket is independently staged, reviewed, tested, approved, committed, and pushed before the next begins.

### Ticket 0 — Commit the design baseline

**Scope**

- Current screenshots, generated concepts, design audit, and this plan.

**Acceptance**

- Assets open correctly and documentation links resolve.
- No application source changes are included.

**Proposed commit**

`docs(design): add modern mobile concepts and implementation plan`

### Ticket 1 — Make native launch visually continuous

**Files likely affected**

- `composeApp/src/androidMain/AndroidManifest.xml`
- new Android theme resources under `res/values` and `res/values-v31`
- `MainActivity.kt` only if Android SplashScreen integration requires it
- launch/splash tests and capture baselines

**Work**

- Replace the default white Android launch theme with the same navy background and approved centered mark used by Compose/iOS.
- Keep the mandatory native launch frame visually indistinguishable from the Compose continuation.
- Recheck the existing iOS launch storyboard; do not rewrite it unless a mismatch is observed.

**Acceptance**

- Cold launch shows no white or black flash on Android or iOS.
- Status/navigation bar colours match the splash.
- Warm launch does not re-run onboarding or extend the splash unnecessarily.

**Tests**

- Android API 24 and API 35 build/launch captures.
- iOS signed device build and launch on Bruno's iPhone when available.
- Existing splash timing unit test.

### Ticket 2 — Add the design-system foundation

**Files likely affected**

- `shared/.../theme/Color.kt`, `Theme.kt`, `Typography.kt`
- new `Shape.kt`, `Spacing.kt`, `Motion.kt`
- new stateless shared UI components and common tests

**Work**

- Add brand semantic tokens, refined typography roles, shapes, spacing, and motion specifications.
- Add the shared primitives listed above with previews for normal, loading, error, disabled, and large-text states.
- Keep the current theme behavior for legacy screens until each customer surface migrates.

**Acceptance**

- No route or screen behavior changes.
- Components work on narrow width, tablet width, light/dark host palettes, and 1.3x font scale.
- No hard-coded colours remain inside the new primitives.

**Tests**

- Common tests for collapse interpolation, progress calculations, and reduced-motion selection.
- Android/iOS compilation.
- Preview and emulator visual matrix.

### Ticket 3 — Replace the navigation shell

**Files likely affected**

- `navigation/.../BottomNavContract.kt`
- `navigation/.../MainNavigation.kt`
- new `SudsNavigationBar.kt`
- navigation common tests

**Work**

- Introduce four tab destinations and the central booking action without changing route identifiers.
- Add the floating capsule, moving selected indicator, central-action states, haptics, and route transitions.
- Preserve notification destinations and existing `saveState`/`restoreState` behavior.

**Acceptance**

- Each tab restores its scroll/state after switching.
- A partially completed booking survives navigation away and back.
- Re-selecting the current destination does not duplicate it on the back stack.
- Central action is announced as a button; persistent destinations are announced as tabs with selected state.
- Compact widths and Portuguese labels do not clip.

**Tests**

- Unit tests for destination order, selected-route mapping, and transition direction.
- UI semantics tests for roles, labels, targets, and selected state.
- Manual deep-link/notification route smoke test.

### Ticket 4 — Rebuild Home around a collapsing hierarchy

**Files likely affected**

- `feature/home/.../HomeScreen.kt`
- extracted Home components/resources
- Home common tests

**Work**

- Replace `Column.verticalScroll` with a keyed `LazyColumn` and collapsing header state.
- Render the next appointment as the hero only for loaded users with an upcoming booking.
- Provide deliberate guest, empty, loading, warning, and error variants using the same layout rhythm.
- Add the compact booking CTA, horizontal service rail, bubble loyalty progress, and reduced-card statistics.
- Use code-native car/water artwork until production imagery is approved.

**Acceptance**

- Header collapses to a 64 dp contextual bar and expands without jumping.
- Hero parallax never hides essential text or actions.
- All existing `HomeUiState` branches remain reachable and correct.
- Service actions continue passing the selected service id.
- Scroll restoration works after tab changes and process recreation.

**Tests**

- Unit tests for collapse fraction and state-to-section mapping.
- UI tests for authenticated, guest, empty, error, and large-text states.
- Before/after screenshots at top and collapsed positions on Android and iOS.

### Ticket 5 — Decompose the booking UI without changing behavior

**Reason**

`ProductsScreen.kt` currently contains roughly 4,500 lines and owns substantial saved state. Moving layout and motion at the same time would make regressions difficult to isolate.

**Work**

- Keep saved state and side effects in `ProductsScreenContent`.
- Move stateless headers, service cards, step bodies, and bottom action UI into focused files.
- Add stable keys and semantics needed by later UI tests.
- Do not change labels, step rules, network calls, draft construction, or submit behavior.

**Acceptance**

- The full service → vehicle → date/time → contact → review → success journey behaves identically.
- Presets, extras, rewards, waitlist, payment resolution, and error recovery remain intact.

**Tests**

- Existing product ViewModel/draft/formatting tests.
- Android and iOS compilation.
- Manual booking smoke matrix for guest and authenticated paths.

### Ticket 6 — Apply the modern booking shell and service selection

**Work**

- Add compact step header and four-segment progress.
- Add shared-axis step transitions with reduced-motion fallback.
- Redesign service selection, selected state, extras, presets, and sticky selection summary.
- Keep imagery abstract/local unless approved assets are supplied.
- Extend the same shell and action hierarchy to vehicle, date/time, contact, review, and success without changing validation.

**Acceptance**

- `A partir de` remains until vehicle pricing is known.
- Continue is enabled under exactly the current business conditions.
- System keyboard, small screens, and large font sizes never cover the active field or action.
- Back navigation follows step order and exits only from the first step.
- Submission cannot be triggered twice during loading.

**Tests**

- UI tests for step progression, disabled/enabled actions, selection semantics, and restored draft state.
- Existing booking tests plus a complete emulator booking smoke test.
- iPhone build/launch and critical-flow check.

### Ticket 7 — Migrate Bookings, Rewards, and Profile

**Files likely affected**

- `feature/cart/.../CartScreen.kt`
- `feature/blog/.../BlogScreen.kt`
- `feature/profile/.../ProfileScreen.kt`

**Work**

- Replace oversized static headers with compact/collapsing shared headers.
- Establish clear hierarchy among data, empty/session states, secondary actions, and destructive actions.
- Use the same loyalty bubble/stamp language on Home and Rewards.
- Keep admin operation screens on semantic legacy surfaces in this pass.

**Acceptance**

- All authenticated, unauthenticated, loading, empty, warning, and error branches remain represented.
- Booking cancellation, rescheduling, rating, referral, reward redemption, profile photo, preferences, and sign-out remain functional.
- Destructive actions stay visually distinct and require existing confirmations.

**Tests**

- Existing ViewModel suites.
- UI state matrix for the three destinations.
- Android/iOS visual and functional smoke tests.

### Ticket 8 — Align onboarding, authentication, secondary screens, and system states

**Work**

- Apply the new type, action, status-card, and background system to onboarding/auth.
- Migrate customer secondary screens such as services, vehicles, personal data, history, notifications, contact, rating, and payment.
- Standardize loading, retry, offline, permission, and empty states.
- Audit status bar, navigation bar, keyboard, dialogs, and sheets.

**Acceptance**

- No customer path visibly falls back to the previous generic card language.
- Admin screens remain usable and semantically correct even if their deeper redesign is deferred.
- External links, permission flows, and forms still work on both platforms.

### Ticket 9 — Accessibility, performance, and release hardening

**Work**

- Audit contrast, TalkBack/VoiceOver order, touch targets, dynamic type, reduced motion, and haptics.
- Profile scroll and navigation performance; remove unnecessary recompositions and costly real-time effects.
- Compress approved raster assets and verify memory use.
- Run the complete regression matrix and capture final reference screenshots.

**Release gates**

- Body text contrast at least 4.5:1; large text and essential icons at least 3:1.
- All interactive targets at least 48 dp and have meaningful labels.
- No continuous animation when reduced motion is enabled.
- No visible frame drops during normal scroll/navigation on the physical iPhone and Android reference device.
- `./gradlew allTests :composeApp:lintDebug :composeApp:assembleRelease` passes.
- Signed iOS device build passes and the main customer flow launches successfully.

## State and regression matrix

Every migrated customer screen must be checked in these applicable states:

| Dimension | Required cases |
| --- | --- |
| Session | restoring, guest, authenticated, expired/failed restore |
| Data | loading, loaded, empty, retryable error, non-retryable error, stale warning |
| Layout | narrow phone, reference phone, large phone, landscape sanity check |
| Accessibility | 1.0x and 1.3x font scale, TalkBack/VoiceOver, reduced motion |
| Navigation | cold entry, tab switch, back, deep link/notification, process recreation |
| Booking | no preset, preset, extras, reward, waitlist, payment required, submit error, success |

## Risks and controls

- **Booking regressions:** isolate the mechanical decomposition from the visual migration and keep current state/validation ownership.
- **Performance from glossy visuals:** use static assets, gradients, translation, and alpha; avoid live blur and large animated shadows.
- **Navigation state loss:** retain route ids and saved-state navigation; add explicit restoration tests before visual rollout.
- **Generated-image mismatch:** treat concepts as references only and require approved production assets.
- **Dark-surface contrast:** use semantic tokens and automated/manual contrast checks, not colour values copied from the mockups.
- **Scope expansion into admin:** customer UI lands first; admin receives inherited semantic tokens and a separate later audit.

## Final definition of done

The redesign is complete only when the customer journey is visually coherent from native launch through booking success, all existing business states remain functional, cross-platform testing passes, accessibility/reduced-motion requirements are met, and final Android/iPhone screenshots match the approved direction closely enough to replace this exploration as the new baseline.
