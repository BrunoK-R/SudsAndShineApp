"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const { defaultNotificationSettings } = require("../src/notificationLifecycle");
const {
  deliverReservationEvents,
  preferenceGateForTemplate,
} = require("../src/notificationDelivery");

test("pending reservation delivery sends customer and admin notifications through the right preference gates", async () => {
  const calls = [];
  const logs = [];
  const deliveries = await deliverReservationEvents({
    before: null,
    after: reservation({ status: "pending" }),
    settings: defaultNotificationSettings,
    getAdminTokens: async (preferenceKey) => {
      calls.push({ target: "admins", preferenceKey });
      return ["admin-token"];
    },
    getUserTokens: async (uid, preferenceKey) => {
      calls.push({ target: "customer", uid, preferenceKey });
      return ["customer-token"];
    },
    sendToTokens: async (tokens, message) => {
      calls.push({ target: "send", tokens, templateKey: message.data.templateKey });
      return { sent: tokens.length, failed: 0 };
    },
    logDelivery: async (event, reservationPayload, sent) => {
      logs.push({ event, reservationId: reservationPayload.id, sent });
    },
  });

  assert.deepEqual(
    deliveries.map((delivery) => ({
      target: delivery.event.target,
      templateKey: delivery.event.templateKey,
      preferenceKey: delivery.preferenceKey,
      tokenCount: delivery.tokenCount,
      sent: delivery.sent.sent,
    })),
    [
      {
        target: "customer",
        templateKey: "booking_request",
        preferenceKey: "bookingStatusEnabled",
        tokenCount: 1,
        sent: 1,
      },
      {
        target: "admins",
        templateKey: "admin_pending_booking",
        preferenceKey: "adminPendingAlertEnabled",
        tokenCount: 1,
        sent: 1,
      },
    ],
  );
  assert.deepEqual(calls, [
    { target: "customer", uid: "user-1", preferenceKey: "bookingStatusEnabled" },
    { target: "send", tokens: ["customer-token"], templateKey: "booking_request" },
    { target: "admins", preferenceKey: "adminPendingAlertEnabled" },
    { target: "send", tokens: ["admin-token"], templateKey: "admin_pending_booking" },
  ]);
  assert.equal(logs.length, 2);
  assert.equal(logs[0].reservationId, "reservation-1");
});

test("delivery respects global switches and disabled templates without sending or logging", async () => {
  const deliveries = await deliverReservationEvents({
    before: reservation({ status: "pending" }),
    after: reservation({ status: "confirmed" }),
    settings: {
      ...defaultNotificationSettings,
      bookingStatusEnabled: false,
    },
    getAdminTokens: async () => {
      throw new Error("admin tokens should not be requested");
    },
    getUserTokens: async () => {
      throw new Error("user tokens should not be requested");
    },
    sendToTokens: async () => {
      throw new Error("send should not be called");
    },
    logDelivery: async () => {
      throw new Error("log should not be called");
    },
  });

  assert.deepEqual(deliveries, []);
});

test("delivery maps special templates to loyalty and reminder preferences", () => {
  assert.equal(preferenceGateForTemplate("loyalty_reward"), "loyaltyEnabled");
  assert.equal(preferenceGateForTemplate("booking_reminder"), "appointmentReminderEnabled");
  assert.equal(preferenceGateForTemplate("booking_completed"), "bookingStatusEnabled");
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
