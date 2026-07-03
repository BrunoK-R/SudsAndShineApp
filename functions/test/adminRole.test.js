"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const { adminRoleDocumentPatch } = require("../src/adminRole");

test("admin role sync creates a Firestore patch for push discovery", () => {
  assert.deepEqual(
    adminRoleDocumentPatch("admin", { email: " admin@example.test " }),
    {
      role: "admin",
      email: "admin@example.test",
      adminRoleSynced: true,
    },
  );
});

test("admin role sync can use callable token email", () => {
  assert.deepEqual(
    adminRoleDocumentPatch("admin", {}, { email: "claim-admin@example.test" }),
    {
      role: "admin",
      email: "claim-admin@example.test",
      adminRoleSynced: true,
    },
  );
});

test("non-admin roles do not create a role persistence patch", () => {
  assert.equal(adminRoleDocumentPatch("customer", { email: "user@example.test" }), null);
  assert.equal(adminRoleDocumentPatch("", { email: "user@example.test" }), null);
});
