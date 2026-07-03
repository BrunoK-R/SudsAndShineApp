"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {
  tokenAllowsPreference,
  tokenPreferencePatch,
  userProfilePreferencePatch,
} = require("../src/notificationPreferences");

test("token preference patch mirrors every delivery gate used for token queries", () => {
  assert.deepEqual(
    tokenPreferencePatch({
      bookingStatusEnabled: true,
      appointmentReminderEnabled: false,
      loyaltyEnabled: true,
      adminPendingAlertEnabled: false,
      marketingEnabled: true,
    }),
    {
      bookingStatusEnabled: true,
      appointmentReminderEnabled: false,
      loyaltyEnabled: true,
      adminPendingAlertEnabled: false,
      marketingEnabled: true,
    },
  );
});

test("user profile preference patch keeps campaign consent and reminders in sync", () => {
  assert.deepEqual(
    userProfilePreferencePatch({
      appointmentReminderEnabled: true,
      marketingEnabled: false,
    }),
    {
      appointmentReminderOptIn: true,
      marketingOptIn: false,
    },
  );
});

test("token preference gate uses explicit token value before user preference fallback", () => {
  assert.equal(
    tokenAllowsPreference(
      { bookingStatusEnabled: false },
      "bookingStatusEnabled",
      { bookingStatusEnabled: true },
    ),
    false,
  );
  assert.equal(
    tokenAllowsPreference(
      { bookingStatusEnabled: true },
      "bookingStatusEnabled",
      { bookingStatusEnabled: false },
    ),
    true,
  );
});

test("legacy tokens fall back to user preferences for transactional notifications", () => {
  assert.equal(
    tokenAllowsPreference({}, "adminPendingAlertEnabled", { adminPendingAlertEnabled: true }),
    true,
  );
  assert.equal(
    tokenAllowsPreference({}, "appointmentReminderEnabled", { appointmentReminderEnabled: false }),
    false,
  );
});

test("legacy tokens do not opt into marketing by default", () => {
  assert.equal(tokenAllowsPreference({}, "marketingEnabled", {}), false);
  assert.equal(tokenAllowsPreference({}, "bookingStatusEnabled", {}), true);
});
