"use strict";

const assert = require("node:assert/strict");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

if (!process.env.FIRESTORE_EMULATOR_HOST) {
  throw new Error("Run this script via the Firestore emulator.");
}

initializeApp({ projectId: process.env.GCLOUD_PROJECT || "sudsandshine-bd3e2" });

const db = getFirestore();
const reservationId = `smoke-reservation-${Date.now()}`;
const reservationRef = db.doc(`reservations/${reservationId}`);
const rejectedReservationId = `${reservationId}-rejected`;
const rejectedReservationRef = db.doc(`reservations/${rejectedReservationId}`);

const expectedCompletedTemplates = [
  "booking_request",
  "admin_pending_booking",
  "booking_accepted",
  "booking_in_progress",
  "booking_completed",
  "review_prompt",
  "loyalty_reward",
];
const expectedRejectedTemplates = [
  "booking_request",
  "admin_pending_booking",
  "booking_rejected",
];

async function main() {
  await reservationRef.set({
    userUid: "smoke-user",
    reservationCode: "SMOKE-001",
    customerName: "Smoke User",
    customerEmail: "smoke@example.test",
    customerPhone: "+351000000000",
    serviceId: "premium",
    serviceName: "Lavagem Premium",
    slotStart: "2026-07-03T10:00:00.000Z",
    slotEnd: "2026-07-03T10:45:00.000Z",
    status: "pending",
    loyalty: {
      rewardReady: false,
      availableRewards: 0,
    },
  });
  await waitForDeliveries(reservationId, ["booking_request", "admin_pending_booking"]);

  await reservationRef.set({ status: "confirmed" }, { merge: true });
  await waitForDeliveries(reservationId, ["booking_accepted"]);

  await reservationRef.set({ status: "in_progress" }, { merge: true });
  await waitForDeliveries(reservationId, ["booking_in_progress"]);

  await reservationRef.set({
    status: "completed",
    loyalty: {
      rewardReady: true,
      availableRewards: 1,
      rewardDescription: "1 lavagem gratis",
    },
  }, { merge: true });
  const completedDeliveries = await waitForDeliveries(reservationId, expectedCompletedTemplates);

  await rejectedReservationRef.set({
    userUid: "smoke-user",
    reservationCode: "SMOKE-002",
    customerName: "Smoke User",
    customerEmail: "smoke@example.test",
    customerPhone: "+351000000000",
    serviceId: "premium",
    serviceName: "Lavagem Premium",
    slotStart: "2026-07-03T12:00:00.000Z",
    slotEnd: "2026-07-03T12:45:00.000Z",
    status: "pending",
  });
  await waitForDeliveries(rejectedReservationId, ["booking_request", "admin_pending_booking"]);

  await rejectedReservationRef.set({ status: "rejected" }, { merge: true });
  const rejectedDeliveries = await waitForDeliveries(rejectedReservationId, expectedRejectedTemplates);

  const completedTemplates = completedDeliveries.map((item) => item.templateKey).sort();
  assert.deepEqual(completedTemplates, [...expectedCompletedTemplates].sort());
  assertDeliveryCounters(completedDeliveries, reservationId);

  const rejectedTemplates = rejectedDeliveries.map((item) => item.templateKey).sort();
  assert.deepEqual(rejectedTemplates, [...expectedRejectedTemplates].sort());
  assertDeliveryCounters(rejectedDeliveries, rejectedReservationId);

  const templates = [...new Set([...completedTemplates, ...rejectedTemplates])].sort();
  console.log(`notification smoke ok: ${templates.join(", ")}`);
}

async function waitForDeliveries(id, requiredTemplates) {
  const required = new Set(requiredTemplates);
  const deadline = Date.now() + 30000;
  while (Date.now() < deadline) {
    const snapshot = await db.collection("notificationDeliveries")
      .where("reservationId", "==", id)
      .get();
    const deliveries = snapshot.docs.map((doc) => doc.data());
    const deliveredTemplates = new Set(deliveries.map((item) => item.templateKey));
    if ([...required].every((templateKey) => deliveredTemplates.has(templateKey))) {
      return deliveries;
    }
    await sleep(500);
  }
  const snapshot = await db.collection("notificationDeliveries")
    .where("reservationId", "==", id)
    .get();
  const templates = snapshot.docs.map((doc) => doc.get("templateKey")).sort();
  throw new Error(`Timed out waiting for ${[...required].join(", ")}. Got: ${templates.join(", ")}`);
}

function assertDeliveryCounters(deliveries, id) {
  assert.equal(deliveries.every((item) => item.reservationId === id), true);
  assert.equal(deliveries.every((item) => Number(item.sent) === 0), true);
  assert.equal(deliveries.every((item) => Number(item.failed) === 0), true);
  assert.equal(deliveries.every((item) => Number(item.invalidated || 0) === 0), true);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
