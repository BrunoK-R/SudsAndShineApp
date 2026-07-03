"use strict";

function tokenPreferencePatch(preferences = {}) {
  return {
    bookingStatusEnabled: preferences.bookingStatusEnabled === true,
    appointmentReminderEnabled: preferences.appointmentReminderEnabled === true,
    loyaltyEnabled: preferences.loyaltyEnabled === true,
    adminPendingAlertEnabled: preferences.adminPendingAlertEnabled === true,
    marketingEnabled: preferences.marketingEnabled === true,
  };
}

function userProfilePreferencePatch(preferences = {}) {
  return {
    appointmentReminderOptIn: preferences.appointmentReminderEnabled === true,
    marketingOptIn: preferences.marketingEnabled === true,
  };
}

function tokenAllowsPreference(token = {}, preferenceKey, userPreferences = {}) {
  if (!preferenceKey) return true;
  if (typeof token[preferenceKey] === "boolean") {
    return token[preferenceKey] === true;
  }
  if (typeof userPreferences[preferenceKey] === "boolean") {
    return userPreferences[preferenceKey] === true;
  }
  return preferenceKey !== "marketingEnabled";
}

module.exports = {
  tokenPreferencePatch,
  tokenAllowsPreference,
  userProfilePreferencePatch,
};
