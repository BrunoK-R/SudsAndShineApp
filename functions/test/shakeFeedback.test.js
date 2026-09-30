"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const { normalizeShakeFeedback, SCREENSHOT_MAX_BYTES } = require("../src/shakeFeedback");

test("normalizes a valid feedback submission with screenshot", () => {
  const result = normalizeShakeFeedback({
    title: "  Botão não responde  ", body: " Acontece na reserva ", platform: "ANDROID",
    screenshot: { base64: Buffer.from([0xff, 0xd8, 0xff, 0xd9]).toString("base64"), mimeType: "image/jpeg", widthPx: 100, heightPx: 200 },
  });
  assert.equal(result.title, "Botão não responde");
  assert.equal(result.body, "Acontece na reserva");
  assert.equal(result.platform, "android");
  assert.equal(result.screenshot.bytes.length, 4);
});

test("rejects invalid fields and oversized screenshots", () => {
  assert.equal(normalizeShakeFeedback({ title: " ", platform: "ios" }), null);
  assert.equal(normalizeShakeFeedback({ title: "ok", platform: "web" }), null);
  assert.equal(normalizeShakeFeedback({ title: "ok", platform: "ios", screenshot: {
    base64: Buffer.alloc(SCREENSHOT_MAX_BYTES + 1).toString("base64"), mimeType: "image/jpeg", widthPx: 10, heightPx: 10,
  } }), null);
});
