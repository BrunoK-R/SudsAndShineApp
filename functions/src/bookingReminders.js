"use strict";

const { canonicalStatus, reservationOwnerUid } = require("./notificationLifecycle");

const defaultReminderLeadMinutes = 120;
const defaultClaimTtlMinutes = 20;

function reminderWindow(now = new Date(), leadMinutes = defaultReminderLeadMinutes) {
  const start = toDate(now);
  const safeLead = clampMinutes(leadMinutes, 5, 24 * 60, defaultReminderLeadMinutes);
  return {
    start,
    end: new Date(start.getTime() + safeLead * 60 * 1000),
    startIso: start.toISOString(),
    endIso: new Date(start.getTime() + safeLead * 60 * 1000).toISOString(),
    leadMinutes: safeLead,
  };
}

function reservationNeedsReminder(reservation, now = new Date(), leadMinutes = defaultReminderLeadMinutes) {
  if (!reservation || canonicalStatus(reservation.status) !== "confirmed") return false;
  if (!reservationOwnerUid(reservation)) return false;
  if (parseDate(reservation.reminderSentAt || reservation.reminderLastSentAtIso)) return false;

  const slotStart = reservationSlotStartDate(reservation);
  if (!slotStart) return false;

  const window = reminderWindow(now, leadMinutes);
  return slotStart.getTime() > window.start.getTime() && slotStart.getTime() <= window.end.getTime();
}

function reminderClaimIsActive(reservation, now = new Date(), ttlMinutes = defaultClaimTtlMinutes) {
  const claimedAt = parseDate(reservation && (reservation.reminderClaimedAt || reservation.reminderClaimedAtIso));
  if (!claimedAt) return false;
  const ttl = clampMinutes(ttlMinutes, 1, 24 * 60, defaultClaimTtlMinutes) * 60 * 1000;
  return toDate(now).getTime() - claimedAt.getTime() < ttl;
}

function reminderDedupeKey(reservation) {
  const reservationId = cleanString(reservation && (reservation.id || reservation.reservationId), 160);
  const slotStart = reservationSlotStartDate(reservation);
  return cleanString(`booking_reminder:${reservationId}:${slotStart ? slotStart.toISOString() : ""}`, 260);
}

function isWithinQuietHours(now = new Date(), settings = {}) {
  const start = parseTime(settings.quietHoursStart);
  const end = parseTime(settings.quietHoursEnd);
  if (!start || !end) return false;

  const current = localMinutes(now, settings.quietHoursTimeZone);
  if (current == null) return false;

  if (start.minutes === end.minutes) return false;
  if (start.minutes < end.minutes) {
    return current >= start.minutes && current < end.minutes;
  }
  return current >= start.minutes || current < end.minutes;
}

function reservationSlotStartDate(reservation) {
  return parseDate(reservation && (reservation.slotStart || reservation.slotStartIso));
}

function localMinutes(value, timeZone) {
  try {
    const parts = new Intl.DateTimeFormat("en-GB", {
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
      timeZone: cleanString(timeZone, 80) || "UTC",
    }).formatToParts(toDate(value));
    const hour = Number(parts.find((part) => part.type === "hour")?.value);
    const minute = Number(parts.find((part) => part.type === "minute")?.value);
    if (!Number.isFinite(hour) || !Number.isFinite(minute)) return null;
    return hour * 60 + minute;
  } catch (_) {
    return null;
  }
}

function parseTime(value) {
  const match = /^(\d{2}):(\d{2})$/.exec(cleanString(value, 5));
  if (!match) return null;
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (hour > 23 || minute > 59) return null;
  return { hour, minute, minutes: hour * 60 + minute };
}

function parseDate(value) {
  if (!value) return null;
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;
  if (typeof value.toDate === "function") return parseDate(value.toDate());
  if (typeof value === "object" && Number.isFinite(value.seconds)) {
    return new Date(value.seconds * 1000 + Math.floor(Number(value.nanoseconds || 0) / 1000000));
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function toDate(value) {
  return parseDate(value) || new Date();
}

function clampMinutes(value, min, max, fallback) {
  const number = Number(value);
  if (!Number.isFinite(number)) return fallback;
  return Math.max(min, Math.min(max, Math.floor(number)));
}

function cleanString(value, maxLength) {
  return String(value || "")
    .trim()
    .replace(/\s+/g, " ")
    .slice(0, maxLength);
}

module.exports = {
  defaultClaimTtlMinutes,
  defaultReminderLeadMinutes,
  isWithinQuietHours,
  reminderClaimIsActive,
  reminderDedupeKey,
  reminderWindow,
  reservationNeedsReminder,
  reservationSlotStartDate,
};
