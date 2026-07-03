"use strict";

const bookingStatusTemplates = new Set([
  "booking_request",
  "booking_accepted",
  "booking_rejected",
  "booking_in_progress",
  "booking_completed",
  "booking_expired",
  "booking_cancelled",
  "booking_rescheduled",
  "booking_reminder",
  "review_prompt",
]);

const defaultTemplates = [
  {
    key: "booking_request",
    label: "Pedido recebido",
    enabled: true,
    title: "Pedido de marcacao recebido",
    body: "Recebemos o seu pedido {{reservationCode}} e vamos confirmar a disponibilidade.",
  },
  {
    key: "booking_accepted",
    label: "Marcacao aceite",
    enabled: true,
    title: "Marcacao confirmada",
    body: "A sua marcacao {{reservationCode}} foi confirmada. Ate breve!",
  },
  {
    key: "booking_rejected",
    label: "Marcacao rejeitada",
    enabled: true,
    title: "Nao foi possivel confirmar a marcacao",
    body: "Nao conseguimos confirmar a marcacao {{reservationCode}}. Consulte os detalhes na app.",
  },
  {
    key: "booking_in_progress",
    label: "Lavagem iniciada",
    enabled: true,
    title: "A sua lavagem comecou",
    body: "A marcacao {{reservationCode}} esta agora a decorrer.",
  },
  {
    key: "booking_completed",
    label: "Lavagem concluida",
    enabled: true,
    title: "Lavagem concluida",
    body: "A lavagem {{reservationCode}} foi concluida. Consulte o historico na app.",
  },
  {
    key: "booking_expired",
    label: "Pedido expirado",
    enabled: true,
    title: "Pedido de marcacao expirado",
    body: "O pedido {{reservationCode}} expirou antes da confirmacao.",
  },
  {
    key: "booking_cancelled",
    label: "Marcacao cancelada",
    enabled: true,
    title: "Marcacao cancelada",
    body: "A sua marcacao {{reservationCode}} foi cancelada.",
  },
  {
    key: "booking_rescheduled",
    label: "Marcacao remarcada",
    enabled: true,
    title: "Marcacao remarcada",
    body: "A sua marcacao {{reservationCode}} foi remarcada para {{slotStart}}.",
  },
  {
    key: "booking_reminder",
    label: "Lembrete de marcacao",
    enabled: true,
    title: "A sua lavagem esta quase a chegar",
    body: "Tem uma marcacao em breve.",
  },
  {
    key: "review_prompt",
    label: "Pedido de avaliacao",
    enabled: true,
    title: "Como correu a lavagem?",
    body: "Avalie o servico para nos ajudar a melhorar.",
  },
  {
    key: "loyalty_reward",
    label: "Recompensa de fidelizacao",
    enabled: true,
    title: "Recompensa disponivel",
    body: "A sua recompensa {{rewardDescription}} esta pronta. Use-a na proxima marcacao.",
  },
  {
    key: "admin_pending_booking",
    label: "Alerta admin de pedido",
    enabled: true,
    title: "Novo pedido de lavagem",
    body: "{{customerName}} pediu {{serviceName}} para {{slotStart}}.",
  },
];

const defaultNotificationSettings = {
  bookingStatusEnabled: true,
  appointmentReminderEnabled: true,
  loyaltyEnabled: true,
  adminPendingAlertEnabled: true,
  marketingEnabled: false,
  reminderLeadMinutes: 120,
  quietHoursStart: "22:00",
  quietHoursEnd: "08:00",
  quietHoursTimeZone: "Europe/Lisbon",
  templates: defaultTemplates,
};

const defaultUserPreferences = {
  bookingStatusEnabled: true,
  appointmentReminderEnabled: true,
  loyaltyEnabled: true,
  adminPendingAlertEnabled: true,
  marketingEnabled: false,
};

function normalizeStatus(value) {
  return String(value || "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[\s-]+/g, "_");
}

function canonicalStatus(value) {
  switch (normalizeStatus(value)) {
    case "novo":
    case "new":
    case "pending":
      return "pending";
    case "confirmado":
    case "accepted":
    case "aceite":
    case "aceita":
    case "aprovado":
    case "aprovada":
    case "confirmed":
      return "confirmed";
    case "a_decorrer":
    case "decorrer":
    case "em_execucao":
    case "em_curso":
    case "in_progress":
    case "running":
    case "started":
      return "in_progress";
    case "complete":
    case "completed":
    case "done":
    case "concluido":
    case "concluida":
    case "finalizado":
    case "finalizada":
      return "completed";
    case "cancelled":
    case "canceled":
    case "cancelado":
    case "cancelada":
      return "cancelled";
    case "rejected":
    case "rejeitado":
    case "rejeitada":
    case "recusado":
    case "recusada":
      return "rejected";
    case "expired":
    case "expirado":
    case "expirada":
      return "expired";
    default:
      return "";
  }
}

function reservationOwnerUid(reservation) {
  return cleanString(
    reservation && (
      reservation.userUid ||
      reservation.customerUid ||
      reservation.uid ||
      reservation.createdByUid
    ),
    160,
  );
}

function reservationCode(reservation) {
  return cleanString(
    reservation && (reservation.reservationCode || reservation.code || reservation.id),
    80,
  );
}

function cleanString(value, maxLength) {
  return String(value || "")
    .trim()
    .replace(/\s+/g, " ")
    .slice(0, maxLength);
}

function pickTemplate(settings, key) {
  const templates = Array.isArray(settings && settings.templates)
    ? settings.templates
    : defaultTemplates;
  const template = templates.find((item) => item && item.key === key) ||
    defaultTemplates.find((item) => item.key === key);
  if (!template || template.enabled === false) return null;
  return template;
}

function notificationAllowed(settings, key) {
  if (key === "admin_pending_booking") return settings.adminPendingAlertEnabled !== false;
  if (key === "loyalty_reward") return settings.loyaltyEnabled !== false;
  if (key === "booking_reminder") return settings.appointmentReminderEnabled !== false;
  if (bookingStatusTemplates.has(key)) return settings.bookingStatusEnabled !== false;
  return true;
}

function statusTemplateFor(status) {
  switch (canonicalStatus(status)) {
    case "confirmed":
      return "booking_accepted";
    case "rejected":
      return "booking_rejected";
    case "in_progress":
      return "booking_in_progress";
    case "completed":
      return "booking_completed";
    case "cancelled":
      return "booking_cancelled";
    case "expired":
      return "booking_expired";
    default:
      return "";
  }
}

function reservationLifecycleEvents(before, after) {
  const events = [];
  const previous = before || null;
  const current = after || {};
  const currentStatus = canonicalStatus(current.status);

  if (!previous) {
    if (!currentStatus || currentStatus === "pending") {
      events.push({ target: "customer", templateKey: "booking_request" });
      events.push({ target: "admins", templateKey: "admin_pending_booking" });
    }
    return events;
  }

  const previousStatus = canonicalStatus(previous.status);
  const wasSlotChanged = slotChanged(previous, current);
  if (currentStatus && currentStatus !== previousStatus) {
    const statusTemplate = statusTemplateFor(currentStatus);
    if (statusTemplate) {
      events.push({ target: "customer", templateKey: statusTemplate });
    }
    if (currentStatus === "completed") {
      events.push({ target: "customer", templateKey: "review_prompt" });
    }
    if (currentStatus === "pending") {
      events.push({ target: "admins", templateKey: "admin_pending_booking" });
    }
  }

  if (wasSlotChanged) {
    events.push({ target: "customer", templateKey: "booking_rescheduled" });
  }

  if (becameRewardReady(previous, current)) {
    events.push({ target: "customer", templateKey: "loyalty_reward" });
  }

  return dedupeEvents(events);
}

function slotChanged(previous, current) {
  const beforeStart = previous.slotStart || previous.slotStartIso || "";
  const afterStart = current.slotStart || current.slotStartIso || "";
  const beforeEnd = previous.slotEnd || previous.slotEndIso || "";
  const afterEnd = current.slotEnd || current.slotEndIso || "";
  return Boolean(afterStart && beforeStart && afterStart !== beforeStart) ||
    Boolean(afterEnd && beforeEnd && afterEnd !== beforeEnd);
}

function becameRewardReady(previous, current) {
  const before = loyaltySummary(previous);
  const after = loyaltySummary(current);
  if (!after) return false;
  const beforeReady = Boolean(before && before.rewardReady) || Number(before && before.availableRewards || 0) > 0;
  const afterReady = Boolean(after.rewardReady) || Number(after.availableRewards || 0) > 0;
  return afterReady && !beforeReady;
}

function loyaltySummary(reservation) {
  if (!reservation) return null;
  return reservation.loyalty || reservation.loyaltySummary || null;
}

function dedupeEvents(events) {
  const seen = new Set();
  return events.filter((event) => {
    const key = `${event.target}:${event.templateKey}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function renderTemplate(value, reservation, extra) {
  const vars = notificationVariables(reservation, extra);
  return cleanString(value, 512).replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g, (_, key) => vars[key] || "");
}

function notificationVariables(reservation, extra) {
  const source = reservation || {};
  const loyalty = loyaltySummary(source) || {};
  return {
    reservationId: cleanString(source.id || source.reservationId, 160),
    reservationCode: reservationCode(source),
    customerName: cleanString(source.customerName || source.displayName, 120),
    serviceName: cleanString(source.serviceName, 120),
    slotStart: cleanString(source.slotStart || source.slotStartIso, 80),
    slotEnd: cleanString(source.slotEnd || source.slotEndIso, 80),
    rewardCode: cleanString(source.loyaltyRewardCode || loyalty.rewardCode, 80),
    rewardDescription: cleanString(
      (extra && extra.rewardDescription) ||
        source.rewardDescription ||
        loyalty.rewardDescription ||
        "1 lavagem gratis",
      120,
    ),
  };
}

function buildNotificationMessage(template, reservation, extra) {
  const templateKey = template.key;
  const type = templateKey === "admin_pending_booking"
    ? "admin_pending_booking"
    : templateKey === "loyalty_reward"
      ? "loyalty_reward"
      : "booking_status";
  const reservationId = cleanString(
    reservation && (reservation.id || reservation.reservationId),
    160,
  );
  const code = reservationCode(reservation);
  return {
    notification: {
      title: renderTemplate(template.title, reservation, extra),
      body: renderTemplate(template.body, reservation, extra),
    },
    data: removeBlankValues({
      type,
      templateKey,
      reservationId,
      reservationCode: code,
      redemptionId: cleanString(extra && extra.redemptionId, 160),
      dedupeKey: cleanString(`${templateKey}:${reservationId || code}`, 220),
      source: "functions",
    }),
  };
}

function removeBlankValues(values) {
  return Object.fromEntries(
    Object.entries(values).filter(([, value]) => String(value || "").trim().length > 0),
  );
}

module.exports = {
  defaultNotificationSettings,
  defaultTemplates,
  defaultUserPreferences,
  normalizeStatus,
  canonicalStatus,
  reservationOwnerUid,
  reservationLifecycleEvents,
  notificationAllowed,
  pickTemplate,
  buildNotificationMessage,
};
