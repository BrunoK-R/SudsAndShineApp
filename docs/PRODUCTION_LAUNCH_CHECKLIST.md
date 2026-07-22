# Production launch checklist

Last reviewed: 22 July 2026

## Release recommendation

- **Android:** ready for a controlled internal or closed pilot after the unchecked P0 items below are completed. It is not yet ready for an unrestricted public launch.
- **iOS:** development preview only. Push notifications, Crashlytics, Apple signing, and App Store release verification are not complete.
- **Website:** keep its current public website and booking scope. It is not intended to reproduce the mobile customer or admin experience.
- **Payments:** the current product records payment status and tells customers to pay on site. It does not process an online payment.

The checklist uses `[x]` for items verified in code or production and `[ ]` for work that still needs a person, an external account, or a production decision.

## P0 release decision

Do not start a public launch until every item in this section is checked.

- [ ] Name one launch owner and one operational backup who can access Firebase, Google Play, Apple Developer, DNS, and customer support.
- [ ] Decide the launch scope: Android closed pilot first, or delay until Android and iOS can launch together.
- [ ] Confirm production services, extras, prices, durations, capacity, opening hours, blocked periods, cancellation window, rescheduling window, and loyalty target in the admin app.
- [ ] Confirm that every booking surface clearly promises **payment on site**. If online payment is required for launch, stop and implement a payment provider, checkout, webhook reconciliation, refunds, and support procedures first.
- [ ] Add an in-app account-deletion request/flow and document what is deleted, retained, or anonymised. No customer-facing account-deletion flow was found in the reviewed app/backend.
- [ ] Complete the Android distribution and privacy items below, including release signing, store listing, Data safety, and a real internal-track smoke test.
- [ ] Protect callable backend traffic with Firebase App Check and monitor enforcement before switching it on for all production callables.
- [ ] Configure billing budget alerts, error/latency alerts, quota monitoring, and a named response path for production incidents.
- [ ] Create and test a Firestore/Storage export, restore, retention, and customer-data deletion runbook.
- [ ] Run the end-to-end launch smoke matrix below with non-admin and admin production test accounts.

## Foundation already verified

- [x] The canonical backend is the sibling `FirebaseSuds` repository; App production function deploys are intentionally blocked.
- [x] Firebase Functions deploy successfully and the production health endpoint returns HTTP 200.
- [x] `www.sudsandshine.pt`, its principal public pages, Firebase Hosting, and production JS/CSS assets return HTTP 200 after the latest backend deploy.
- [x] The live public `getBusinessInfo` and `getServiceCatalog` callable contracts return valid responses.
- [x] Booking requests, admin acceptance/rejection/start/completion, customer cancellation/rescheduling, waitlist, history timeline, loyalty ledger/reward redemption, campaigns, favourites, and one-tap rebooking have automated coverage.
- [x] Notification delivery uses Firebase Installation IDs on current Android builds while retaining legacy-token compatibility during migration.
- [x] A physical Android device registered its production installation successfully and showed notifications active.
- [x] Android debug and release unit tests pass; Android lint and debug assembly pass.
- [x] Release builds enable Crashlytics collection and debug builds disable it.
- [x] The Android App Bundle task refuses to produce a release bundle when signing secrets are absent.
- [x] Profile photos can be chosen from the gallery, cropped/resized, uploaded, and replaced.
- [x] The Android launcher icon, adaptive icon, notification icon, backup exclusions, and release shrinker rules are configured.

## Android distribution

- [ ] Create or confirm the Google Play application for package `org.sudsandshine.app`.
- [ ] Enrol in Play App Signing and create the upload keystore. Store the keystore and recovery material in two secure locations outside Git.
- [ ] Supply `SUDS_RELEASE_STORE_FILE`, `SUDS_RELEASE_STORE_PASSWORD`, `SUDS_RELEASE_KEY_ALIAS`, and `SUDS_RELEASE_KEY_PASSWORD` in the release environment.
- [ ] Decide the first public version and bump `versionCode`/`versionName` if `1`/`1.0.0` is not the intended release identity.
- [ ] Run `./gradlew :composeApp:bundleRelease` with production signing, upload the AAB to an internal track, and verify Play’s signing/certificate details.
- [ ] Complete the Portuguese store listing: title, short/full descriptions, icon, phone screenshots, feature graphic, category, support email, support URL, privacy-policy URL, and release notes.
- [ ] Complete Content rating, Target audience, Ads, App access, Data safety, country availability, and pricing declarations using the real production data flows.
- [ ] Invite at least one tester who is not an administrator and install only from Google Play. Test upgrade, fresh install, notification permission, denied permission, background notification, deep link, and sign-out.
- [ ] Force one controlled non-fatal/fatal test in the internal build and confirm it appears in the correct Firebase Crashlytics project before removing the test path.
- [ ] Confirm Android 8, 10, 13, and the current Android release on representative small/large devices or a device lab.

## iOS parity and distribution

- [x] The iOS Google client and reversed-client values are present in `Config.xcconfig`.
- [x] The iOS simulator build has been verified during this production-readiness work.
- [ ] Set the Apple Development Team and replace the current team-derived bundle identifier arrangement with the final registered identifier if necessary.
- [ ] Add the Push Notifications capability, APNs entitlement, APNs key/certificate, Firebase Messaging integration, and an iOS implementation of `NotificationDeviceRegistrar`.
- [ ] Verify foreground, background, terminated, permission-denied, token/installation rotation, deep-link, and sign-out notification cases on a physical iPhone.
- [ ] Add Firebase Crashlytics to the iOS host, upload dSYMs, and verify a controlled crash in Firebase.
- [ ] Configure production signing, provisioning, version/build numbers, archive validation, and TestFlight.
- [ ] Complete App Store metadata, screenshots, privacy nutrition labels, age rating, review notes/test credentials, support URL, privacy URL, and account-deletion instructions.
- [ ] Run the full launch smoke matrix from a TestFlight build on at least two supported iPhone sizes.

## Backend security and reliability

- [x] Firestore/Storage rules and backend tests are versioned with the canonical Firebase repository.
- [x] Admin actions require authenticated roles and use the admin allowlist/claims flow.
- [x] Notification campaigns provide a recipient estimate, test-send path, schedule validation, and delivery reporting.
- [x] Invalid legacy tokens and installation IDs are retired from future notification sends.
- [ ] Review every production admin and employee allowlist entry; remove stale access and document who approves new roles.
- [ ] Apply least-privilege IAM to Firebase/Google Cloud users and service accounts; require MFA for all privileged people.
- [ ] Add Firebase App Check providers to Android, iOS, and the website clients as applicable. Observe metrics first, then enforce on protected callable functions.
- [ ] Configure budget thresholds, function error/latency alerts, Firestore quota alerts, notification-outbox age/failure alerts, and invalid-installation trend monitoring.
- [ ] Document the canonical deploy command, pre-deploy checks, rollback procedure, and who can deploy. Use only the `FirebaseSuds` repository for production backend deploys.
- [ ] Schedule Firestore exports and Storage backup/retention appropriate to the business. Perform and record a restore drill before public launch.
- [ ] Define log retention, customer record retention, booking/audit record retention, and account-deletion/anonymisation rules with a Portuguese/EU privacy reviewer.
- [ ] Re-run dependency auditing before each release. Six moderate transitive advisories currently come through Google Storage/Admin dependencies; do not accept npm's incompatible Admin downgrade as a fix.
- [ ] Send one real transactional notification of every enabled template to controlled production accounts and verify delivery, copy, destination, deduplication, and opt-out behaviour.
- [ ] Schedule one controlled campaign, verify the estimate, test send, single broadcast, delivered/failed counts, and conversion attribution. Confirm it cannot be sent twice accidentally.

## Product and operations configuration

- [ ] Create a production service catalogue and compare the same service, extra, price, and duration across website booking, mobile booking, admin queue, confirmation, and history.
- [ ] Confirm whether admin acceptance is required for every request and set customer expectations in all booking confirmation copy.
- [ ] Publish plain-language cancellation, rescheduling, late-arrival, no-show, payment-on-site, refund, voucher-expiry, and loyalty rules.
- [ ] Confirm the loyalty rule (currently expressed as completed washes toward a free wash), eligible services, treatment of historical washes, manual adjustments, expiry, and fraud/support handling.
- [ ] Seed historical loyalty only through the supported ledger/migration process; never rewrite customer history to make totals match.
- [ ] Decide waitlist notification expiry and response expectations. Train staff on what happens when multiple customers are notified for one cancellation.
- [ ] Train at least two operators on Today, pending requests, start/complete, cancellation, loyalty correction, campaign test/send, capacity overrides, and incident escalation.
- [ ] Prepare customer-support replies for missing bookings, notification permission, wrong vehicle, cancellation cutoff, loyalty disputes, duplicate requests, and payment questions.
- [ ] Create separate non-admin, employee, and admin production test accounts. Do not use a real customer account for release verification.
- [ ] Add lightweight product analytics or an equivalent privacy-conscious event funnel for booking started, request submitted, accepted, completed, cancelled, waitlist joined/notified/converted, reward earned/redeemed, and notification opened.

## Privacy and customer trust

- [x] The website privacy page is live.
- [ ] Review the privacy policy against the actual mobile/backend data inventory: account identity, profile photo, vehicles, bookings, loyalty, notification installation IDs, campaigns, logs, and analytics.
- [ ] Add privacy-policy and support links inside the mobile profile/settings experience.
- [ ] Implement account deletion from the app. Require recent authentication for destructive identity deletion and use a recoverable request/grace-period design if business records must be retained.
- [ ] Confirm profile-photo removal, Storage object deletion/replacement, and orphan cleanup behaviour with production accounts.
- [ ] Document data-access, correction, portability, deletion, and complaint request handling, including owner and response target.
- [ ] Check accessibility on the final store build: screen reader names, font scaling, contrast, touch targets, keyboard/focus where applicable, motion, empty/error/loading states, and Portuguese copy.

## End-to-end launch smoke matrix

Run these checks from store-distributed builds against production. Save account, time, booking reference, result, and evidence for each run.

- [ ] New customer: install, deny/allow notifications, sign up/sign in, edit profile, choose/crop/replace profile photo, add/edit/delete a vehicle, and sign out/in.
- [ ] Booking: browse services, choose vehicle/extras/time, review payment-on-site copy, submit once, and see the pending timeline without duplicates.
- [ ] Admin: see the request in Today/pending, accept it, and verify the customer timeline plus notification/deep link.
- [ ] Operations: start and complete the wash, mark payment status correctly, and verify customer history and loyalty ledger.
- [ ] Reward: complete enough eligible washes, earn the reward once, redeem it once, and verify the free booking/payment state without changing historical events.
- [ ] Customer change: reschedule inside policy, cancel inside policy, verify released capacity, and verify both customer/admin timelines.
- [ ] Waitlist: join a full slot, create a cancellation, receive availability notice, convert to a real booking, and confirm stale/duplicate entries are not reused.
- [ ] Rebooking: favourite a successful booking, rebook it, handle a no-longer-available service/extra safely, and remove the favourite.
- [ ] Notifications: test accepted, rejected, reminder, rescheduled, cancelled, waitlist, completed/review, reward, foreground/background/terminated, opt-out, sign-out, and device removal.
- [ ] Failure modes: offline launch, connection loss during submit, repeated taps, expired auth, backend error, stale app version, and recovery without duplicate bookings or rewards.
- [ ] Website regression after the final backend deploy: `/`, `/servicos`, `/contacto`, `/privacidade`, JS/CSS assets, public catalogue/info callables, and one website booking request.

## Launch and first-week operations

- [ ] Start with a small named cohort and an explicit rollback/stop condition.
- [ ] Watch Firebase errors, crashes, ANRs, function latency, quotas, notification failures, booking duplicates, payment-state mismatches, waitlist conversions, and support contacts daily.
- [ ] Reconcile completed jobs, on-site payments, loyalty credits/redemptions, cancellations, and admin actions against the real operational record each day for the first week.
- [ ] Hold a 24-hour and 7-day launch review. Turn observed problems into owned fixes with severity and deadlines.

## Product roadmap after the launch gate

These are useful retention/revenue features, but they should not block the first pilot unless the business makes them contractual launch requirements.

- [ ] **Referrals:** introduce a referral code/link, attribution window, qualification event, abuse controls, transparent status, and ledger-backed reward for both parties.
- [ ] **Prepaid packages:** define included services, validity, transfer/refund rules, purchase/payment source, remaining uses, atomic redemption, and admin correction/audit.
- [ ] **Memberships:** define tier/price/cadence, entitlements, renewal/failure/cancellation states, payment-provider webhooks, customer self-service, and revenue/support reporting.

Do not present packages or memberships as purchasable until a production payment provider and financial reconciliation process exist. If they are initially sold at the counter, label them as staff-issued and preserve an auditable admin assignment trail.

## Release commands

From the App repository:

```bash
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug assembleDebug
./gradlew :composeApp:bundleRelease
```

From the canonical `FirebaseSuds` repository:

```bash
npm ci
npm test
npm run test:consumer-contract
npx firebase-tools deploy --only functions --project sudsandshine-bd3e2
```

After every Firebase deploy, verify the health endpoint, the public callable contracts, the custom-domain website pages, Firebase Hosting, and production static assets before considering the deploy complete.
