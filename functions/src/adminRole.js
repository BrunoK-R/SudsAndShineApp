"use strict";

function adminRoleDocumentPatch(role, user = {}, token = {}) {
  if (cleanString(role, 40).toLowerCase() !== "admin") return null;
  return {
    role: "admin",
    email: cleanString(user.email || token.email, 160),
    adminRoleSynced: true,
  };
}

function cleanString(value, maxLength) {
  return String(value || "")
    .trim()
    .replace(/\s+/g, " ")
    .slice(0, maxLength);
}

module.exports = {
  adminRoleDocumentPatch,
};
