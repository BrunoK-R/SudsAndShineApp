"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {
  adminReservationExpectedStatuses,
  buildDaySlots,
  capacityForDate,
  normalizeLoyalty,
  reservationEarnsLoyaltyStamp,
  slotOverlapsBlockedRange,
} = require("../src/bookingDomain");

test("admin reservation lifecycle requires start before complete", () => {
  assert.deepEqual(adminReservationExpectedStatuses("accept"), ["pending"]);
  assert.deepEqual(adminReservationExpectedStatuses("reject"), ["pending", "confirmed"]);
  assert.deepEqual(adminReservationExpectedStatuses("start"), ["confirmed"]);
  assert.deepEqual(adminReservationExpectedStatuses("complete"), ["in_progress"]);
});

test("loyalty reward becomes ready after ten paid completed washes", () => {
  const ninth = normalizeLoyalty({ totalWashes: 9, claimedRewards: 0 });
  assert.equal(ninth.rewardReady, false);
  assert.equal(ninth.currentWashes, 9);
  assert.equal(ninth.remainingWashes, 1);

  const tenth = normalizeLoyalty({ totalWashes: 10, claimedRewards: 0 });
  assert.equal(tenth.rewardReady, true);
  assert.equal(tenth.availableRewards, 1);
  assert.equal(tenth.currentWashes, 10);
  assert.equal(tenth.remainingWashes, 0);
});

test("loyalty free wash disappears after the reward is claimed", () => {
  const claimed = normalizeLoyalty({ totalWashes: 10, claimedRewards: 1 });

  assert.equal(claimed.rewardReady, false);
  assert.equal(claimed.availableRewards, 0);
  assert.equal(claimed.currentWashes, 0);
  assert.equal(claimed.remainingWashes, 10);
});

test("loyalty stamp is only awarded for non-reward reservations with valid payment state", () => {
  assert.equal(reservationEarnsLoyaltyStamp({ paymentStatus: "pending" }), true);
  assert.equal(reservationEarnsLoyaltyStamp({ paymentStatus: "paid" }), true);
  assert.equal(reservationEarnsLoyaltyStamp({ paymentStatus: "covered_by_loyalty" }), false);
  assert.equal(reservationEarnsLoyaltyStamp({ paymentStatus: "failed" }), false);
  assert.equal(reservationEarnsLoyaltyStamp({ paymentStatus: "refunded" }), false);
  assert.equal(reservationEarnsLoyaltyStamp({ paymentStatus: "cancelled" }), false);
  assert.equal(reservationEarnsLoyaltyStamp({ paymentStatus: "falhou" }), false);
  assert.equal(reservationEarnsLoyaltyStamp({ loyaltyRewardApplied: true, paymentStatus: "paid" }), false);
});

test("loyalty target follows admin settings while keeping ten as default", () => {
  assert.equal(normalizeLoyalty({ totalWashes: 4 }).targetWashes, 10);

  const custom = normalizeLoyalty(
    { totalWashes: 5, claimedRewards: 0 },
    { stampsRequired: 5, rewardDescription: "Lavagem oferta" },
  );

  assert.equal(custom.targetWashes, 5);
  assert.equal(custom.rewardReady, true);
  assert.equal(custom.rewardDescription, "Lavagem oferta");
});

test("capacity override replaces the default capacity for one date", () => {
  const config = {
    capacityPerSlot: 2,
    capacityOverrides: [
      { date: "2026-07-03", maxBookingsPerSlot: 0 },
      { date: "2026-07-04", maxBookingsPerSlot: 3 },
    ],
  };

  assert.equal(capacityForDate("2026-07-02", config), 2);
  assert.equal(capacityForDate("2026-07-03", config), 0);
  assert.equal(capacityForDate("2026-07-04", config), 3);
});

test("blocked interval matches any overlapping service slot", () => {
  const blocked = {
    slotStart: "2026-07-03T10:30:00.000Z",
    slotEnd: "2026-07-03T11:30:00.000Z",
  };

  assert.equal(
    slotOverlapsBlockedRange("2026-07-03T10:00:00.000Z", "2026-07-03T10:45:00.000Z", blocked),
    true,
  );
  assert.equal(
    slotOverlapsBlockedRange("2026-07-03T11:00:00.000Z", "2026-07-03T11:45:00.000Z", blocked),
    true,
  );
  assert.equal(
    slotOverlapsBlockedRange("2026-07-03T11:30:00.000Z", "2026-07-03T12:15:00.000Z", blocked),
    false,
  );
});

test("availability slots account for interval blocks, reservations, and capacity", () => {
  const slots = buildDaySlots(
    "2026-07-03",
    { hoursLabel: "10:00 - 12:00" },
    30,
    45,
    {
      capacityPerSlot: 2,
      capacityOverrides: [],
      blockedSlots: [
        {
          date: "2026-07-03",
          slotStart: "2026-07-03T10:30:00.000Z",
          slotEnd: "2026-07-03T11:30:00.000Z",
        },
      ],
    },
    new Map([["2026-07-03T10:00:00.000Z", 1]]),
  );

  assert.deepEqual(slots, [
    { time: "10:00", available: false, remainingCapacity: 0 },
    { time: "10:30", available: false, remainingCapacity: 0 },
    { time: "11:00", available: false, remainingCapacity: 0 },
  ]);
});
