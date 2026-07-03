"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {
  isWithinQuietHours,
  reminderClaimIsActive,
  reminderDedupeKey,
  reminderWindow,
  reservationNeedsReminder,
} = require("../src/bookingReminders");

test("reminder window uses sanitized lead minutes", () => {
  const window = reminderWindow(new Date("2026-07-03T10:00:00.000Z"), 30);

  assert.equal(window.startIso, "2026-07-03T10:00:00.000Z");
  assert.equal(window.endIso, "2026-07-03T10:30:00.000Z");
  assert.equal(window.leadMinutes, 30);
});

test("confirmed future reservation inside window needs reminder", () => {
  assert.equal(
    reservationNeedsReminder(
      reservation({ status: "confirmed", slotStart: "2026-07-03T11:30:00.000Z" }),
      new Date("2026-07-03T10:00:00.000Z"),
      120,
    ),
    true,
  );
});

test("reminder skips closed, already sent, ownerless and outside-window reservations", () => {
  const now = new Date("2026-07-03T10:00:00.000Z");

  assert.equal(reservationNeedsReminder(reservation({ status: "pending" }), now, 120), false);
  assert.equal(reservationNeedsReminder(reservation({ reminderSentAt: "2026-07-03T08:00:00.000Z" }), now, 120), false);
  assert.equal(reservationNeedsReminder(reservation({ userUid: "" }), now, 120), false);
  assert.equal(
    reservationNeedsReminder(reservation({ slotStart: "2026-07-03T12:30:00.000Z" }), now, 120),
    false,
  );
});

test("quiet hours support same-day and overnight windows in configured timezone", () => {
  assert.equal(
    isWithinQuietHours(new Date("2026-07-03T12:30:00.000Z"), {
      quietHoursStart: "13:00",
      quietHoursEnd: "14:00",
      quietHoursTimeZone: "Europe/Lisbon",
    }),
    true,
  );
  assert.equal(
    isWithinQuietHours(new Date("2026-07-03T06:30:00.000Z"), {
      quietHoursStart: "22:00",
      quietHoursEnd: "08:00",
      quietHoursTimeZone: "Europe/Lisbon",
    }),
    true,
  );
  assert.equal(
    isWithinQuietHours(new Date("2026-07-03T09:00:00.000Z"), {
      quietHoursStart: "22:00",
      quietHoursEnd: "08:00",
      quietHoursTimeZone: "Europe/Lisbon",
    }),
    false,
  );
});

test("active reminder claim blocks duplicate delivery until ttl expires", () => {
  const now = new Date("2026-07-03T10:00:00.000Z");

  assert.equal(
    reminderClaimIsActive(reservation({ reminderClaimedAtIso: "2026-07-03T09:50:00.000Z" }), now, 20),
    true,
  );
  assert.equal(
    reminderClaimIsActive(reservation({ reminderClaimedAtIso: "2026-07-03T09:30:00.000Z" }), now, 20),
    false,
  );
});

test("dedupe key includes reservation and slot", () => {
  assert.equal(
    reminderDedupeKey(reservation({ id: "reservation-9", slotStart: "2026-07-03T11:30:00.000Z" })),
    "booking_reminder:reservation-9:2026-07-03T11:30:00.000Z",
  );
});

function reservation(overrides = {}) {
  return {
    id: "reservation-1",
    status: "confirmed",
    slotStart: "2026-07-03T11:30:00.000Z",
    userUid: "user-1",
    ...overrides,
  };
}
