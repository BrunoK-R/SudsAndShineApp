"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {
  buildNotificationMessage,
  canonicalStatus,
  defaultNotificationSettings,
  notificationAllowed,
  pickTemplate,
  reservationLifecycleEvents,
} = require("../src/notificationLifecycle");

test("new pending reservation notifies customer and admins", () => {
  const events = reservationLifecycleEvents(null, reservation({ status: "pending" }));

  assert.deepEqual(events, [
    { target: "customer", templateKey: "booking_request" },
    { target: "admins", templateKey: "admin_pending_booking" },
  ]);
});

test("status transitions map to customer lifecycle notifications", () => {
  assert.deepEqual(
    reservationLifecycleEvents(reservation({ status: "pending" }), reservation({ status: "confirmed" })),
    [{ target: "customer", templateKey: "booking_accepted" }],
  );
  assert.deepEqual(
    reservationLifecycleEvents(reservation({ status: "confirmed" }), reservation({ status: "in_progress" })),
    [{ target: "customer", templateKey: "booking_in_progress" }],
  );
  assert.deepEqual(
    reservationLifecycleEvents(reservation({ status: "pending" }), reservation({ status: "rejected" })),
    [{ target: "customer", templateKey: "booking_rejected" }],
  );
});

test("completion sends completed notification and review prompt", () => {
  const events = reservationLifecycleEvents(
    reservation({ status: "in_progress" }),
    reservation({ status: "completed" }),
  );

  assert.deepEqual(events, [
    { target: "customer", templateKey: "booking_completed" },
    { target: "customer", templateKey: "review_prompt" },
  ]);
});

test("loyalty reward event fires only when reward becomes available", () => {
  const events = reservationLifecycleEvents(
    reservation({ status: "completed", loyalty: { rewardReady: false, availableRewards: 0 } }),
    reservation({ status: "completed", loyalty: { rewardReady: true, availableRewards: 1 } }),
  );

  assert.deepEqual(events, [{ target: "customer", templateKey: "loyalty_reward" }]);
});

test("reschedule returning to pending notifies customer and admins", () => {
  const events = reservationLifecycleEvents(
    reservation({ status: "confirmed", slotStart: "2026-07-03T10:00:00.000Z" }),
    reservation({ status: "pending", slotStart: "2026-07-04T11:00:00.000Z" }),
  );

  assert.deepEqual(events, [
    { target: "admins", templateKey: "admin_pending_booking" },
    { target: "customer", templateKey: "booking_rescheduled" },
  ]);
});

test("template rendering produces app routing data", () => {
  const template = pickTemplate(defaultNotificationSettings, "booking_completed");
  const message = buildNotificationMessage(template, reservation({
    id: "reservation-1",
    reservationCode: "SS-001",
    status: "completed",
  }));

  assert.equal(message.data.type, "booking_status");
  assert.equal(message.data.templateKey, "booking_completed");
  assert.equal(message.data.reservationId, "reservation-1");
  assert.equal(message.data.reservationCode, "SS-001");
  assert.match(message.notification.title, /Lavagem/);
});

test("global switches gate relevant notification groups", () => {
  const settings = {
    ...defaultNotificationSettings,
    bookingStatusEnabled: false,
    appointmentReminderEnabled: false,
    loyaltyEnabled: false,
    adminPendingAlertEnabled: false,
  };

  assert.equal(notificationAllowed(settings, "booking_completed"), false);
  assert.equal(notificationAllowed(settings, "booking_reminder"), false);
  assert.equal(notificationAllowed(settings, "loyalty_reward"), false);
  assert.equal(notificationAllowed(settings, "admin_pending_booking"), false);
  assert.equal(notificationAllowed(defaultNotificationSettings, "booking_completed"), true);
  assert.equal(notificationAllowed(defaultNotificationSettings, "booking_reminder"), true);
});

test("portuguese status aliases normalize to canonical lifecycle states", () => {
  assert.equal(canonicalStatus("confirmado"), "confirmed");
  assert.equal(canonicalStatus("aceita"), "confirmed");
  assert.equal(canonicalStatus("em execução"), "in_progress");
  assert.equal(canonicalStatus("a decorrer"), "in_progress");
  assert.equal(canonicalStatus("concluído"), "completed");
  assert.equal(canonicalStatus("concluída"), "completed");
  assert.equal(canonicalStatus("recusada"), "rejected");
  assert.equal(canonicalStatus("cancelada"), "cancelled");
});

function reservation(overrides = {}) {
  return {
    id: "reservation-1",
    reservationCode: "SS-001",
    customerName: "Bruno Ribeiro",
    serviceName: "Lavagem Premium",
    slotStart: "2026-07-03T10:00:00.000Z",
    slotEnd: "2026-07-03T10:45:00.000Z",
    userUid: "user-1",
    ...overrides,
  };
}
