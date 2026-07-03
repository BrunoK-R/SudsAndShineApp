"use strict";

const invalidTokenErrorCodes = new Set([
  "messaging/invalid-registration-token",
  "messaging/registration-token-not-registered",
]);

const androidNotificationChannelId = "suds_notifications";
const androidNotificationClickAction = "org.sudsmobile.app.NOTIFICATION_OPEN";

function buildMulticastMessage(tokens, message) {
  return {
    tokens,
    notification: message && message.notification,
    data: normalizeFcmData(message && message.data),
    android: {
      notification: {
        channelId: androidNotificationChannelId,
        clickAction: androidNotificationClickAction,
      },
    },
    apns: {
      payload: {
        aps: {
          sound: "default",
        },
      },
    },
  };
}

function normalizeTokenRecords(items) {
  const byToken = new Map();
  (items || []).forEach((item) => {
    const record = normalizeTokenRecord(item);
    if (!record || byToken.has(record.token)) return;
    byToken.set(record.token, record);
  });
  return [...byToken.values()];
}

function normalizeTokenRecord(item) {
  if (typeof item === "string") {
    const token = cleanString(item, 4096);
    return token ? { token } : null;
  }
  if (!item || typeof item !== "object") return null;
  const token = cleanString(item.token, 4096);
  if (!token) return null;
  return {
    token,
    uid: cleanString(item.uid, 160),
    tokenId: cleanString(item.tokenId, 160),
    refPath: cleanString(item.refPath, 512),
  };
}

function invalidTokenRecordsForBatch(records, response) {
  const responses = Array.isArray(response && response.responses) ? response.responses : [];
  return responses
    .map((item, index) => ({ item, record: records[index] }))
    .filter(({ item, record }) => record && item && item.success !== true && isInvalidTokenError(item.error))
    .map(({ record }) => record);
}

function isInvalidTokenError(error) {
  const code = cleanString(
    error && (error.code || error.errorInfo && error.errorInfo.code),
    120,
  );
  return invalidTokenErrorCodes.has(code);
}

function normalizeFcmData(data) {
  return Object.fromEntries(
    Object.entries(data || {})
      .map(([key, value]) => [
        cleanString(key, 128),
        cleanDataValue(value, 4096),
      ])
      .filter(([key, value]) => key && value),
  );
}

function cleanDataValue(value, maxLength) {
  if (value === null || value === undefined) return "";
  return String(value)
    .trim()
    .replace(/\s+/g, " ")
    .slice(0, maxLength);
}

function cleanString(value, maxLength) {
  return String(value || "")
    .trim()
    .replace(/\s+/g, " ")
    .slice(0, maxLength);
}

module.exports = {
  androidNotificationChannelId,
  androidNotificationClickAction,
  buildMulticastMessage,
  invalidTokenRecordsForBatch,
  isInvalidTokenError,
  normalizeFcmData,
  normalizeTokenRecords,
};
