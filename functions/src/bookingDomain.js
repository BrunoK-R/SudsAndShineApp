"use strict";

function cleanString(value, maxLength) {
  return String(value || "")
    .trim()
    .replace(/\s+/g, " ")
    .slice(0, maxLength);
}

function normalizeLoyalty(data = {}, settings = {}) {
  const configuredTarget = Number.isFinite(Number(settings.stampsRequired))
    ? Number(settings.stampsRequired)
    : Number(data.targetWashes);
  const targetWashes = Number.isFinite(configuredTarget) ? Math.max(1, Number(configuredTarget)) : 10;
  const totalWashes = Number.isFinite(Number(data.totalWashes)) ? Math.max(0, Number(data.totalWashes)) : 0;
  const claimedRewards = Number.isFinite(Number(data.claimedRewards)) ? Math.max(0, Number(data.claimedRewards)) : 0;
  const completedRewards = Math.floor(totalWashes / targetWashes);
  const availableRewards = Math.max(0, completedRewards - claimedRewards);
  const currentWashes = availableRewards > 0 ? targetWashes : totalWashes % targetWashes;
  return {
    totalWashes,
    currentWashes,
    targetWashes,
    remainingWashes: availableRewards > 0 ? 0 : targetWashes - currentWashes,
    progress: targetWashes > 0 ? currentWashes / targetWashes : 0,
    rewardReady: availableRewards > 0,
    completedRewards,
    claimedRewards,
    availableRewards,
    rewardType: cleanString(settings.rewardType || data.rewardType, 80) || "free_wash",
    rewardValue: Number.isFinite(Number(settings.rewardValue || data.rewardValue))
      ? Math.max(1, Number(settings.rewardValue || data.rewardValue))
      : 1,
    rewardDescription: cleanString(settings.rewardDescription || data.rewardDescription, 120) || "1 lavagem gratis",
  };
}

function reservationEarnsLoyaltyStamp(reservation = {}) {
  if (reservation.loyaltyRewardApplied === true) return false;
  const paymentStatus = normalizePaymentStatus(reservation.paymentStatus);
  return ![
    "covered_by_loyalty",
    "loyalty",
    "reward",
    "recompensa",
    "failed",
    "declined",
    "falhou",
    "refused",
    "recusado",
    "refunded",
    "refund",
    "reembolsado",
    "cancelled",
    "canceled",
    "cancelado",
  ].includes(paymentStatus);
}

function adminReservationExpectedStatuses(action) {
  switch (cleanString(action, 40)) {
    case "accept":
      return ["pending"];
    case "reject":
      return ["pending", "confirmed"];
    case "start":
      return ["confirmed"];
    case "complete":
      return ["in_progress"];
    default:
      return [];
  }
}

function normalizePaymentStatus(value) {
  return String(value || "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[\s-]+/g, "_");
}

function capacityForDate(date, config) {
  const override = (config.capacityOverrides || []).find((item) => item.date === date);
  if (override) return override.maxBookingsPerSlot;
  return config.capacityPerSlot;
}

function buildDaySlots(dateId, opening, slotInterval, durationMinutes, config, reservations) {
  const range = parseTimeRange(opening.hoursLabel);
  if (!range) return [];
  const slots = [];
  const latestStart = range.endMinutes - durationMinutes;
  const capacityPerSlot = capacityForDate(dateId, config);
  const blockedRanges = (config.blockedSlots || []).filter((slot) => slot.date === dateId);
  for (let minute = range.startMinutes; minute <= latestStart; minute += slotInterval) {
    const time = toTimeLabel(minute);
    const slotStart = `${dateId}T${time}:00.000Z`;
    const slotEnd = `${dateId}T${toTimeLabel(minute + durationMinutes)}:00.000Z`;
    const used = reservations.get(slotStart) || 0;
    const blocked = blockedRanges.some((slot) => slotOverlapsBlockedRange(slotStart, slotEnd, slot));
    const remainingCapacity = blocked ? 0 : Math.max(0, capacityPerSlot - used);
    slots.push({
      time,
      available: !blocked && remainingCapacity > 0,
      remainingCapacity,
    });
  }
  return slots;
}

function slotOverlapsBlockedRange(slotStart, slotEnd, blockedSlot) {
  const blockedStart = cleanString(blockedSlot && blockedSlot.slotStart, 80);
  const blockedEnd = cleanString(blockedSlot && blockedSlot.slotEnd, 80);
  if (!blockedStart || !blockedEnd) return false;
  const end = slotEnd && slotEnd > slotStart ? slotEnd : slotStart;
  return slotStart < blockedEnd && end > blockedStart;
}

function parseTimeRange(value) {
  const match = cleanString(value, 80).match(/^(\d{2}):(\d{2})\s*-\s*(\d{2}):(\d{2})$/);
  if (!match) return null;
  const startMinutes = Number(match[1]) * 60 + Number(match[2]);
  const endMinutes = Number(match[3]) * 60 + Number(match[4]);
  return endMinutes > startMinutes ? { startMinutes, endMinutes } : null;
}

function toTimeLabel(totalMinutes) {
  const hour = Math.floor(totalMinutes / 60);
  const minute = totalMinutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

module.exports = {
  adminReservationExpectedStatuses,
  buildDaySlots,
  capacityForDate,
  normalizeLoyalty,
  reservationEarnsLoyaltyStamp,
  slotOverlapsBlockedRange,
};
