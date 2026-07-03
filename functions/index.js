"use strict";

const crypto = require("crypto");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { setGlobalOptions } = require("firebase-functions/v2");
const {
  adminReservationExpectedStatuses,
  buildDaySlots,
  capacityForDate,
  normalizeLoyalty,
  reservationEarnsLoyaltyStamp,
  slotOverlapsBlockedRange,
} = require("./src/bookingDomain");
const {
  buildNotificationMessage,
  canonicalStatus,
  defaultNotificationSettings,
  defaultUserPreferences,
  notificationAllowed,
  pickTemplate,
  reservationOwnerUid,
} = require("./src/notificationLifecycle");
const {
  deliverReservationEvents: deliverReservationNotifications,
  preferenceGateForTemplate,
} = require("./src/notificationDelivery");
const {
  buildMulticastMessage,
  invalidTokenRecordsForBatch,
  normalizeTokenRecords,
} = require("./src/fcmDelivery");
const {
  isWithinQuietHours,
  reminderClaimIsActive,
  reminderDedupeKey,
  reminderWindow,
  reservationNeedsReminder,
} = require("./src/bookingReminders");
const {
  adminRoleDocumentPatch,
} = require("./src/adminRole");
const {
  deliveryStateForSend,
  selfTestDeliveryMessage,
} = require("./src/notificationTestReceipt");
const {
  tokenAllowsPreference,
  tokenPreferencePatch,
  userProfilePreferencePatch,
} = require("./src/notificationPreferences");

initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

const db = getFirestore();
const messaging = getMessaging();

exports.getServiceCatalog = onCall(async () => {
  return await getPublicServiceCatalog();
});

exports.getBusinessInfo = onCall(async () => {
  return await getPublicBusinessInfo();
});

exports.getAvailability = onCall(async (request) => {
  const availabilityRequest = sanitizeAvailabilityRequest(request.data || {});
  return await buildAvailabilityMonth(availabilityRequest);
});

exports.getMyNotificationPreferences = onCall(async (request) => {
  const uid = requireUid(request);
  return { preferences: await getUserPreferences(uid) };
});

exports.updateMyNotificationPreferences = onCall(async (request) => {
  const uid = requireUid(request);
  const preferences = sanitizePreferences(request.data || {});
  await saveUserPreferences(uid, preferences);
  return { preferences };
});

exports.registerNotificationToken = onCall(async (request) => {
  const uid = requireUid(request);
  const data = request.data || {};
  const token = cleanString(data.token, 4096);
  const platform = cleanString(data.platform, 32);
  if (!token || !["android", "ios", "web"].includes(platform)) {
    throw new HttpsError("invalid-argument", "Token de notificacoes invalido.");
  }

  const tokenId = cleanTokenId(data.tokenId) || hashToken(token);
  const preferences = await getUserPreferences(uid);
  const role = await roleForUser(uid, request.auth && request.auth.token);
  if (role === "admin") {
    await syncAdminRoleDocument(uid, role, request.auth && request.auth.token);
  }
  await db.doc(`users/${uid}/notificationTokens/${tokenId}`).set({
    token,
    tokenId,
    platform,
    enabled: true,
    deviceLabel: cleanString(data.deviceLabel, 120),
    appVersion: cleanString(data.appVersion, 80),
    ...tokenPreferencePatch(preferences),
    updatedAt: FieldValue.serverTimestamp(),
    createdAt: FieldValue.serverTimestamp(),
  }, { merge: true });

  return {
    token: {
      tokenId,
      platform,
      enabled: true,
    },
  };
});

exports.deleteNotificationToken = onCall(async (request) => {
  const uid = requireUid(request);
  const tokenId = cleanTokenId((request.data || {}).tokenId);
  if (!tokenId) {
    throw new HttpsError("invalid-argument", "Token de notificacoes invalido.");
  }

  await db.doc(`users/${uid}/notificationTokens/${tokenId}`).set({
    enabled: false,
    deletedAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });

  return { tokenId, status: "revoked" };
});

exports.getAdminNotificationSettings = onCall(async (request) => {
  await requireAdmin(request);
  return await getNotificationSettings();
});

exports.updateNotificationSettings = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const settings = sanitizeNotificationSettings(request.data || {});
  await db.doc("adminConfig/notificationSettings").set({
    ...settings,
    source: "firestore",
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    ...settings,
    source: "firestore",
    updatedAtIso: new Date().toISOString(),
    updatedByUid: admin.uid,
  };
});

exports.sendAdminNotificationTest = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const data = request.data || {};
  const templateKey = cleanString(data.templateKey, 80);
  const campaignId = cleanDocumentId(data.campaignId, 160);
  if (templateKey && campaignId) {
    throw new HttpsError("invalid-argument", "Escolha um modelo ou uma campanha.");
  }
  if (campaignId) {
    return await sendCampaignDraftTestToSelf(admin, campaignId);
  }

  const settings = await getNotificationSettings();
  if (!templateKey || !notificationAllowed(settings, templateKey)) {
    throw new HttpsError("invalid-argument", "Modelo de notificacao invalido.");
  }
  const template = pickTemplate(settings, templateKey);
  if (!template) {
    throw new HttpsError("failed-precondition", "Modelo de notificacao desligado.");
  }

  const tokens = await getUserEnabledTokens(admin.uid);
  const message = buildNotificationMessage(template, {
    id: "test",
    reservationId: "test",
    reservationCode: "TEST",
    customerName: admin.email || "Administrador",
    serviceName: "Teste",
    slotStart: new Date().toISOString(),
  });
  const sent = await sendToTokens(tokens, message);
  const sentCount = Number(sent.sent) || 0;
  const failedCount = Number(sent.failed) || 0;
  const invalidatedCount = Number(sent.invalidated) || 0;
  const deliveryState = sentCount > 0
    ? "sent"
    : failedCount > 0
      ? "failed"
      : "no_recipients";
  return {
    notificationId: `test-${Date.now()}`,
    templateKey,
    campaignId: "",
    deliveryState,
    recipientUid: admin.uid,
    targetScope: "self",
    testOnly: true,
    tokenCount: tokens.length,
    sentCount,
    failedCount,
    invalidatedCount,
    message: sentCount > 0
      ? "Teste enviado apenas para o administrador atual."
      : tokens.length === 0
        ? "Este administrador ainda nao tem um dispositivo ativo para notificacoes."
        : "Nao foi possivel entregar o teste ao dispositivo atual.",
  };
});

exports.getAdminNotificationCampaignDrafts = onCall(async (request) => {
  await requireAdmin(request);
  return await getNotificationCampaignDraftsPayload();
});

exports.upsertAdminNotificationCampaignDraft = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const draft = sanitizeNotificationCampaignDraft(request.data || {});
  const doc = draft.campaignId
    ? db.doc(`notificationCampaignDrafts/${draft.campaignId}`)
    : db.collection("notificationCampaignDrafts").doc();
  const existing = await doc.get();
  if (existing.exists && ["sent", "archived"].includes(cleanString(existing.get("status"), 40))) {
    throw new HttpsError("failed-precondition", "Esta campanha ja nao pode ser editada.");
  }
  await doc.set({
    campaignId: doc.id,
    title: draft.title,
    body: draft.body,
    targetAudience: draft.targetAudience,
    channels: draft.channels,
    marketingConsentRequired: draft.marketingConsentRequired,
    status: "draft",
    scheduledAtIso: draft.scheduledAtIso,
    notes: draft.notes,
    sendBlocked: false,
    sendBlockedReason: "",
    deliveryLocked: false,
    sendState: "ready",
    createdAt: existing.exists ? existing.get("createdAt") || FieldValue.serverTimestamp() : FieldValue.serverTimestamp(),
    createdByUid: existing.exists ? existing.get("createdByUid") || admin.uid : admin.uid,
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    campaignId: doc.id,
    status: "draft",
    created: !existing.exists,
    targetAudience: draft.targetAudience,
    sendBlocked: false,
    sendBlockedReason: "",
    deliveryLocked: false,
    sendState: "ready",
  };
});

exports.archiveAdminNotificationCampaignDraft = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const campaignId = cleanDocumentId((request.data || {}).campaignId, 160);
  if (!campaignId) {
    throw new HttpsError("invalid-argument", "Campanha invalida.");
  }
  await db.doc(`notificationCampaignDrafts/${campaignId}`).set({
    status: "archived",
    sendBlocked: true,
    sendBlockedReason: "campaign-archived",
    deliveryLocked: true,
    sendState: "archived",
    archivedAt: FieldValue.serverTimestamp(),
    archivedByUid: admin.uid,
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    campaignId,
    status: "archived",
    created: false,
    targetAudience: "",
    sendBlocked: true,
    sendBlockedReason: "campaign-archived",
    deliveryLocked: true,
    sendState: "archived",
  };
});

exports.broadcastAdminNotificationCampaign = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const data = request.data || {};
  const campaignId = cleanDocumentId(data.campaignId, 160);
  if (!campaignId || data.confirmBroadcast !== true) {
    throw new HttpsError("invalid-argument", "Confirme a campanha antes de enviar.");
  }

  const doc = await db.doc(`notificationCampaignDrafts/${campaignId}`).get();
  if (!doc.exists) {
    throw new HttpsError("not-found", "Campanha nao encontrada.");
  }
  const campaign = toNotificationCampaignDraftPayload(doc.id, doc.data());
  if (!campaign || ["sent", "archived"].includes(campaign.status)) {
    throw new HttpsError("failed-precondition", "Esta campanha ja nao pode ser enviada.");
  }

  const settings = await getNotificationSettings();
  if (!settings.marketingEnabled) {
    await doc.ref.set({
      sendBlocked: true,
      sendBlockedReason: "marketing-disabled",
      sendState: "blocked",
      updatedAt: FieldValue.serverTimestamp(),
      updatedByUid: admin.uid,
    }, { merge: true });
    return {
      campaignId,
      status: "draft",
      targetAudience: campaign.targetAudience,
      queuedCount: 0,
      skippedCount: 0,
      sentByUid: admin.uid,
      sendBlocked: true,
      sendBlockedReason: "marketing-disabled",
      deliveryLocked: false,
      sendState: "blocked",
    };
  }

  const tokens = await getCampaignRecipientTokens(campaign.targetAudience, campaign.marketingConsentRequired);
  const sent = await sendToTokens(tokens, {
    notification: {
      title: campaign.title,
      body: campaign.body,
    },
    data: {
      type: "marketing_campaign",
      campaignId,
      route: "profile",
    },
  });
  await doc.ref.set({
    status: "sent",
    sentAt: FieldValue.serverTimestamp(),
    sentByUid: admin.uid,
    queuedCount: sent.sent,
    skippedCount: Math.max(0, tokens.length - sent.sent),
    sendBlocked: true,
    sendBlockedReason: "campaign-already-sent",
    deliveryLocked: true,
    sendState: "sent",
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });

  return {
    campaignId,
    status: "sent",
    targetAudience: campaign.targetAudience,
    queuedCount: sent.sent,
    skippedCount: Math.max(0, tokens.length - sent.sent),
    sentByUid: admin.uid,
    sendBlocked: true,
    sendBlockedReason: "campaign-already-sent",
    deliveryLocked: true,
    sendState: "sent",
  };
});

exports.getAdminBusinessInfo = onCall(async (request) => {
  await requireAdmin(request);
  return await getAdminBusinessInfoConfig();
});

exports.getAdminAvailabilityConfiguration = onCall(async (request) => {
  await requireAdmin(request);
  return await getAdminAvailabilityConfig();
});

exports.getAdminBookingPolicy = onCall(async (request) => {
  await requireAdmin(request);
  return await getAdminBookingPolicyConfig();
});

exports.getAdminLoyaltySettings = onCall(async (request) => {
  await requireAdmin(request);
  return await getAdminLoyaltySettingsConfig();
});

exports.getAdminServiceCatalog = onCall(async (request) => {
  await requireAdmin(request);
  return { services: await getAdminServiceCatalogItems() };
});

exports.getAdminServiceExtras = onCall(async (request) => {
  await requireAdmin(request);
  return { extras: await getAdminServiceExtraItems() };
});

exports.updateBusinessInfo = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const config = sanitizeBusinessInfoUpdate(request.data || {});
  await db.doc("adminConfig/businessInfo").set({
    ...config,
    source: "firestore",
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return await getAdminBusinessInfoConfig({ updatedByUid: admin.uid });
});

exports.updateAvailabilityConfiguration = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const config = sanitizeAvailabilityUpdate(request.data || {});
  await db.doc("adminConfig/availability").set({
    ...config,
    capacityPerSlot: config.defaultMaxBookingsPerSlot,
    slotIntervalMinutes: config.defaultSlotIntervalMinutes,
    source: "firestore",
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return await getAdminAvailabilityConfig({ updatedByUid: admin.uid });
});

exports.updateBookingPolicy = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const config = sanitizeBookingPolicyUpdate(request.data || {});
  await db.doc("adminConfig/bookingPolicy").set({
    ...config,
    source: "firestore",
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return await getAdminBookingPolicyConfig({ updatedByUid: admin.uid });
});

exports.updateLoyaltySettings = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const config = sanitizeLoyaltySettingsUpdate(request.data || {});
  await db.doc("adminConfig/loyaltySettings").set({
    ...config,
    targetWashes: config.stampsRequired,
    source: "firestore",
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return await getAdminLoyaltySettingsConfig({ updatedByUid: admin.uid });
});

exports.upsertCapacityOverride = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const override = sanitizeCapacityOverrideUpsert(request.data || {});
  await db.doc(`adminConfig/availability/capacityOverrides/${override.date}`).set({
    ...override,
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    date: override.date,
    status: "saved",
    maxBookingsPerSlot: override.maxBookingsPerSlot,
  };
});

exports.clearCapacityOverride = onCall(async (request) => {
  await requireAdmin(request);
  const date = sanitizeDateArgument((request.data || {}).date);
  await db.doc(`adminConfig/availability/capacityOverrides/${date}`).delete();
  return {
    date,
    status: "cleared",
    maxBookingsPerSlot: null,
  };
});

exports.upsertBlockedSlot = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const slot = sanitizeBlockedSlotUpsert(request.data || {});
  const doc = slot.blockedSlotId
    ? db.doc(`adminConfig/availability/blockedSlots/${slot.blockedSlotId}`)
    : db.collection("adminConfig/availability/blockedSlots").doc();
  await doc.set({
    ...slot,
    blockedSlotId: doc.id,
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    blockedSlotId: doc.id,
    status: "saved",
    date: slot.date,
  };
});

exports.clearBlockedSlot = onCall(async (request) => {
  await requireAdmin(request);
  const blockedSlotId = cleanDocumentId((request.data || {}).blockedSlotId, 160);
  if (!blockedSlotId) {
    throw new HttpsError("invalid-argument", "Bloqueio invalido.");
  }
  await db.doc(`adminConfig/availability/blockedSlots/${blockedSlotId}`).delete();
  return {
    blockedSlotId,
    status: "cleared",
    date: "",
  };
});

exports.upsertServiceCatalogItem = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const item = sanitizeServiceCatalogUpsert(request.data || {});
  const doc = item.serviceId
    ? db.doc(`serviceCatalog/${item.serviceId}`)
    : db.collection("serviceCatalog").doc();
  const existing = await doc.get();
  await doc.set({
    id: doc.id,
    name: item.name,
    description: item.description,
    durationMinutes: item.durationMinutes,
    passengerPriceCents: item.passengerPriceCents,
    suvPriceCents: item.suvPriceCents,
    iconKey: item.iconKey,
    popular: item.popular,
    archived: !item.active,
    sortOrder: item.sortOrder,
    createdAt: existing.exists ? existing.get("createdAt") || FieldValue.serverTimestamp() : FieldValue.serverTimestamp(),
    createdByUid: existing.exists ? existing.get("createdByUid") || admin.uid : admin.uid,
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    serviceId: doc.id,
    status: "saved",
    created: !existing.exists,
  };
});

exports.archiveServiceCatalogItem = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const serviceId = cleanDocumentId((request.data || {}).serviceId, 160);
  if (!serviceId) {
    throw new HttpsError("invalid-argument", "Servico invalido.");
  }
  await db.doc(`serviceCatalog/${serviceId}`).set({
    archived: true,
    archivedAt: FieldValue.serverTimestamp(),
    archivedByUid: admin.uid,
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    serviceId,
    status: "archived",
    created: false,
  };
});

exports.upsertServiceExtra = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const item = sanitizeServiceExtraUpsert(request.data || {});
  const doc = item.extraId
    ? db.doc(`serviceExtras/${item.extraId}`)
    : db.collection("serviceExtras").doc();
  const existing = await doc.get();
  await doc.set({
    id: doc.id,
    name: item.name,
    description: item.description,
    priceCents: item.priceCents,
    iconKey: item.iconKey,
    eligibleServiceIds: item.eligibleServiceIds,
    archived: !item.active,
    sortOrder: item.sortOrder,
    createdAt: existing.exists ? existing.get("createdAt") || FieldValue.serverTimestamp() : FieldValue.serverTimestamp(),
    createdByUid: existing.exists ? existing.get("createdByUid") || admin.uid : admin.uid,
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    extraId: doc.id,
    status: "saved",
    created: !existing.exists,
  };
});

exports.archiveServiceExtra = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const extraId = cleanDocumentId((request.data || {}).extraId, 160);
  if (!extraId) {
    throw new HttpsError("invalid-argument", "Extra invalido.");
  }
  await db.doc(`serviceExtras/${extraId}`).set({
    archived: true,
    archivedAt: FieldValue.serverTimestamp(),
    archivedByUid: admin.uid,
    updatedAt: FieldValue.serverTimestamp(),
    updatedByUid: admin.uid,
  }, { merge: true });
  return {
    extraId,
    status: "archived",
    created: false,
  };
});

exports.syncMyRole = onCall(async (request) => {
  const uid = requireUid(request);
  const user = await getAuth().getUser(uid);
  const role = await roleForUser(uid, request.auth && request.auth.token);
  await syncAdminRoleDocument(uid, role, request.auth && request.auth.token, user);
  return {
    uid,
    email: user.email || "",
    role,
  };
});

exports.getMyProfile = onCall(async (request) => {
  const uid = requireUid(request);
  const user = await getAuth().getUser(uid);
  const doc = await db.doc(`users/${uid}`).get();
  return {
    profile: toProfilePayload(uid, user, doc.exists ? doc.data() : {}),
  };
});

exports.updateMyProfile = onCall(async (request) => {
  const uid = requireUid(request);
  const user = await getAuth().getUser(uid);
  const profile = sanitizeProfileUpdate(request.data || {});
  const patch = {
    ...profile,
    email: user.email || "",
    updatedAt: FieldValue.serverTimestamp(),
  };

  await db.doc(`users/${uid}`).set(patch, { merge: true });
  await syncProfileNotificationPreferenceBits(uid, profile);

  if (profile.displayName || profile.photoUrl) {
    await getAuth().updateUser(uid, {
      displayName: profile.displayName,
      photoURL: profile.photoUrl || null,
    });
  }

  const updatedUser = await getAuth().getUser(uid);
  const updatedDoc = await db.doc(`users/${uid}`).get();
  return {
    profile: toProfilePayload(uid, updatedUser, updatedDoc.exists ? updatedDoc.data() : {}),
  };
});

exports.getMyVehicles = onCall(async (request) => {
  const uid = requireUid(request);
  const snapshot = await db.collection(`users/${uid}/vehicles`).get();
  return {
    vehicles: snapshot.docs
      .map((doc) => toVehiclePayload(doc.id, doc.data()))
      .filter(Boolean)
      .sort(sortVehicles),
  };
});

exports.createVehicle = onCall(async (request) => {
  const uid = requireUid(request);
  const vehicle = sanitizeVehicleSave(request.data || {}, false);
  const vehicleRef = db.collection(`users/${uid}/vehicles`).doc();
  let savedVehicle = null;

  await db.runTransaction(async (transaction) => {
    const vehiclesQuery = db.collection(`users/${uid}/vehicles`);
    const snapshot = await transaction.get(vehiclesQuery);
    const shouldBeDefault = vehicle.isDefault || snapshot.empty;

    if (shouldBeDefault) {
      snapshot.docs.forEach((doc) => {
        transaction.set(doc.ref, {
          isDefault: false,
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      });
    }

    const { id: _id, ...vehiclePatch } = vehicle;
    savedVehicle = {
      ...vehiclePatch,
      isDefault: shouldBeDefault,
    };
    transaction.set(vehicleRef, {
      ...savedVehicle,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return {
    vehicle: toVehiclePayload(vehicleRef.id, savedVehicle),
  };
});

exports.updateVehicle = onCall(async (request) => {
  const uid = requireUid(request);
  const vehicle = sanitizeVehicleSave(request.data || {}, true);
  const vehicleRef = db.doc(`users/${uid}/vehicles/${vehicle.id}`);
  let savedVehicle = null;

  await db.runTransaction(async (transaction) => {
    const existingDoc = await transaction.get(vehicleRef);
    if (!existingDoc.exists) {
      throw new HttpsError("not-found", "Este veiculo ja nao existe.");
    }

    const vehiclesQuery = db.collection(`users/${uid}/vehicles`);
    const snapshot = await transaction.get(vehiclesQuery);
    if (vehicle.isDefault) {
      snapshot.docs.forEach((doc) => {
        if (doc.id === vehicle.id) return;
        transaction.set(doc.ref, {
          isDefault: false,
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      });
    }

    const { id: _id, ...vehiclePatch } = vehicle;
    savedVehicle = vehiclePatch;
    transaction.set(vehicleRef, {
      ...savedVehicle,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return {
    vehicle: toVehiclePayload(vehicle.id, savedVehicle),
  };
});

exports.deleteVehicle = onCall(async (request) => {
  const uid = requireUid(request);
  const vehicleId = cleanDocumentId((request.data || {}).vehicleId, 160);
  if (!vehicleId) {
    throw new HttpsError("invalid-argument", "Escolha um veiculo valido.");
  }

  await db.runTransaction(async (transaction) => {
    const vehicleRef = db.doc(`users/${uid}/vehicles/${vehicleId}`);
    const vehicleDoc = await transaction.get(vehicleRef);
    if (!vehicleDoc.exists) {
      throw new HttpsError("not-found", "Este veiculo ja nao existe.");
    }

    const vehiclesQuery = db.collection(`users/${uid}/vehicles`);
    const snapshot = await transaction.get(vehiclesQuery);
    const deletingDefault = vehicleDoc.get("isDefault") === true;
    transaction.delete(vehicleRef);

    if (deletingDefault) {
      const replacement = snapshot.docs
        .filter((doc) => doc.id !== vehicleId)
        .map((doc) => toVehiclePayload(doc.id, doc.data()))
        .filter(Boolean)
        .sort(sortVehicles)[0];
      if (replacement) {
        transaction.set(db.doc(`users/${uid}/vehicles/${replacement.id}`), {
          isDefault: true,
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      }
    }
  });

  return {
    ok: true,
    vehicleId,
  };
});

exports.createReservation = onCall(async (request) => {
  const authUid = request.auth && request.auth.uid;
  const data = request.data || {};
  const reservation = sanitizeReservationCreate(data, authUid || "");
  const doc = db.collection("reservations").doc();
  const reservationCode = await nextReservationCode();
  const availabilityConfig = await getAvailabilityConfig();
  let loyaltyRewardApplied = false;
  let discountCents = 0;

  await db.runTransaction(async (transaction) => {
    await assertSlotHasCapacity(transaction, reservation.slotStart, reservation.slotEnd, availabilityConfig);

    let redemptionDoc = null;
    if (authUid && reservation.loyaltyRewardCode) {
      redemptionDoc = await findUsableRedemptionInTransaction(transaction, authUid, reservation.loyaltyRewardCode);
      if (!redemptionDoc) {
        throw new HttpsError("failed-precondition", "Esta recompensa nao esta disponivel ou ja foi utilizada.");
      }
      loyaltyRewardApplied = true;
      discountCents = reservation.priceCents || 0;
    }

    transaction.set(doc, {
      ...reservation,
      reservationCode,
      status: "pending",
      paymentStatus: loyaltyRewardApplied ? "covered_by_loyalty" : "pending",
      loyaltyRewardApplied,
      discountCents,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });

    if (redemptionDoc) {
      transaction.set(redemptionDoc.ref, {
        status: "reserved",
        reservationId: doc.id,
        reservedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    }
  });

  return {
    ok: true,
    reservationId: doc.id,
    reservationCode,
    status: "pending",
    pendingExpiresAt: null,
    loyaltyRewardApplied,
    loyaltyRewardCode: reservation.loyaltyRewardCode || null,
    priceCents: reservation.priceCents,
    discountCents,
    extras: [],
    paymentStatus: loyaltyRewardApplied ? "covered_by_loyalty" : "pending",
  };
});

exports.getMyReservations = onCall(async (request) => {
  const uid = requireUid(request);
  const snapshot = await db.collection("reservations").where("userUid", "==", uid).get();
  const reservations = snapshot.docs
    .map((doc) => toReservationPayload(doc.id, doc.data()))
    .sort((a, b) => String(b.slotStart).localeCompare(String(a.slotStart)));
  return {
    reservations,
    loyalty: await getLoyaltyPayload(uid),
  };
});

exports.getMyLoyalty = onCall(async (request) => {
  const uid = requireUid(request);
  return await getLoyaltyPayload(uid);
});

exports.submitReservationReview = onCall(async (request) => {
  const uid = requireUid(request);
  const review = sanitizeReview(request.data || {});
  const reviewId = await db.runTransaction(async (transaction) => {
    const reservationRef = db.doc(`reservations/${review.reservationId}`);
    const reservationDoc = await transaction.get(reservationRef);
    if (!reservationDoc.exists) {
      throw new HttpsError("not-found", "Marcacao nao encontrada.");
    }

    const reservation = reservationDoc.data() || {};
    requireReservationOwner(uid, reservation);
    const status = canonicalStatus(reservation.status);
    if (status !== "completed") {
      throw new HttpsError("failed-precondition", "Apenas lavagens concluidas podem ser avaliadas.");
    }
    if (reservation.reviewed === true) {
      throw new HttpsError("failed-precondition", "Esta marcacao ja foi avaliada.");
    }

    const reviewRef = db.collection(`reservations/${review.reservationId}/reviews`).doc(uid);
    transaction.set(reviewRef, {
      uid,
      rating: review.rating,
      tags: review.tags,
      comment: review.comment,
      createdAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(reservationRef, {
      reviewed: true,
      reviewRating: review.rating,
      reviewTags: review.tags,
      reviewComment: review.comment,
      reviewedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    return reviewRef.id;
  });

  return {
    ok: true,
    reviewId,
    reservationId: review.reservationId,
  };
});

exports.cancelMyReservation = onCall(async (request) => {
  const uid = requireUid(request);
  const reservationId = cleanReservationId((request.data || {}).reservationId);
  if (!reservationId) {
    throw new HttpsError("invalid-argument", "Marcacao invalida.");
  }

  await db.runTransaction(async (transaction) => {
    const ref = db.doc(`reservations/${reservationId}`);
    const doc = await transaction.get(ref);
    if (!doc.exists) {
      throw new HttpsError("not-found", "Marcacao nao encontrada.");
    }

    const reservation = doc.data() || {};
    requireReservationOwner(uid, reservation);
    const status = canonicalStatus(reservation.status);
    if (!["pending", "confirmed"].includes(status)) {
      throw new HttpsError("failed-precondition", "Esta marcacao ja nao pode ser cancelada.");
    }
    if (reservation.loyaltyRewardApplied === true && reservation.userUid) {
      await releaseReservedReward(transaction, reservation.userUid, reservation.loyaltyRewardCode, reservationId);
    }

    transaction.set(ref, {
      status: "cancelled",
      cancelledAt: FieldValue.serverTimestamp(),
      cancelledByUid: uid,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return {
    ok: true,
    reservationId,
    status: "cancelled",
  };
});

exports.rescheduleMyReservation = onCall(async (request) => {
  const uid = requireUid(request);
  const reschedule = sanitizeReschedule(request.data || {});
  const availabilityConfig = await getAvailabilityConfig();
  await db.runTransaction(async (transaction) => {
    const ref = db.doc(`reservations/${reschedule.reservationId}`);
    const doc = await transaction.get(ref);
    if (!doc.exists) {
      throw new HttpsError("not-found", "Marcacao nao encontrada.");
    }

    const reservation = doc.data() || {};
    requireReservationOwner(uid, reservation);
    const status = canonicalStatus(reservation.status);
    if (!["pending", "confirmed"].includes(status)) {
      throw new HttpsError("failed-precondition", "Esta marcacao ja nao pode ser remarcada.");
    }
    await assertSlotHasCapacity(
      transaction,
      reschedule.slotStart,
      reschedule.slotEnd,
      availabilityConfig,
      reschedule.reservationId,
    );

    transaction.set(ref, {
      status: "pending",
      slotStart: reschedule.slotStart,
      slotEnd: reschedule.slotEnd,
      previousSlotStart: reservation.slotStart || "",
      previousSlotEnd: reservation.slotEnd || "",
      rescheduledAt: FieldValue.serverTimestamp(),
      rescheduledByUid: uid,
      rescheduleCount: Number.isFinite(Number(reservation.rescheduleCount))
        ? Number(reservation.rescheduleCount) + 1
        : 1,
      acceptedAt: null,
      acceptedByUid: "",
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  return {
    ok: true,
    reservationId: reschedule.reservationId,
    status: "pending",
    slotStart: reschedule.slotStart,
    slotEnd: reschedule.slotEnd,
  };
});

exports.redeemMyLoyaltyReward = onCall(async (request) => {
  const uid = requireUid(request);
  const receipt = await db.runTransaction(async (transaction) => {
    const loyaltyRef = db.doc(`users/${uid}/loyalty/current`);
    const settingsRef = db.doc("adminConfig/loyaltySettings");
    const loyaltyDoc = await transaction.get(loyaltyRef);
    const settingsDoc = await transaction.get(settingsRef);
    const loyalty = normalizeLoyalty(
      loyaltyDoc.exists ? loyaltyDoc.data() : {},
      sanitizeLoyaltySettings(settingsDoc.exists ? settingsDoc.data() : {}),
    );
    if (loyalty.availableRewards <= 0) {
      throw new HttpsError("failed-precondition", "Nao existem recompensas disponiveis.");
    }
    const outstandingRedemptions = await countOutstandingRedemptions(transaction, uid);
    if (outstandingRedemptions >= loyalty.availableRewards) {
      throw new HttpsError("already-exists", "Esta recompensa ja foi resgatada.");
    }

    const redemptionRef = db.collection(`users/${uid}/loyaltyRedemptions`).doc();
    const rewardNumber = loyalty.claimedRewards + 1;
    const rewardCode = `SUDS-${rewardNumber}-${crypto.randomBytes(3).toString("hex").toUpperCase()}`;
    transaction.set(redemptionRef, {
      rewardCode,
      rewardNumber,
      status: "issued",
      createdAt: FieldValue.serverTimestamp(),
    });
    return {
      redemption: {
        id: redemptionRef.id,
        rewardCode,
        rewardNumber,
        status: "issued",
        createdAt: new Date().toISOString(),
      },
      loyalty,
    };
  });

  return {
    ok: true,
    redemption: receipt.redemption,
    loyalty: loyaltyPayloadFromSummary(receipt.loyalty),
  };
});

exports.getAdminPendingReservations = onCall(async (request) => {
  await requireAdmin(request);
  const snapshot = await db.collection("reservations").where("status", "==", "pending").get();
  return {
    requests: snapshot.docs
      .map((doc) => toAdminReservationPayload(doc.id, doc.data()))
      .sort((a, b) => String(a.slotStart).localeCompare(String(b.slotStart))),
  };
});

exports.getAdminAcceptedReservations = onCall(async (request) => {
  await requireAdmin(request);
  const snapshot = await db.collection("reservations").where("status", "in", ["confirmed", "in_progress"]).get();
  return {
    requests: snapshot.docs
      .map((doc) => toAdminReservationPayload(doc.id, doc.data()))
      .sort((a, b) => String(a.slotStart).localeCompare(String(b.slotStart))),
  };
});

exports.getAdminCompletableReservations = onCall(async (request) => {
  await requireAdmin(request);
  const snapshot = await db.collection("reservations").where("status", "==", "in_progress").get();
  return {
    requests: snapshot.docs
      .map((doc) => toAdminReservationPayload(doc.id, doc.data()))
      .sort((a, b) => String(a.slotStart).localeCompare(String(b.slotStart))),
  };
});

exports.acceptReservation = onCall(async (request) => {
  const admin = await requireAdmin(request);
  return await updateReservationStatus(request.data, admin.uid, {
    expected: adminReservationExpectedStatuses("accept"),
    status: "confirmed",
    auditAtField: "acceptedAt",
    auditByField: "acceptedByUid",
  });
});

exports.rejectReservation = onCall(async (request) => {
  const admin = await requireAdmin(request);
  const reason = cleanString((request.data || {}).rejectionReason, 280);
  return await updateReservationStatus(request.data, admin.uid, {
    expected: adminReservationExpectedStatuses("reject"),
    status: "rejected",
    auditAtField: "rejectedAt",
    auditByField: "rejectedByUid",
    extra: { rejectionReason: reason },
  });
});

exports.startReservation = onCall(async (request) => {
  const admin = await requireAdmin(request);
  return await updateReservationStatus(request.data, admin.uid, {
    expected: adminReservationExpectedStatuses("start"),
    status: "in_progress",
    auditAtField: "startedAt",
    auditByField: "startedByUid",
  });
});

exports.completeReservation = onCall(async (request) => {
  const admin = await requireAdmin(request);
  return await updateReservationStatus(request.data, admin.uid, {
    expected: adminReservationExpectedStatuses("complete"),
    status: "completed",
    auditAtField: "completedAt",
    auditByField: "completedByUid",
    completeLoyalty: true,
  });
});

exports.onReservationCreatedNotify = onDocumentCreated("reservations/{reservationId}", async (event) => {
  const reservation = normalizeReservationSnapshot(event.params.reservationId, event.data && event.data.data());
  await deliverReservationEvents(null, reservation);
});

exports.onReservationUpdatedNotify = onDocumentUpdated("reservations/{reservationId}", async (event) => {
  const before = normalizeReservationSnapshot(
    event.params.reservationId,
    event.data && event.data.before && event.data.before.data(),
  );
  const after = normalizeReservationSnapshot(
    event.params.reservationId,
    event.data && event.data.after && event.data.after.data(),
  );
  await deliverReservationEvents(before, after);
});

exports.sendReservationReminders = onSchedule("every 15 minutes", async () => {
  await sendDueReservationReminders(new Date());
});

async function deliverReservationEvents(before, after) {
  const settings = await getNotificationSettings();
  await deliverReservationNotifications({
    before,
    after,
    settings,
    getAdminTokens,
    getUserTokens,
    sendToTokens,
    logDelivery: logNotificationDelivery,
  });
}

async function sendDueReservationReminders(now) {
  const settings = await getNotificationSettings();
  if (!notificationAllowed(settings, "booking_reminder")) return { scanned: 0, sent: 0, skipped: "disabled" };

  const template = pickTemplate(settings, "booking_reminder");
  if (!template) return { scanned: 0, sent: 0, skipped: "template_disabled" };
  if (isWithinQuietHours(now, settings)) return { scanned: 0, sent: 0, skipped: "quiet_hours" };

  const window = reminderWindow(now, settings.reminderLeadMinutes);
  const snapshot = await db.collection("reservations")
    .where("slotStart", ">", window.startIso)
    .where("slotStart", "<=", window.endIso)
    .get();

  let sentCount = 0;
  let scanned = 0;
  for (const doc of snapshot.docs) {
    scanned += 1;
    const reservation = await claimReservationReminder(doc.ref, doc.id, now, window.leadMinutes);
    if (!reservation) continue;

    const preferenceKey = preferenceGateForTemplate("booking_reminder");
    const tokens = await getUserTokens(reservationOwnerUid(reservation), preferenceKey);
    const message = buildNotificationMessage(template, reservation);
    const sent = await sendToTokens(tokens, message);
    sentCount += Number(sent.sent) || 0;

    await doc.ref.set({
      reminderSentAt: FieldValue.serverTimestamp(),
      reminderLastSentAtIso: now.toISOString(),
      reminderDelivery: {
        tokenCount: tokens.length,
        sent: Number(sent.sent) || 0,
        failed: Number(sent.failed) || 0,
        invalidated: Number(sent.invalidated) || 0,
      },
      reminderClaimCompletedAt: FieldValue.serverTimestamp(),
      reminderClaimKey: FieldValue.delete(),
      reminderClaimedAt: FieldValue.delete(),
      reminderClaimedAtIso: FieldValue.delete(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    await logNotificationDelivery({ target: "customer", templateKey: "booking_reminder" }, reservation, sent);
  }

  return { scanned, sent: sentCount };
}

async function claimReservationReminder(ref, reservationId, now, leadMinutes) {
  return await db.runTransaction(async (transaction) => {
    const doc = await transaction.get(ref);
    if (!doc.exists) return null;
    const reservation = normalizeReservationSnapshot(reservationId, doc.data());
    if (!reservationNeedsReminder(reservation, now, leadMinutes)) return null;
    if (reminderClaimIsActive(reservation, now)) return null;

    transaction.set(ref, {
      reminderClaimKey: reminderDedupeKey(reservation),
      reminderClaimedAt: FieldValue.serverTimestamp(),
      reminderClaimedAtIso: now.toISOString(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    return reservation;
  });
}

async function updateReservationStatus(data, adminUid, options) {
  const reservationId = cleanReservationId(data && data.reservationId);
  if (!reservationId) {
    throw new HttpsError("invalid-argument", "Marcacao invalida.");
  }

  return await db.runTransaction(async (transaction) => {
    const ref = db.doc(`reservations/${reservationId}`);
    const doc = await transaction.get(ref);
    if (!doc.exists) {
      throw new HttpsError("not-found", "Marcacao nao encontrada.");
    }
    const reservation = doc.data() || {};
    const currentStatus = canonicalStatus(reservation.status) || cleanString(reservation.status, 40);
    if (!options.expected.includes(currentStatus)) {
      throw new HttpsError("failed-precondition", "Esta marcacao ja mudou de estado.");
    }

    const patch = {
      status: options.status,
      updatedAt: FieldValue.serverTimestamp(),
      [options.auditAtField]: FieldValue.serverTimestamp(),
      [options.auditByField]: adminUid,
      ...(options.extra || {}),
    };

    if (options.completeLoyalty && reservation.userUid) {
      const loyalty = await completeReservationLoyalty(transaction, reservation.userUid, ref, reservation);
      patch.loyalty = loyaltyPayloadFromSummary(loyalty);
    }
    if (options.status === "rejected" && reservation.loyaltyRewardApplied === true && reservation.userUid) {
      await releaseReservedReward(transaction, reservation.userUid, reservation.loyaltyRewardCode, reservationId);
    }

    transaction.set(ref, patch, { merge: true });
    return {
      ok: true,
      reservationId,
      reservationCode: cleanString(reservation.reservationCode, 80),
      status: options.status,
    };
  });
}

async function completeReservationLoyalty(transaction, uid, reservationRef, reservation) {
  const loyaltyRef = db.doc(`users/${uid}/loyalty/current`);
  const settingsRef = db.doc("adminConfig/loyaltySettings");
  const loyaltyDoc = await transaction.get(loyaltyRef);
  const settingsDoc = await transaction.get(settingsRef);
  const settings = sanitizeLoyaltySettings(settingsDoc.exists ? settingsDoc.data() : {});
  const current = normalizeLoyalty(loyaltyDoc.exists ? loyaltyDoc.data() : {}, settings);
  let nextTotal = current.totalWashes;
  let nextClaimed = current.claimedRewards;

  if (reservation.loyaltyRewardApplied) {
    nextClaimed += 1;
    await markRewardUsed(transaction, uid, reservation.loyaltyRewardCode, reservationRef.id);
  } else if (reservationEarnsLoyaltyStamp(reservation)) {
    nextTotal += 1;
    const stampRef = db.collection(`users/${uid}/loyaltyStamps`).doc(reservationRef.id);
    transaction.set(stampRef, {
      serviceId: reservation.serviceId || "",
      serviceName: reservation.serviceName || "",
      slotStart: reservation.slotStart || "",
      slotEnd: reservation.slotEnd || "",
      points: 1,
      reservationId: reservationRef.id,
      createdAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  }

  const next = normalizeLoyalty({
    ...current,
    totalWashes: nextTotal,
    claimedRewards: nextClaimed,
  }, settings);
  transaction.set(loyaltyRef, {
    ...next,
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
  return next;
}

async function markRewardUsed(transaction, uid, rewardCode, reservationId) {
  if (!rewardCode) return;
  const query = db.collection(`users/${uid}/loyaltyRedemptions`)
    .where("rewardCode", "==", rewardCode)
    .limit(1);
  const snapshot = await transaction.get(query);
  snapshot.docs.forEach((doc) => {
    transaction.set(doc.ref, {
      status: "used",
      reservationId,
      usedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
}

async function sendToTokens(tokens, message) {
  const tokenRecords = normalizeTokenRecords(tokens);
  if (tokenRecords.length === 0) return { sent: 0, failed: 0, invalidated: 0 };

  let sent = 0;
  let failed = 0;
  const invalidTokenRecords = [];
  for (let index = 0; index < tokenRecords.length; index += 500) {
    const batchRecords = tokenRecords.slice(index, index + 500);
    const batch = batchRecords.map((record) => record.token);
    const response = await messaging.sendEachForMulticast(buildMulticastMessage(batch, message));
    sent += response.successCount;
    failed += response.failureCount;
    invalidTokenRecords.push(...invalidTokenRecordsForBatch(batchRecords, response));
  }
  const invalidated = await disableInvalidNotificationTokens(invalidTokenRecords);
  return { sent, failed, invalidated };
}

async function sendCampaignDraftTestToSelf(admin, campaignId) {
  const doc = await db.doc(`notificationCampaignDrafts/${campaignId}`).get();
  if (!doc.exists) {
    throw new HttpsError("not-found", "Campanha nao encontrada.");
  }
  const campaign = toNotificationCampaignDraftPayload(doc.id, doc.data());
  if (!campaign || campaign.status === "archived") {
    throw new HttpsError("failed-precondition", "Esta campanha nao pode ser testada.");
  }

  const tokens = await getUserEnabledTokens(admin.uid);
  const sent = await sendToTokens(tokens, {
    notification: {
      title: campaign.title,
      body: campaign.body,
    },
    data: removeBlankValues({
      type: "admin_test_notification",
      templateKey: "campaign_draft",
      campaignId: campaign.campaignId,
      targetScope: "self",
      testOnly: "true",
      dedupeKey: cleanString(`campaign_draft:${campaign.campaignId}:${admin.uid}`, 220),
      source: "functions",
    }),
  });

  const sentCount = Number(sent.sent) || 0;
  const failedCount = Number(sent.failed) || 0;
  const invalidatedCount = Number(sent.invalidated) || 0;
  return {
    notificationId: `test-${Date.now()}`,
    templateKey: "campaign_draft",
    campaignId: campaign.campaignId,
    deliveryState: deliveryStateForSend(tokens.length, sentCount, failedCount),
    recipientUid: admin.uid,
    targetScope: "self",
    testOnly: true,
    targetAudience: campaign.targetAudience,
    marketingConsentRequired: campaign.marketingConsentRequired,
    sendBlocked: true,
    sendBlockedReason: "campaign-self-test-only",
    deliveryLocked: true,
    sendState: campaign.sendState || "ready",
    tokenCount: tokens.length,
    sentCount,
    failedCount,
    invalidatedCount,
    message: selfTestDeliveryMessage(tokens.length, sentCount),
  };
}

function removeBlankValues(values) {
  return Object.fromEntries(
    Object.entries(values).filter(([, value]) => String(value || "").trim().length > 0),
  );
}

async function getUserEnabledTokens(uid) {
  if (!uid) return [];
  const snapshot = await db.collection(`users/${uid}/notificationTokens`)
    .where("enabled", "==", true)
    .get();
  return snapshot.docs.map((doc) => ({
    uid,
    tokenId: cleanString(doc.get("tokenId") || doc.id, 160),
    token: doc.get("token"),
    refPath: doc.ref.path,
  }));
}

async function getUserTokens(uid, preferenceKey) {
  if (!uid) return [];
  const [snapshot, preferences] = await Promise.all([
    db.collection(`users/${uid}/notificationTokens`)
      .where("enabled", "==", true)
      .get(),
    getUserPreferences(uid),
  ]);
  return snapshot.docs
    .filter((doc) => tokenAllowsPreference(doc.data() || {}, preferenceKey, preferences))
    .map((doc) => ({
      uid,
      tokenId: cleanString(doc.get("tokenId") || doc.id, 160),
      token: doc.get("token"),
      refPath: doc.ref.path,
    }));
}

async function getAdminTokens(preferenceKey) {
  const adminUsers = await db.collection("users").where("role", "==", "admin").get();
  const tokens = [];
  for (const userDoc of adminUsers.docs) {
    tokens.push(...await getUserTokens(userDoc.id, preferenceKey));
  }
  return tokens;
}

async function disableInvalidNotificationTokens(records) {
  const invalidRecords = normalizeTokenRecords(records).filter((record) => record.refPath);
  if (invalidRecords.length === 0) return 0;

  const batch = db.batch();
  invalidRecords.forEach((record) => {
    batch.set(db.doc(record.refPath), {
      enabled: false,
      invalidatedAt: FieldValue.serverTimestamp(),
      invalidationReason: "fcm-permanent-token-error",
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
  await batch.commit();
  return invalidRecords.length;
}

async function getCampaignRecipientTokens(targetAudience, marketingConsentRequired) {
  if (targetAudience === "admins") {
    return await getAdminTokens("marketingEnabled");
  }

  let usersQuery = db.collection("users");
  if (targetAudience === "marketing_opt_in_users" || marketingConsentRequired) {
    usersQuery = usersQuery.where("marketingOptIn", "==", true);
  }
  const users = await usersQuery.get();
  const tokens = [];
  for (const userDoc of users.docs) {
    tokens.push(...await getUserTokens(userDoc.id, "marketingEnabled"));
  }
  return tokens;
}

function sanitizeReservationCreate(data, authUid) {
  const customerName = cleanString(data.customerName, 120);
  const customerEmail = cleanString(data.customerEmail, 160);
  const customerPhone = cleanString(data.customerPhone, 40);
  const serviceId = cleanString(data.serviceId, 120);
  const serviceName = cleanString(data.serviceName, 120);
  const slotStart = cleanString(data.slotStart, 80);
  const slotEnd = cleanString(data.slotEnd, 80);
  const vehicleType = cleanString(data.vehicleType, 80) || "passageiros";
  const gdprConsent = data.gdprConsent === true;
  if (!customerName || !customerEmail || !customerPhone || !serviceId || !serviceName || !slotStart || !slotEnd) {
    throw new HttpsError("invalid-argument", "Preencha os dados obrigatorios da marcacao.");
  }
  if (!gdprConsent) {
    throw new HttpsError("invalid-argument", "E necessario aceitar a politica de privacidade.");
  }

  return {
    userUid: authUid,
    customerName,
    customerEmail,
    customerPhone,
    serviceId,
    serviceName,
    slotStart,
    slotEnd,
    vehicleType,
    notes: cleanString(data.notes, 500),
    userVehicleId: cleanString(data.userVehicleId, 160),
    vehicleLabel: cleanString(data.vehicleLabel, 160),
    loyaltyRewardCode: cleanString(data.loyaltyRewardCode, 120),
    extraIds: Array.isArray(data.extraIds)
      ? data.extraIds.map((item) => cleanString(item, 120)).filter(Boolean).slice(0, 20)
      : [],
    priceCents: Number.isFinite(Number(data.priceCents)) ? Math.max(0, Number(data.priceCents)) : null,
  };
}

function sanitizeReview(data) {
  const reservationId = cleanReservationId(data.reservationId);
  const rating = Number(data.rating);
  const tags = Array.isArray(data.tags)
    ? data.tags.map((tag) => cleanString(tag, 40)).filter(Boolean).slice(0, 8)
    : [];
  const comment = cleanString(data.comment, 1000);
  if (!reservationId) {
    throw new HttpsError("invalid-argument", "Marcacao invalida.");
  }
  if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
    throw new HttpsError("invalid-argument", "Escolha uma avaliacao entre 1 e 5 estrelas.");
  }
  return {
    reservationId,
    rating,
    tags,
    comment,
  };
}

function sanitizeReschedule(data) {
  const reservationId = cleanReservationId(data.reservationId);
  const slotStart = cleanString(data.slotStart, 80);
  const slotEnd = cleanString(data.slotEnd, 80);
  if (!reservationId) {
    throw new HttpsError("invalid-argument", "Marcacao invalida.");
  }
  if (!slotStart || !slotEnd || slotEnd <= slotStart) {
    throw new HttpsError("invalid-argument", "Escolha uma data e hora validas.");
  }
  return {
    reservationId,
    slotStart,
    slotEnd,
  };
}

async function nextReservationCode() {
  const now = new Date();
  const date = [
    now.getUTCFullYear(),
    String(now.getUTCMonth() + 1).padStart(2, "0"),
    String(now.getUTCDate()).padStart(2, "0"),
  ].join("");
  return `SS-${date}-${crypto.randomBytes(2).toString("hex").toUpperCase()}`;
}

function toReservationPayload(id, data) {
  const reservation = data || {};
  const status = canonicalStatus(reservation.status) || cleanString(reservation.status, 40) || "pending";
  return {
    id,
    reservationCode: cleanString(reservation.reservationCode, 80),
    serviceId: cleanString(reservation.serviceId, 120),
    serviceName: cleanString(reservation.serviceName, 120),
    slotStart: firestoreDateToIso(reservation.slotStart),
    slotEnd: firestoreDateToIso(reservation.slotEnd),
    status,
    paymentStatus: cleanString(reservation.paymentStatus, 40),
    vehicleType: cleanString(reservation.vehicleType, 80) || "passageiros",
    vehicleLabel: cleanString(reservation.vehicleLabel, 160),
    priceCents: Number.isFinite(Number(reservation.priceCents)) ? Number(reservation.priceCents) : null,
    upcoming: !closedReservationStatuses.has(status),
    reviewed: reservation.reviewed === true,
    reviewRating: Number.isFinite(Number(reservation.reviewRating)) ? Number(reservation.reviewRating) : null,
    reviewTags: Array.isArray(reservation.reviewTags) ? reservation.reviewTags.map((item) => cleanString(item, 40)) : [],
    reviewComment: cleanString(reservation.reviewComment, 500),
    extras: Array.isArray(reservation.extras) ? reservation.extras.map(toExtraPayload) : [],
    createdAt: firestoreDateToIso(reservation.createdAt),
    updatedAt: firestoreDateToIso(reservation.updatedAt),
    cancelledAt: firestoreDateToIso(reservation.cancelledAt) || null,
    rejectedAt: firestoreDateToIso(reservation.rejectedAt) || null,
    rejectionReason: cleanString(reservation.rejectionReason, 280),
    acceptedAt: firestoreDateToIso(reservation.acceptedAt) || null,
    pendingExpiresAt: firestoreDateToIso(reservation.pendingExpiresAt) || null,
    rescheduledAt: firestoreDateToIso(reservation.rescheduledAt) || null,
    previousSlotStart: firestoreDateToIso(reservation.previousSlotStart) || null,
    previousSlotEnd: firestoreDateToIso(reservation.previousSlotEnd) || null,
    rescheduleCount: Number.isFinite(Number(reservation.rescheduleCount)) ? Number(reservation.rescheduleCount) : 0,
  };
}

function toAdminReservationPayload(id, data) {
  const reservation = data || {};
  const status = canonicalStatus(reservation.status) || cleanString(reservation.status, 40) || "pending";
  return {
    id,
    reservationCode: cleanString(reservation.reservationCode, 80),
    customerName: cleanString(reservation.customerName, 120),
    customerEmail: cleanString(reservation.customerEmail, 160),
    customerPhone: cleanString(reservation.customerPhone, 40),
    serviceId: cleanString(reservation.serviceId, 120),
    serviceName: cleanString(reservation.serviceName, 120),
    slotStart: firestoreDateToIso(reservation.slotStart),
    slotEnd: firestoreDateToIso(reservation.slotEnd),
    status,
    paymentStatus: cleanString(reservation.paymentStatus, 40) || "pending",
    vehicleType: cleanString(reservation.vehicleType, 80) || "passageiros",
    vehicleLabel: cleanString(reservation.vehicleLabel, 160),
    priceCents: Number.isFinite(Number(reservation.priceCents)) ? Number(reservation.priceCents) : null,
    extras: Array.isArray(reservation.extras) ? reservation.extras.map(toExtraPayload) : [],
    notes: cleanString(reservation.notes, 500),
    createdAt: firestoreDateToIso(reservation.createdAt),
    pendingExpiresAt: firestoreDateToIso(reservation.pendingExpiresAt) || null,
    loyaltyRewardApplied: reservation.loyaltyRewardApplied === true,
    canStart: status === "confirmed",
    canComplete: status === "in_progress",
    acceptedAt: firestoreDateToIso(reservation.acceptedAt) || null,
    acceptedByUid: cleanString(reservation.acceptedByUid, 160),
    startedAt: firestoreDateToIso(reservation.startedAt) || null,
    startedByUid: cleanString(reservation.startedByUid, 160),
    rejectedAt: firestoreDateToIso(reservation.rejectedAt) || null,
    rejectedByUid: cleanString(reservation.rejectedByUid, 160),
    completedAt: firestoreDateToIso(reservation.completedAt) || null,
    completedByUid: cleanString(reservation.completedByUid, 160),
  };
}

function toExtraPayload(extra) {
  return {
    id: cleanString(extra && extra.id, 120),
    name: cleanString(extra && extra.name, 120),
    priceCents: Number.isFinite(Number(extra && extra.priceCents)) ? Math.max(0, Number(extra.priceCents)) : 0,
  };
}

async function getLoyaltyPayload(uid) {
  const [doc, settingsDoc] = await Promise.all([
    db.doc(`users/${uid}/loyalty/current`).get(),
    db.doc("adminConfig/loyaltySettings").get(),
  ]);
  return loyaltyPayloadFromSummary(
    normalizeLoyalty(
      doc.exists ? doc.data() : {},
      sanitizeLoyaltySettings(settingsDoc.exists ? settingsDoc.data() : {}),
    ),
  );
}

function loyaltyPayloadFromSummary(summary) {
  return {
    totalWashes: summary.totalWashes,
    currentWashes: summary.currentWashes,
    targetWashes: summary.targetWashes,
    remainingWashes: summary.remainingWashes,
    progress: summary.progress,
    rewardReady: summary.rewardReady,
    completedRewards: summary.completedRewards,
    claimedRewards: summary.claimedRewards,
    availableRewards: summary.availableRewards,
    rewardType: summary.rewardType,
    rewardValue: summary.rewardValue,
    rewardDescription: summary.rewardDescription,
    stampHistory: [],
    redemptions: [],
  };
}

async function findUsableRedemptionInTransaction(transaction, uid, rewardCode) {
  const query = db.collection(`users/${uid}/loyaltyRedemptions`)
    .where("rewardCode", "==", rewardCode)
    .limit(5);
  const snapshot = await transaction.get(query);
  return snapshot.docs.find((doc) => canonicalRedemptionStatus(doc.get("status")) === "issued") || null;
}

async function countOutstandingRedemptions(transaction, uid) {
  const query = db.collection(`users/${uid}/loyaltyRedemptions`)
    .where("status", "in", ["issued", "reserved"]);
  const snapshot = await transaction.get(query);
  return snapshot.docs.length;
}

async function releaseReservedReward(transaction, uid, rewardCode, reservationId) {
  if (!rewardCode) return;
  const query = db.collection(`users/${uid}/loyaltyRedemptions`)
    .where("rewardCode", "==", rewardCode)
    .limit(5);
  const snapshot = await transaction.get(query);
  snapshot.docs.forEach((doc) => {
    const status = canonicalRedemptionStatus(doc.get("status"));
    const currentReservationId = cleanString(doc.get("reservationId"), 160);
    if (status !== "reserved" || currentReservationId !== reservationId) return;
    transaction.set(doc.ref, {
      status: "issued",
      reservationId: "",
      releasedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
}

async function assertSlotHasCapacity(transaction, slotStart, slotEnd, availabilityConfig, excludeReservationId = "") {
  const date = cleanString(slotStart, 10);
  if (availabilityConfig.blockedSlots.some((slot) => slotOverlapsBlockedRange(slotStart, slotEnd, slot))) {
    throw new HttpsError("already-exists", "Este horario esta bloqueado.");
  }
  const capacityPerSlot = capacityForDate(date, availabilityConfig);
  if (capacityPerSlot <= 0) {
    throw new HttpsError("already-exists", "Este horario deixou de estar disponivel.");
  }
  const query = db.collection("reservations").where("slotStart", "==", slotStart);
  const snapshot = await transaction.get(query);
  const used = snapshot.docs.filter((doc) => {
    if (doc.id === excludeReservationId) return false;
    const status = canonicalStatus(doc.get("status"));
    return !status || !closedReservationStatuses.has(status);
  }).length;
  if (used >= capacityPerSlot) {
    throw new HttpsError("already-exists", "Este horario deixou de estar disponivel.");
  }
}

function canonicalRedemptionStatus(value) {
  const status = cleanString(value, 40)
    .toLowerCase()
    .replace(/[\s-]+/g, "_");
  switch (status) {
    case "reserved":
    case "applied":
    case "em_uso":
      return "reserved";
    case "used":
    case "redeemed":
    case "usada":
      return "used";
    case "cancelled":
    case "canceled":
    case "expired":
      return "closed";
    case "issued":
    default:
      return "issued";
  }
}

function firestoreDateToIso(value) {
  if (!value) return "";
  if (typeof value === "string") return value;
  if (typeof value.toDate === "function") return value.toDate().toISOString();
  if (value instanceof Date) return value.toISOString();
  return "";
}

function cleanReservationId(value) {
  const id = cleanString(value, 160);
  if (!id || id.includes("/") || id.includes("?") || id.includes("#")) return "";
  return id;
}

function requireReservationOwner(uid, reservation) {
  const ownerUid = reservationOwnerUid(reservation);
  if (!ownerUid || ownerUid !== uid) {
    throw new HttpsError("permission-denied", "Esta marcacao nao pertence a sessao atual.");
  }
}

const closedReservationStatuses = new Set([
  "completed",
  "cancelled",
  "canceled",
  "rejected",
  "expired",
]);

async function getUserPreferences(uid) {
  const doc = await db.doc(`users/${uid}/notificationPreferences/current`).get();
  return sanitizePreferences(doc.exists ? doc.data() : {});
}

async function saveUserPreferences(uid, preferences) {
  const tokenPatch = tokenPreferencePatch(preferences);
  const userPatch = userProfilePreferencePatch(preferences);
  await db.doc(`users/${uid}/notificationPreferences/current`).set({
    ...preferences,
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
  await db.doc(`users/${uid}`).set({
    ...userPatch,
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });

  const tokens = await db.collection(`users/${uid}/notificationTokens`).get();
  const batch = db.batch();
  tokens.docs.forEach((doc) => {
    batch.set(doc.ref, {
      ...tokenPatch,
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  });
  await batch.commit();
}

async function syncProfileNotificationPreferenceBits(uid, profile) {
  const patch = {
    marketingEnabled: profile.marketingOptIn,
    appointmentReminderEnabled: profile.appointmentReminderOptIn,
    updatedAt: FieldValue.serverTimestamp(),
  };
  await db.doc(`users/${uid}/notificationPreferences/current`).set(patch, { merge: true });

  const tokens = await db.collection(`users/${uid}/notificationTokens`).get();
  const batch = db.batch();
  tokens.docs.forEach((doc) => {
    batch.set(doc.ref, patch, { merge: true });
  });
  await batch.commit();
}

async function getNotificationSettings() {
  const doc = await db.doc("adminConfig/notificationSettings").get();
  return sanitizeNotificationSettings(doc.exists ? doc.data() : {});
}

async function getPublicServiceCatalog() {
  const [servicesSnapshot, extrasSnapshot] = await Promise.all([
    db.collection("serviceCatalog").get(),
    db.collection("serviceExtras").get(),
  ]);

  const services = servicesSnapshot.docs
    .map((doc) => toServiceCatalogItem(doc.id, doc.data()))
    .filter((item) => item && item.archived !== true)
    .sort(sortBySortOrderThenName)
    .map(({ archived, sortOrder, ...item }) => item);
  const extras = extrasSnapshot.docs
    .map((doc) => toServiceExtraItem(doc.id, doc.data()))
    .filter((item) => item && item.archived !== true)
    .sort(sortBySortOrderThenName)
    .map(({ archived, sortOrder, ...item }) => item);

  return {
    services: services.length > 0 ? services : defaultServiceCatalogServices,
    extras: extras.length > 0 ? extras : defaultServiceCatalogExtras,
  };
}

async function getPublicBusinessInfo() {
  const doc = await db.doc("adminConfig/businessInfo").get();
  return sanitizeBusinessInfo(doc.exists ? doc.data() : {});
}

async function getAdminBusinessInfoConfig(fallback = {}) {
  const doc = await db.doc("adminConfig/businessInfo").get();
  const data = sanitizeBusinessInfo(doc.exists ? doc.data() : {});
  return {
    ...data,
    source: doc.exists ? "firestore" : "default",
    updatedAtIso: firestoreDateToIso(doc.exists ? doc.get("updatedAt") : null),
    updatedByUid: cleanString((doc.exists ? doc.get("updatedByUid") : "") || fallback.updatedByUid, 160),
  };
}

async function getAdminAvailabilityConfig(fallback = {}) {
  const config = await getAvailabilityConfig();
  const doc = await db.doc("adminConfig/availability").get();
  return {
    defaultMaxBookingsPerSlot: config.capacityPerSlot,
    defaultSlotIntervalMinutes: config.slotIntervalMinutes,
    openingHours: config.openingHours,
    capacityOverrides: config.capacityOverrides,
    blockedSlots: config.blockedSlots,
    source: doc.exists ? "firestore" : "default",
    updatedAtIso: firestoreDateToIso(doc.exists ? doc.get("updatedAt") : null),
    updatedByUid: cleanString((doc.exists ? doc.get("updatedByUid") : "") || fallback.updatedByUid, 160),
  };
}

async function getAdminBookingPolicyConfig(fallback = {}) {
  const doc = await db.doc("adminConfig/bookingPolicy").get();
  const data = sanitizeBookingPolicy(doc.exists ? doc.data() : {});
  return {
    ...data,
    source: doc.exists ? "firestore" : "default",
    updatedAtIso: firestoreDateToIso(doc.exists ? doc.get("updatedAt") : null),
    updatedByUid: cleanString((doc.exists ? doc.get("updatedByUid") : "") || fallback.updatedByUid, 160),
  };
}

async function getAdminLoyaltySettingsConfig(fallback = {}) {
  const doc = await db.doc("adminConfig/loyaltySettings").get();
  const data = sanitizeLoyaltySettings(doc.exists ? doc.data() : {});
  return {
    ...data,
    source: doc.exists ? "firestore" : "default",
    updatedAtIso: firestoreDateToIso(doc.exists ? doc.get("updatedAt") : null),
    updatedByUid: cleanString((doc.exists ? doc.get("updatedByUid") : "") || fallback.updatedByUid, 160),
  };
}

async function getAdminServiceCatalogItems() {
  const snapshot = await db.collection("serviceCatalog").get();
  const items = snapshot.docs
    .map((doc) => toAdminServiceCatalogItem(doc.id, doc.data()))
    .filter(Boolean)
    .sort(sortBySortOrderThenName);
  return items.length > 0
    ? items
    : defaultServiceCatalogServices.map((item) => toAdminServiceCatalogItem(item.id, item)).filter(Boolean);
}

async function getAdminServiceExtraItems() {
  const snapshot = await db.collection("serviceExtras").get();
  const items = snapshot.docs
    .map((doc) => toAdminServiceExtraItem(doc.id, doc.data()))
    .filter(Boolean)
    .sort(sortBySortOrderThenName);
  return items.length > 0
    ? items
    : defaultServiceCatalogExtras.map((item) => toAdminServiceExtraItem(item.id, item)).filter(Boolean);
}

async function getNotificationCampaignDraftsPayload() {
  const snapshot = await db.collection("notificationCampaignDrafts").get();
  return {
    source: snapshot.empty ? "empty" : "firestore",
    campaigns: snapshot.docs
      .map((doc) => toNotificationCampaignDraftPayload(doc.id, doc.data()))
      .filter(Boolean)
      .sort((left, right) => String(right.updatedAtIso || right.createdAtIso).localeCompare(
        String(left.updatedAtIso || left.createdAtIso),
      )),
  };
}

async function buildAvailabilityMonth(request) {
  const anchor = parseDateId(request.anchorDate) || todayUtcDate();
  const year = anchor.getUTCFullYear();
  const month = anchor.getUTCMonth();
  const monthStart = new Date(Date.UTC(year, month, 1));
  const nextMonthStart = new Date(Date.UTC(year, month + 1, 1));
  const reservations = await reservationsBySlot(monthStart, nextMonthStart);
  const config = await getAvailabilityConfig();
  const slotInterval = request.slotIntervalMinutes || config.slotIntervalMinutes;
  const days = [];

  for (let cursor = new Date(monthStart); cursor < nextMonthStart; cursor.setUTCDate(cursor.getUTCDate() + 1)) {
    const dateId = toDateId(cursor);
    const dayOpening = openingForDate(cursor, config.openingHours);
    const slots = dayOpening && !dayOpening.closed
      ? buildDaySlots(dateId, dayOpening, slotInterval, request.serviceDurationMinutes, config, reservations)
      : [];
    days.push({
      id: dateId,
      dayOfMonth: cursor.getUTCDate(),
      dateLabel: `${cursor.getUTCDate()} ${monthShortPt[cursor.getUTCMonth()]}`,
      summaryLabel: `${cursor.getUTCDate()} de ${monthLongPt[cursor.getUTCMonth()]}, ${cursor.getUTCFullYear()}`,
      available: slots.some((slot) => slot.available),
      slots,
    });
  }

  return {
    monthTitle: `${monthLongPt[month]} ${year}`,
    leadingEmptyCells: (monthStart.getUTCDay() + 6) % 7,
    days,
  };
}

async function reservationsBySlot(monthStart, nextMonthStart) {
  const snapshot = await db.collection("reservations")
    .where("slotStart", ">=", monthStart.toISOString())
    .where("slotStart", "<", nextMonthStart.toISOString())
    .get();
  const bySlot = new Map();
  snapshot.docs.forEach((doc) => {
    const reservation = doc.data() || {};
    const status = canonicalStatus(reservation.status);
    if (closedReservationStatuses.has(status)) return;
    const slotStart = cleanString(reservation.slotStart, 80);
    if (!slotStart) return;
    bySlot.set(slotStart, (bySlot.get(slotStart) || 0) + 1);
  });
  return bySlot;
}

async function getAvailabilityConfig() {
  const [doc, capacitySnapshot, blockedSnapshot] = await Promise.all([
    db.doc("adminConfig/availability").get(),
    db.collection("adminConfig/availability/capacityOverrides").get(),
    db.collection("adminConfig/availability/blockedSlots").get(),
  ]);
  const data = doc.exists ? doc.data() || {} : {};
  const openingHours = Array.isArray(data.openingHours)
    ? data.openingHours.map(toOpeningHoursItem).filter(Boolean)
    : [];
  const capacityPerSlot = Number.isFinite(Number(data.defaultMaxBookingsPerSlot))
    ? Number(data.defaultMaxBookingsPerSlot)
    : Number(data.capacityPerSlot);
  const slotIntervalMinutes = Number.isFinite(Number(data.defaultSlotIntervalMinutes))
    ? Number(data.defaultSlotIntervalMinutes)
    : Number(data.slotIntervalMinutes);
  return {
    slotIntervalMinutes: Number.isFinite(slotIntervalMinutes)
      ? Math.max(5, Math.min(240, slotIntervalMinutes))
      : 30,
    capacityPerSlot: Number.isFinite(capacityPerSlot)
      ? Math.max(1, Math.min(20, capacityPerSlot))
      : 1,
    openingHours: openingHours.length > 0 ? openingHours : defaultAvailabilityOpeningHours,
    capacityOverrides: capacitySnapshot.docs
      .map((overrideDoc) => toCapacityOverridePayload(overrideDoc.id, overrideDoc.data()))
      .filter(Boolean),
    blockedSlots: blockedSnapshot.docs
      .map((blockedDoc) => toBlockedSlotPayload(blockedDoc.id, blockedDoc.data()))
      .filter(Boolean),
  };
}

function sanitizeNotificationSettings(data) {
  const templates = Array.isArray(data.templates) ? data.templates : defaultNotificationSettings.templates;
  return {
    bookingStatusEnabled: booleanOrDefault(data.bookingStatusEnabled, defaultNotificationSettings.bookingStatusEnabled),
    appointmentReminderEnabled: booleanOrDefault(
      data.appointmentReminderEnabled,
      defaultNotificationSettings.appointmentReminderEnabled,
    ),
    loyaltyEnabled: booleanOrDefault(data.loyaltyEnabled, defaultNotificationSettings.loyaltyEnabled),
    adminPendingAlertEnabled: booleanOrDefault(
      data.adminPendingAlertEnabled,
      defaultNotificationSettings.adminPendingAlertEnabled,
    ),
    marketingEnabled: booleanOrDefault(data.marketingEnabled, defaultNotificationSettings.marketingEnabled),
    reminderLeadMinutes: Number.isFinite(Number(data.reminderLeadMinutes))
      ? Math.max(5, Math.min(24 * 60, Number(data.reminderLeadMinutes)))
      : defaultNotificationSettings.reminderLeadMinutes,
    quietHoursStart: cleanTime(data.quietHoursStart) || defaultNotificationSettings.quietHoursStart,
    quietHoursEnd: cleanTime(data.quietHoursEnd) || defaultNotificationSettings.quietHoursEnd,
    quietHoursTimeZone: cleanString(data.quietHoursTimeZone, 80) || defaultNotificationSettings.quietHoursTimeZone,
    templates: sanitizeTemplates(templates),
  };
}

function sanitizeTemplates(templates) {
  const byKey = new Map(defaultNotificationSettings.templates.map((template) => [template.key, template]));
  templates.forEach((template) => {
    const key = cleanString(template && template.key, 80);
    if (!key || !byKey.has(key)) return;
    const fallback = byKey.get(key);
    byKey.set(key, {
      key,
      label: fallback.label,
      enabled: booleanOrDefault(template.enabled, fallback.enabled),
      title: cleanString(template.title, 140) || fallback.title,
      body: cleanString(template.body, 512) || fallback.body,
    });
  });
  return [...byKey.values()];
}

function sanitizePreferences(data) {
  return {
    bookingStatusEnabled: booleanOrDefault(data.bookingStatusEnabled, defaultUserPreferences.bookingStatusEnabled),
    appointmentReminderEnabled: booleanOrDefault(
      data.appointmentReminderEnabled,
      defaultUserPreferences.appointmentReminderEnabled,
    ),
    loyaltyEnabled: booleanOrDefault(data.loyaltyEnabled, defaultUserPreferences.loyaltyEnabled),
    adminPendingAlertEnabled: booleanOrDefault(
      data.adminPendingAlertEnabled,
      defaultUserPreferences.adminPendingAlertEnabled,
    ),
    marketingEnabled: booleanOrDefault(data.marketingEnabled, defaultUserPreferences.marketingEnabled),
  };
}

async function requireAdmin(request) {
  const uid = requireUid(request);
  const role = await roleForUser(uid, request.auth && request.auth.token);
  if (role !== "admin") {
    throw new HttpsError("permission-denied", "Apenas administradores podem executar esta operacao.");
  }
  const user = await getAuth().getUser(uid);
  await syncAdminRoleDocument(uid, role, request.auth && request.auth.token, user);
  return { uid, email: user.email || "" };
}

function requireUid(request) {
  const uid = request.auth && request.auth.uid;
  if (!uid) {
    throw new HttpsError("unauthenticated", "Inicie sessao para continuar.");
  }
  return uid;
}

async function roleForUser(uid, token) {
  const claimRole = cleanString(token && (token.role || token.adminRole), 40).toLowerCase();
  if (claimRole === "admin") return "admin";
  if (token && token.admin === true) return "admin";

  const userDoc = await db.doc(`users/${uid}`).get();
  const role = cleanString(userDoc.exists ? userDoc.get("role") : "", 40).toLowerCase();
  return role || "customer";
}

async function syncAdminRoleDocument(uid, role, token, user = null) {
  const patch = adminRoleDocumentPatch(role, user || {}, token || {});
  if (!patch) return;
  await db.doc(`users/${uid}`).set({
    ...patch,
    adminRoleSyncedAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
}

function normalizeReservationSnapshot(id, data) {
  return {
    id,
    ...(data || {}),
  };
}

async function logNotificationDelivery(event, reservation, sent) {
  await db.collection("notificationDeliveries").add({
    templateKey: event.templateKey,
    target: event.target,
    reservationId: reservation.id || reservation.reservationId || "",
    sent: sent.sent,
    failed: sent.failed,
    invalidated: sent.invalidated || 0,
    createdAt: FieldValue.serverTimestamp(),
  });
}

function cleanString(value, maxLength) {
  return String(value || "")
    .trim()
    .replace(/\s+/g, " ")
    .slice(0, maxLength);
}

function cleanTokenId(value) {
  const tokenId = cleanString(value, 160);
  if (!/^[A-Za-z0-9._-]+$/.test(tokenId)) return "";
  return tokenId;
}

function cleanDocumentId(value, maxLength) {
  const id = cleanString(value, maxLength);
  if (!id || id.includes("/") || id.includes("?") || id.includes("#")) return "";
  return id;
}

function cleanTime(value) {
  const time = cleanString(value, 5);
  return /^\d{2}:\d{2}$/.test(time) ? time : "";
}

function booleanOrDefault(value, fallback) {
  return typeof value === "boolean" ? value : fallback;
}

function hashToken(token) {
  return crypto.createHash("sha256").update(token).digest("hex").slice(0, 32);
}

function sanitizeProfileUpdate(data) {
  const displayName = cleanString(data.displayName, 100);
  const phoneNumber = cleanString(data.phoneNumber, 32);
  const photoUrl = cleanString(data.photoUrl, 2048);
  if (!displayName) {
    throw new HttpsError("invalid-argument", "Indique o nome para guardar o perfil.");
  }
  if (phoneNumber.length < 6 || !validPhoneNumber(phoneNumber)) {
    throw new HttpsError("invalid-argument", "Indique um telemovel valido.");
  }
  if (photoUrl && !validProfilePhotoUrl(photoUrl)) {
    throw new HttpsError("invalid-argument", "Indique uma URL de fotografia valida.");
  }
  return {
    displayName,
    phoneNumber,
    marketingOptIn: data.marketingOptIn === true,
    appointmentReminderOptIn: data.appointmentReminderOptIn === true,
    photoUrl,
  };
}

function toProfilePayload(uid, user, data) {
  return {
    uid,
    email: cleanString((data && data.email) || (user && user.email), 160),
    displayName: cleanString((data && data.displayName) || (user && user.displayName), 100),
    phoneNumber: cleanString((data && data.phoneNumber) || (user && user.phoneNumber), 32),
    marketingOptIn: data && data.marketingOptIn === true,
    appointmentReminderOptIn: data && data.appointmentReminderOptIn === true,
    photoUrl: cleanString((data && data.photoUrl) || (user && user.photoURL), 2048),
  };
}

function validPhoneNumber(value) {
  return value.length <= 32 && /^[0-9+().\-\s]+$/.test(value);
}

function validProfilePhotoUrl(value) {
  if (value.length > 2048 || /[\s\u0000-\u001F]/.test(value)) return false;
  try {
    const url = new URL(value);
    return url.protocol === "https:" || url.protocol === "http:";
  } catch (_) {
    return false;
  }
}

function sanitizeVehicleSave(data, requireId) {
  const id = cleanDocumentId(data.vehicleId || data.id, 160);
  const brand = cleanString(data.brand, 80);
  const model = cleanString(data.model, 80);
  const plate = cleanString(data.plate, 24).toUpperCase();
  const color = cleanString(data.color, 60);
  const type = normalizeVehicleType(data.type);

  if (requireId && !id) {
    throw new HttpsError("invalid-argument", "Escolha um veiculo valido.");
  }
  if (!brand) {
    throw new HttpsError("invalid-argument", "Indique a marca do veiculo.");
  }
  if (!model) {
    throw new HttpsError("invalid-argument", "Indique o modelo do veiculo.");
  }
  if (plate.length < 2) {
    throw new HttpsError("invalid-argument", "Indique a matricula do veiculo.");
  }
  if (!["passenger", "suv"].includes(type)) {
    throw new HttpsError("invalid-argument", "Escolha um tipo de veiculo valido.");
  }

  return {
    id,
    brand,
    model,
    plate,
    color,
    type,
    isDefault: data.isDefault === true,
  };
}

function normalizeVehicleType(value) {
  const type = cleanString(value, 40).toLowerCase();
  if (type === "passageiros" || type === "passageiro" || type === "passenger") return "passenger";
  if (type === "suv") return "suv";
  return type;
}

function toVehiclePayload(id, data) {
  if (!data) return null;
  const brand = cleanString(data.brand, 80);
  const model = cleanString(data.model, 80);
  const plate = cleanString(data.plate, 24).toUpperCase();
  if (!id || !brand || !model || !plate) return null;
  const type = normalizeVehicleType(data.type) || "passenger";
  return {
    id,
    brand,
    model,
    plate,
    color: cleanString(data.color, 60),
    type: ["passenger", "suv"].includes(type) ? type : "passenger",
    isDefault: data.isDefault === true,
  };
}

function sortVehicles(left, right) {
  if (left.isDefault !== right.isDefault) return left.isDefault ? -1 : 1;
  const leftLabel = `${left.brand} ${left.model} ${left.plate}`;
  const rightLabel = `${right.brand} ${right.model} ${right.plate}`;
  return leftLabel.localeCompare(rightLabel);
}

function sanitizeAvailabilityRequest(data) {
  const serviceDurationMinutes = Number(data.serviceDurationMinutes);
  const slotIntervalMinutes = data.slotIntervalMinutes == null ? null : Number(data.slotIntervalMinutes);
  const anchorDate = cleanString(data.anchorDate, 10);
  if (!Number.isInteger(serviceDurationMinutes) || serviceDurationMinutes < 5 || serviceDurationMinutes > 480) {
    throw new HttpsError("invalid-argument", "A duracao do servico e invalida.");
  }
  if (slotIntervalMinutes != null && (!Number.isInteger(slotIntervalMinutes) || slotIntervalMinutes < 5 || slotIntervalMinutes > 240)) {
    throw new HttpsError("invalid-argument", "O intervalo de horarios e invalido.");
  }
  if (anchorDate && !parseDateId(anchorDate)) {
    throw new HttpsError("invalid-argument", "A data de disponibilidade e invalida.");
  }
  return {
    serviceDurationMinutes,
    slotIntervalMinutes,
    anchorDate: anchorDate || "",
  };
}

function sanitizeBusinessInfo(data) {
  const fallback = defaultBusinessInfo;
  const openingHours = Array.isArray(data.openingHours)
    ? data.openingHours.map(toBusinessOpeningHoursItem).filter(Boolean)
    : [];
  const faq = Array.isArray(data.faq)
    ? data.faq.map(toFaqItem).filter(Boolean)
    : [];
  const stats = Array.isArray(data.stats)
    ? data.stats.map(toStatItem).filter(Boolean)
    : [];
  const socialLinks = Array.isArray(data.socialLinks)
    ? data.socialLinks.map(toSocialLinkItem).filter(Boolean)
    : [];
  return {
    phone: cleanString(data.phone, 80) || fallback.phone,
    phoneUri: cleanString(data.phoneUri, 160) || fallback.phoneUri,
    email: cleanString(data.email, 160) || fallback.email,
    emailUri: cleanString(data.emailUri, 200) || fallback.emailUri,
    addressLine1: cleanString(data.addressLine1, 240) || fallback.addressLine1,
    addressLine2: cleanString(data.addressLine2, 240) || fallback.addressLine2,
    mapsUri: cleanString(data.mapsUri, 600) || fallback.mapsUri,
    whatsappUri: cleanString(data.whatsappUri, 200) || fallback.whatsappUri,
    openingHours: openingHours.length > 0 ? openingHours : fallback.openingHours,
    faq: faq.length > 0 ? faq : fallback.faq,
    stats: stats.length > 0 ? stats : fallback.stats,
    socialLinks,
  };
}

function sanitizeBusinessInfoUpdate(data) {
  const config = sanitizeBusinessInfo(data);
  if (!config.phone && !config.email) {
    throw new HttpsError("invalid-argument", "Indique pelo menos um contacto do negocio.");
  }
  return config;
}

function sanitizeAvailabilityUpdate(data) {
  const openingHours = Array.isArray(data.openingHours)
    ? data.openingHours.map(toOpeningHoursItem).filter(Boolean)
    : [];
  if (openingHours.length === 0) {
    throw new HttpsError("invalid-argument", "Configure pelo menos um horario de funcionamento.");
  }
  return {
    defaultMaxBookingsPerSlot: boundedInteger(data.defaultMaxBookingsPerSlot, 1, 20, "Capacidade invalida."),
    defaultSlotIntervalMinutes: boundedInteger(data.defaultSlotIntervalMinutes, 5, 240, "Intervalo de horarios invalido."),
    openingHours,
  };
}

function sanitizeBookingPolicy(data) {
  return {
    pendingHoldMinutes: boundedIntegerOrDefault(data.pendingHoldMinutes, 15, 10080, 1440),
    cancellationWindowMinutes: boundedIntegerOrDefault(data.cancellationWindowMinutes, 0, 10080, 0),
    rescheduleWindowMinutes: boundedIntegerOrDefault(data.rescheduleWindowMinutes, 0, 10080, 0),
    paymentEligibilityCopy: cleanString(data.paymentEligibilityCopy, 300) ||
      "Pagamento confirmado no local apos validacao da marcacao.",
  };
}

function sanitizeBookingPolicyUpdate(data) {
  return {
    pendingHoldMinutes: boundedInteger(data.pendingHoldMinutes, 15, 10080, "Tempo de reserva pendente invalido."),
    cancellationWindowMinutes: boundedInteger(data.cancellationWindowMinutes, 0, 10080, "Janela de cancelamento invalida."),
    rescheduleWindowMinutes: boundedInteger(data.rescheduleWindowMinutes, 0, 10080, "Janela de remarcacao invalida."),
    paymentEligibilityCopy: cleanString(data.paymentEligibilityCopy, 300) ||
      "Pagamento confirmado no local apos validacao da marcacao.",
  };
}

function sanitizeLoyaltySettings(data) {
  const stampsRequired = Number.isFinite(Number(data.stampsRequired))
    ? Number(data.stampsRequired)
    : Number(data.targetWashes);
  return {
    stampsRequired: Number.isFinite(stampsRequired) ? Math.max(1, Math.min(50, Math.round(stampsRequired))) : 10,
    rewardType: cleanString(data.rewardType, 80) || "free_wash",
    rewardValue: Number.isFinite(Number(data.rewardValue)) ? Math.max(1, Math.round(Number(data.rewardValue))) : 1,
    rewardDescription: cleanString(data.rewardDescription, 120) || "1 lavagem gratis",
  };
}

function sanitizeLoyaltySettingsUpdate(data) {
  return {
    stampsRequired: boundedInteger(data.stampsRequired, 1, 50, "Numero de lavagens invalido."),
    rewardType: cleanString(data.rewardType, 80) || "free_wash",
    rewardValue: boundedInteger(data.rewardValue, 1, 100, "Valor de recompensa invalido."),
    rewardDescription: cleanString(data.rewardDescription, 120) || "1 lavagem gratis",
  };
}

function sanitizeNotificationCampaignDraft(data) {
  const title = cleanString(data.title, 120);
  const body = cleanString(data.body, 500);
  if (!title || !body) {
    throw new HttpsError("invalid-argument", "Indique titulo e mensagem da campanha.");
  }
  const targetAudience = normalizeCampaignAudience(data.targetAudience);
  const pushEnabled = data.pushEnabled !== false;
  if (!pushEnabled) {
    throw new HttpsError("invalid-argument", "Ative push para guardar a campanha.");
  }
  return {
    campaignId: cleanDocumentId(data.campaignId, 160),
    title,
    body,
    targetAudience,
    channels: ["push"],
    marketingConsentRequired: targetAudience === "marketing_opt_in_users",
    scheduledAtIso: cleanString(data.scheduledAtIso, 80),
    notes: cleanString(data.notes, 500),
  };
}

function normalizeCampaignAudience(value) {
  const audience = cleanString(value, 80).toLowerCase();
  if (["all_users", "marketing_opt_in_users", "admins"].includes(audience)) return audience;
  return "all_users";
}

function toNotificationCampaignDraftPayload(id, data) {
  const title = cleanString(data && data.title, 120);
  const body = cleanString(data && data.body, 500);
  if (!id || !title || !body) return null;
  const status = cleanString(data && data.status, 40) || "draft";
  const targetAudience = normalizeCampaignAudience(data && data.targetAudience);
  const locked = status === "sent" || status === "archived";
  return {
    campaignId: cleanString((data && data.campaignId) || id, 160),
    title,
    body,
    targetAudience,
    channels: Array.isArray(data && data.channels)
      ? data.channels.map((item) => cleanString(item, 40)).filter(Boolean)
      : ["push"],
    marketingConsentRequired: (data && data.marketingConsentRequired === true) ||
      targetAudience === "marketing_opt_in_users",
    status,
    scheduledAtIso: cleanString(data && data.scheduledAtIso, 80),
    notes: cleanString(data && data.notes, 500),
    sendBlocked: locked || data && data.sendBlocked === true,
    sendBlockedReason: cleanString(data && data.sendBlockedReason, 120),
    deliveryLocked: locked || data && data.deliveryLocked === true,
    sendState: cleanString(data && data.sendState, 40) || (locked ? status : "ready"),
    createdAtIso: firestoreDateToIso(data && data.createdAt),
    updatedAtIso: firestoreDateToIso(data && data.updatedAt),
    archivedAtIso: firestoreDateToIso(data && data.archivedAt),
    createdByUid: cleanString(data && data.createdByUid, 160),
    updatedByUid: cleanString(data && data.updatedByUid, 160),
    archivedByUid: cleanString(data && data.archivedByUid, 160),
    sentAtIso: firestoreDateToIso(data && data.sentAt),
    sentByUid: cleanString(data && data.sentByUid, 160),
    queuedCount: Number.isFinite(Number(data && data.queuedCount)) ? Math.max(0, Number(data.queuedCount)) : 0,
  };
}

function sanitizeCapacityOverrideUpsert(data) {
  return {
    date: sanitizeDateArgument(data.date),
    maxBookingsPerSlot: boundedInteger(data.maxBookingsPerSlot, 0, 20, "Capacidade invalida."),
  };
}

function sanitizeBlockedSlotUpsert(data) {
  const date = sanitizeDateArgument(data.date);
  const slotStart = cleanString(data.slotStart || data.slotStartIso, 80);
  const slotEnd = cleanString(data.slotEnd || data.slotEndIso, 80);
  if (!slotStart.startsWith(`${date}T`) || !slotEnd.startsWith(`${date}T`)) {
    throw new HttpsError("invalid-argument", "Horario bloqueado invalido.");
  }
  return {
    blockedSlotId: cleanDocumentId(data.blockedSlotId, 160),
    date,
    slotStart,
    slotEnd,
    reason: cleanString(data.reason, 200) || "Bloqueio administrativo",
  };
}

function sanitizeServiceCatalogUpsert(data) {
  const name = cleanString(data.name, 120);
  if (!name) {
    throw new HttpsError("invalid-argument", "Indique o nome do servico.");
  }
  return {
    serviceId: cleanDocumentId(data.serviceId, 160),
    name,
    description: cleanString(data.description, 500),
    durationMinutes: boundedInteger(data.durationMinutes, 5, 480, "Duracao do servico invalida."),
    passengerPriceCents: boundedInteger(data.passengerPriceCents, 0, 100000, "Preco invalido."),
    suvPriceCents: boundedInteger(data.suvPriceCents, 0, 100000, "Preco SUV invalido."),
    iconKey: cleanString(data.iconKey, 80) || "car",
    popular: data.popular === true,
    active: data.active !== false,
    sortOrder: boundedIntegerOrDefault(data.sortOrder, 0, 9999, 999),
  };
}

function sanitizeServiceExtraUpsert(data) {
  const name = cleanString(data.name, 120);
  if (!name) {
    throw new HttpsError("invalid-argument", "Indique o nome do extra.");
  }
  return {
    extraId: cleanDocumentId(data.extraId, 160),
    name,
    description: cleanString(data.description, 500),
    priceCents: boundedInteger(data.priceCents, 0, 100000, "Preco invalido."),
    iconKey: cleanString(data.iconKey, 80) || "auto_awesome",
    eligibleServiceIds: Array.isArray(data.eligibleServiceIds)
      ? [...new Set(data.eligibleServiceIds.map((item) => cleanDocumentId(item, 160)).filter(Boolean))]
      : [],
    active: data.active !== false,
    sortOrder: boundedIntegerOrDefault(data.sortOrder, 0, 9999, 999),
  };
}

function sanitizeDateArgument(value) {
  const date = cleanString(value, 10);
  if (!parseDateId(date)) {
    throw new HttpsError("invalid-argument", "Data invalida.");
  }
  return date;
}

function boundedInteger(value, min, max, message) {
  const number = Number(value);
  if (!Number.isInteger(number) || number < min || number > max) {
    throw new HttpsError("invalid-argument", message);
  }
  return number;
}

function boundedIntegerOrDefault(value, min, max, fallback) {
  const number = Number(value);
  if (!Number.isInteger(number)) return fallback;
  return Math.max(min, Math.min(max, number));
}

function toCapacityOverridePayload(id, data) {
  const date = cleanString((data && data.date) || id, 10);
  if (!parseDateId(date)) return null;
  return {
    date,
    maxBookingsPerSlot: boundedIntegerOrDefault(data && data.maxBookingsPerSlot, 0, 20, 0),
    updatedAtIso: firestoreDateToIso(data && data.updatedAt),
    updatedByUid: cleanString(data && data.updatedByUid, 160),
  };
}

function toBlockedSlotPayload(id, data) {
  const date = cleanString(data && data.date, 10);
  const slotStart = cleanString((data && (data.slotStart || data.slotStartIso)), 80);
  const slotEnd = cleanString((data && (data.slotEnd || data.slotEndIso)), 80);
  if (!parseDateId(date) || !slotStart || !slotEnd) return null;
  return {
    blockedSlotId: cleanString((data && data.blockedSlotId) || id, 160),
    id: cleanString((data && data.blockedSlotId) || id, 160),
    date,
    slotStart,
    slotEnd,
    reason: cleanString(data && data.reason, 200) || "Bloqueio administrativo",
    updatedAtIso: firestoreDateToIso(data && data.updatedAt),
    updatedByUid: cleanString(data && data.updatedByUid, 160),
  };
}

function toAdminServiceCatalogItem(id, data) {
  const item = toServiceCatalogItem(id, data);
  if (!item) return null;
  return {
    id: item.id,
    name: item.name,
    description: item.description,
    durationMinutes: item.durationMinutes,
    passengerPriceCents: item.passengerPriceCents,
    suvPriceCents: item.suvPriceCents,
    iconKey: item.iconKey,
    popular: item.popular,
    active: item.archived !== true,
    sortOrder: item.sortOrder,
    createdAtIso: firestoreDateToIso(data && data.createdAt),
    updatedAtIso: firestoreDateToIso(data && data.updatedAt),
    archivedAtIso: firestoreDateToIso(data && data.archivedAt),
    createdByUid: cleanString(data && data.createdByUid, 160),
    updatedByUid: cleanString(data && data.updatedByUid, 160),
    archivedByUid: cleanString(data && data.archivedByUid, 160),
  };
}

function toAdminServiceExtraItem(id, data) {
  const item = toServiceExtraItem(id, data);
  if (!item) return null;
  return {
    id: item.id,
    name: item.name,
    description: item.description,
    priceCents: item.priceCents,
    iconKey: item.iconKey,
    eligibleServiceIds: item.eligibleServiceIds,
    active: item.archived !== true,
    sortOrder: item.sortOrder,
    createdAtIso: firestoreDateToIso(data && data.createdAt),
    updatedAtIso: firestoreDateToIso(data && data.updatedAt),
    archivedAtIso: firestoreDateToIso(data && data.archivedAt),
    createdByUid: cleanString(data && data.createdByUid, 160),
    updatedByUid: cleanString(data && data.updatedByUid, 160),
    archivedByUid: cleanString(data && data.archivedByUid, 160),
  };
}

function toServiceCatalogItem(id, data) {
  const serviceId = cleanString((data && data.id) || id, 120);
  const name = cleanString(data && data.name, 120);
  const durationMinutes = Number(data && data.durationMinutes);
  if (!serviceId || !name || !Number.isFinite(durationMinutes)) return null;
  return {
    id: serviceId,
    name,
    description: cleanString(data.description, 500),
    durationMinutes: Math.max(5, Math.min(480, durationMinutes)),
    passengerPriceCents: nonNegativeInt(data.passengerPriceCents),
    suvPriceCents: nonNegativeInt(data.suvPriceCents),
    iconKey: cleanString(data.iconKey, 80) || "car",
    popular: data.popular === true,
    archived: data.archived === true,
    sortOrder: Number.isFinite(Number(data.sortOrder)) ? Number(data.sortOrder) : 0,
  };
}

function toServiceExtraItem(id, data) {
  const extraId = cleanString((data && data.id) || id, 120);
  const name = cleanString(data && data.name, 120);
  if (!extraId || !name) return null;
  return {
    id: extraId,
    name,
    description: cleanString(data.description, 500),
    priceCents: nonNegativeInt(data.priceCents),
    iconKey: cleanString(data.iconKey, 80) || "auto_awesome",
    eligibleServiceIds: Array.isArray(data.eligibleServiceIds)
      ? [...new Set(data.eligibleServiceIds.map((item) => cleanString(item, 120)).filter(Boolean))]
      : [],
    archived: data.archived === true,
    sortOrder: Number.isFinite(Number(data.sortOrder)) ? Number(data.sortOrder) : 0,
  };
}

function toOpeningHoursItem(value) {
  const dayLabel = cleanString(value && value.dayLabel, 80);
  const hoursLabel = cleanString(value && value.hoursLabel, 80);
  if (!dayLabel || !hoursLabel) return null;
  return {
    dayLabel,
    hoursLabel,
    closed: value.closed === true,
  };
}

function toBusinessOpeningHoursItem(value) {
  return toOpeningHoursItem(value);
}

function toFaqItem(value) {
  const question = cleanString(value && value.question, 240);
  const answer = cleanString(value && value.answer, 800);
  return question && answer ? { question, answer } : null;
}

function toStatItem(value) {
  const statValue = cleanString(value && value.value, 80);
  const label = cleanString(value && value.label, 120);
  return statValue && label ? { value: statValue, label } : null;
}

function toSocialLinkItem(value) {
  const label = cleanString(value && value.label, 80);
  const uri = cleanString(value && value.uri, 300);
  return label && uri ? { label, uri } : null;
}

function sortBySortOrderThenName(left, right) {
  return (left.sortOrder - right.sortOrder) || left.name.localeCompare(right.name);
}

function nonNegativeInt(value) {
  return Number.isFinite(Number(value)) ? Math.max(0, Math.round(Number(value))) : 0;
}

function parseDateId(dateId) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(dateId || ""))) return null;
  const date = new Date(`${dateId}T00:00:00.000Z`);
  return Number.isNaN(date.getTime()) || toDateId(date) !== dateId ? null : date;
}

function todayUtcDate() {
  const now = new Date();
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
}

function toDateId(date) {
  return [
    date.getUTCFullYear(),
    String(date.getUTCMonth() + 1).padStart(2, "0"),
    String(date.getUTCDate()).padStart(2, "0"),
  ].join("-");
}

function openingForDate(date, openingHours) {
  const day = date.getUTCDay();
  const label = dayLabelsPt[day];
  return openingHours.find((item) => item.dayLabel === label) ||
    openingHours.find((item) => item.dayLabel === "Segunda a Sexta" && day >= 1 && day <= 5) ||
    openingHours.find((item) => item.dayLabel === "Sabado" && day === 6) ||
    openingHours.find((item) => item.dayLabel === "Sábado" && day === 6) ||
    openingHours.find((item) => item.dayLabel === "Domingo" && day === 0) ||
    null;
}

const defaultServiceCatalogServices = [
  {
    id: "exterior",
    name: "Lavagem Exterior",
    description: "Lavagem exterior com acabamento cuidado.",
    durationMinutes: 20,
    passengerPriceCents: 1200,
    suvPriceCents: 1500,
    iconKey: "car",
    popular: false,
  },
  {
    id: "standard",
    name: "Lavagem Standard",
    description: "Exterior e interior para manutencao regular.",
    durationMinutes: 30,
    passengerPriceCents: 2200,
    suvPriceCents: 2500,
    iconKey: "local_car_wash",
    popular: true,
  },
  {
    id: "interior",
    name: "Limpeza Interior",
    description: "Aspiracao e detalhe interior.",
    durationMinutes: 25,
    passengerPriceCents: 1800,
    suvPriceCents: 2100,
    iconKey: "airline_seat_recline_normal",
    popular: false,
  },
  {
    id: "premium",
    name: "Lavagem Premium",
    description: "Servico completo com detalhe extra.",
    durationMinutes: 45,
    passengerPriceCents: 3200,
    suvPriceCents: 3400,
    iconKey: "sparkles",
    popular: true,
  },
];

const defaultServiceCatalogExtras = [
  {
    id: "wax",
    name: "Enceramento",
    description: "Protecao e brilho extra.",
    priceCents: 1500,
    iconKey: "shield",
    eligibleServiceIds: ["standard", "premium"],
  },
];

const defaultAvailabilityOpeningHours = [
  { dayLabel: "Segunda a Sexta", hoursLabel: "09:00 - 19:00", closed: false },
  { dayLabel: "Sabado", hoursLabel: "09:00 - 13:00", closed: false },
  { dayLabel: "Domingo", hoursLabel: "Encerrado", closed: true },
];

const defaultBusinessInfo = {
  phone: "913 005 855",
  phoneUri: "tel:913005855",
  email: "info@sudsshine.pt",
  emailUri: "mailto:info@sudsshine.pt",
  addressLine1: "Rua Virgilio Vieira da Cunha, R. Pte. das Mestras, 2400-447",
  addressLine2: "Leiria, Portugal",
  mapsUri: "https://www.google.com/maps/search/?api=1&query=Suds%20%26%20Shine%20Solutions%2C%20Leiria",
  whatsappUri: "https://wa.me/351913005855",
  openingHours: [
    { dayLabel: "Segunda a Sexta", hoursLabel: "09:00 - 19:00", closed: false },
    { dayLabel: "Sabado", hoursLabel: "09:00 - 13:00", closed: false },
    { dayLabel: "Domingo", hoursLabel: "Encerrado", closed: true },
  ],
  faq: [
    {
      question: "Como posso marcar uma lavagem?",
      answer: "Pode marcar atraves da app, escolhendo o servico, tipo de veiculo, data e hora.",
    },
    {
      question: "Como funciona o programa de fidelizacao?",
      answer: "A cada lavagem completa recebe 1 selo. Ao completar 10 selos ganha 1 lavagem gratis.",
    },
  ],
  stats: [
    { value: "500+", label: "Carros Tratados" },
    { value: "4.9", label: "Avaliacao Media" },
    { value: "3+", label: "Anos Experiencia" },
  ],
  socialLinks: [],
};

const monthLongPt = [
  "janeiro",
  "fevereiro",
  "marco",
  "abril",
  "maio",
  "junho",
  "julho",
  "agosto",
  "setembro",
  "outubro",
  "novembro",
  "dezembro",
];

const monthShortPt = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];
const dayLabelsPt = ["Domingo", "Segunda", "Terca", "Quarta", "Quinta", "Sexta", "Sabado"];
