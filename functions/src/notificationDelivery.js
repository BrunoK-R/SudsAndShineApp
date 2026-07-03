"use strict";

const {
  buildNotificationMessage,
  notificationAllowed,
  pickTemplate,
  reservationLifecycleEvents,
  reservationOwnerUid,
} = require("./notificationLifecycle");

async function deliverReservationEvents(options) {
  const {
    before,
    after,
    settings,
    getAdminTokens,
    getUserTokens,
    sendToTokens,
    logDelivery,
  } = options;
  const events = reservationLifecycleEvents(before, after);
  if (events.length === 0) return [];

  const deliveries = [];
  for (const lifecycleEvent of events) {
    if (!notificationAllowed(settings, lifecycleEvent.templateKey)) continue;
    const template = pickTemplate(settings, lifecycleEvent.templateKey);
    if (!template) continue;

    const preferenceKey = preferenceGateForTemplate(lifecycleEvent.templateKey);
    const tokens = lifecycleEvent.target === "admins"
      ? await getAdminTokens(preferenceKey)
      : await getUserTokens(reservationOwnerUid(after), preferenceKey);
    const message = buildNotificationMessage(template, after);
    const sent = await sendToTokens(tokens, message);
    const delivery = {
      event: lifecycleEvent,
      preferenceKey,
      tokenCount: tokens.length,
      sent,
      message,
    };
    deliveries.push(delivery);
    if (logDelivery) {
      await logDelivery(lifecycleEvent, after, sent);
    }
  }

  return deliveries;
}

function preferenceGateForTemplate(templateKey) {
  if (templateKey === "admin_pending_booking") return "adminPendingAlertEnabled";
  if (templateKey === "loyalty_reward") return "loyaltyEnabled";
  if (templateKey === "booking_reminder") return "appointmentReminderEnabled";
  return "bookingStatusEnabled";
}

module.exports = {
  deliverReservationEvents,
  preferenceGateForTemplate,
};
