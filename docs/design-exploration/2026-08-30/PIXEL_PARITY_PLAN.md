# AI reference pixel-parity plan

## Acceptance baseline

The visual source of truth is the generated concept set in `concepts/`:

- `01-modern-home.png`
- `02-modern-booking.png`
- `03-motion-navigation-storyboard.png`

The previous `final/` captures are implementation checkpoints, not approval baselines. Business rules, live user data, safe areas, and accessibility semantics remain authoritative, but the rendered authenticated Home and selected-service booking states must reproduce the concepts' composition.

## Reference viewport

The portrait concepts map to the existing 1080 × 2400 Android reference device at approximately 411 × 914 dp. Measurements below are normalized to that viewport. Platform-owned status/navigation pixels are excluded from comparisons; the app-owned navigation shell is included.

### Home target geometry

| Element | Target geometry |
| --- | --- |
| Content gutter | 17–20 dp |
| Identity header | 88 dp high; 56 dp brand mark; greeting and location; notification action |
| Appointment hero | 377 × 251 dp; 20 dp radius; image weighted to the right; dark text scrim on the left |
| Primary booking action | 377 × 66 dp; capsule; 16 dp below hero |
| Service heading | Baseline around 467 dp from the content top |
| Service rail | 180 dp high; approximately 136 dp cards; 8 dp gaps |
| Loyalty strip | 377 × 73 dp; single-row stamp progress |
| Navigation shell | 377 × 76 dp; centered raised action; 17 dp side gutter |

Equivalent capture state:

- authenticated first name `Bruno`;
- location `Leiria`;
- upcoming `Lavagem Premium`, Tuesday at `10:30`;
- seven of ten loyalty stamps;
- three visible service families: Exterior, Completa, and Detailing.

### Booking target geometry

| Element | Target geometry |
| --- | --- |
| Header | 20 dp gutter; 48 dp accessible back target; title and `1 de 4` progress |
| Category filters | Three capsule controls beginning around 88 dp from the app content top |
| Service cards | 377 dp wide; 181–195 dp high; 16 dp gaps; 18–20 dp radius |
| Service imagery | Full-bleed right-weighted photography with a stable dark left scrim |
| Selected state | 2 dp cyan outline, cyan check, restrained glow, champagne popular badge |
| Sticky summary | Approximately 90 dp high above navigation; thumbnail, service, duration/price, CTA |
| Navigation shell | Approximately 76 dp high with selected central booking action |

Equivalent capture state:

- loaded catalog with Standard, Premium, and Exterior in that order;
- `Lavagem Premium` selected;
- no extras expanded in the visible viewport;
- summary `Lavagem Premium · 45 min · 32€` and enabled `Continuar` action.

## Visual gates

1. At 1080 × 2400, major container edges and text baselines differ by no more than 2 physical pixels after safe-area alignment.
2. Sampled brand surfaces differ by no more than a perceptual delta of 3; gradients must preserve the same direction and visual weight.
3. Manrope family, weight, line height, wrapping, and truncation match the reference hierarchy.
4. A 50% reference/build overlay shows no doubled edges in app chrome, cards, controls, or navigation.
5. Non-photographic regions reach SSIM 0.97 or better; the complete frame reaches SSIM 0.92 or better. Photo texture and dynamic platform glyphs are masked only for the metric, not for visual review.
6. Scroll and destination motion match the storyboard: 0.35× hero parallax, 64 dp compact header, 280 ms shared-axis navigation, haptic selection feedback, and non-continuous reduced-motion fallbacks.
7. The same production composables used by live data render the deterministic comparison states; no screenshot-only replacement UI is accepted.
8. Physical Android validation, signed iOS compilation, all tests, Android lint, and release assembly must pass after the final visual iteration.

## Delivery tickets

### P0 — Lock measurements and comparison rules

This document is the review contract for subsequent captures.

### P1 — Production photographic asset set

Create cross-platform, text-free automotive imagery from the approved AI direction: one upcoming-booking hero and three service treatments. Preserve negative space needed for Compose overlays, optimize files, and record prompts/source roles.

### P2 — Home parity

Rebuild the expanded authenticated hierarchy, photographic hero, CTA, compact service rail, loyalty strip, and collapsed header. Keep guest/loading/empty/error states functional with the same geometry.

### P3 — Booking and navigation parity

Add filters, photographic service cards, exact selected state, compact sticky summary, and the reference navigation proportions without changing booking rules.

### P4 — Device convergence and release evidence

Capture equivalent states, produce overlays and metrics, iterate until every visual gate passes, then run accessibility, reduced-motion, performance, regression, and release checks.
