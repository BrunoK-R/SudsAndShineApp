"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {
  androidNotificationChannelId,
  androidNotificationClickAction,
  buildMulticastMessage,
  invalidTokenRecordsForBatch,
  isInvalidTokenError,
  normalizeFcmData,
  normalizeTokenRecords,
} = require("../src/fcmDelivery");

test("token records are cleaned and deduplicated by token", () => {
  const records = normalizeTokenRecords([
    " token-a ",
    { token: "token-a", uid: "ignored" },
    { token: " token-b ", uid: " user-1 ", tokenId: " device-1 ", refPath: " users/user-1/notificationTokens/device-1 " },
    "",
    { token: "" },
  ]);

  assert.deepEqual(records, [
    { token: "token-a" },
    {
      token: "token-b",
      uid: "user-1",
      tokenId: "device-1",
      refPath: "users/user-1/notificationTokens/device-1",
    },
  ]);
});

test("invalid token responses are mapped back to token records", () => {
  const records = normalizeTokenRecords([
    { token: "token-ok", refPath: "users/u/notificationTokens/ok" },
    { token: "token-gone", refPath: "users/u/notificationTokens/gone" },
    { token: "token-bad", refPath: "users/u/notificationTokens/bad" },
    { token: "token-temporary", refPath: "users/u/notificationTokens/temp" },
  ]);
  const invalid = invalidTokenRecordsForBatch(records, {
    responses: [
      { success: true },
      { success: false, error: { code: "messaging/registration-token-not-registered" } },
      { success: false, error: { errorInfo: { code: "messaging/invalid-registration-token" } } },
      { success: false, error: { code: "messaging/internal-error" } },
    ],
  });

  assert.deepEqual(
    invalid.map((record) => record.token),
    ["token-gone", "token-bad"],
  );
});

test("only permanent FCM token errors invalidate tokens", () => {
  assert.equal(isInvalidTokenError({ code: "messaging/registration-token-not-registered" }), true);
  assert.equal(isInvalidTokenError({ errorInfo: { code: "messaging/invalid-registration-token" } }), true);
  assert.equal(isInvalidTokenError({ code: "messaging/unavailable" }), false);
  assert.equal(isInvalidTokenError({ code: "messaging/internal-error" }), false);
});

test("multicast payload includes android tap routing and apns sound", () => {
  const payload = buildMulticastMessage(["token-a"], {
    notification: {
      title: "Novo pedido",
      body: "Pedido SS-001 aguarda validação.",
    },
    data: {
      type: "admin_pending_booking",
      templateKey: "admin_pending_booking",
      reservationId: "reservation-1",
      attempt: 1,
      empty: "",
    },
  });

  assert.deepEqual(payload.tokens, ["token-a"]);
  assert.deepEqual(payload.notification, {
    title: "Novo pedido",
    body: "Pedido SS-001 aguarda validação.",
  });
  assert.deepEqual(payload.data, {
    type: "admin_pending_booking",
    templateKey: "admin_pending_booking",
    reservationId: "reservation-1",
    attempt: "1",
  });
  assert.equal(payload.android.notification.channelId, androidNotificationChannelId);
  assert.equal(payload.android.notification.clickAction, androidNotificationClickAction);
  assert.equal(payload.apns.payload.aps.sound, "default");
});

test("fcm data normalization keeps only non-empty string values", () => {
  assert.deepEqual(
    normalizeFcmData({
      type: " booking_status ",
      templateKey: "booking_completed",
      reservationId: 123,
      enabled: false,
      skipped: null,
      blank: " ",
    }),
    {
      type: "booking_status",
      templateKey: "booking_completed",
      reservationId: "123",
      enabled: "false",
    },
  );
});
