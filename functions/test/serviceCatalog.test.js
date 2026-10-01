"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const { mergeCatalogDocuments } = require("../src/serviceCatalog");

test("editing one service keeps the other default services available", () => {
  const defaults = [
    { id: "exterior", name: "Exterior", passengerPriceCents: 1600 },
    { id: "standard", name: "Standard", passengerPriceCents: 2000 },
    { id: "premium", name: "Premium", passengerPriceCents: 3200 },
  ];

  const items = mergeCatalogDocuments(defaults, [
    { id: "standard", data: { name: "Standard", passengerPriceCents: 2500 } },
  ]);

  assert.deepEqual(items.map((item) => item.id), ["exterior", "standard", "premium"]);
  assert.equal(items.find((item) => item.id === "standard").passengerPriceCents, 2500);
  assert.equal(items.find((item) => item.id === "exterior").passengerPriceCents, 1600);
});

test("archived default items stay archived even when only a tombstone was saved", () => {
  const items = mergeCatalogDocuments(
    [{ id: "odor-removal", name: "Remoção de odores" }],
    [{ id: "odor-removal", data: { archived: true } }],
  );

  assert.deepEqual(items, [{ id: "odor-removal", name: "Remoção de odores", archived: true }]);
  assert.equal(items.filter((item) => item.archived !== true).length, 0);
});

test("new catalog items are added alongside defaults", () => {
  const items = mergeCatalogDocuments(
    [{ id: "exterior", name: "Exterior" }],
    [{ id: "custom", data: { name: "Personalizado" } }],
  );

  assert.deepEqual(items.map((item) => item.id), ["exterior", "custom"]);
});
