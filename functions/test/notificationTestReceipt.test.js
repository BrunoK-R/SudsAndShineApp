"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {
  deliveryStateForSend,
  selfTestDeliveryMessage,
} = require("../src/notificationTestReceipt");

test("self test delivery state reflects sent, failed, queued, and missing token cases", () => {
  assert.equal(deliveryStateForSend(1, 1, 0), "sent");
  assert.equal(deliveryStateForSend(1, 0, 1), "failed");
  assert.equal(deliveryStateForSend(1, 0, 0), "queued");
  assert.equal(deliveryStateForSend(0, 0, 0), "no_recipients");
});

test("self test delivery message explains the real next action", () => {
  assert.equal(
    selfTestDeliveryMessage(1, 1),
    "Teste enviado apenas para o administrador atual.",
  );
  assert.equal(
    selfTestDeliveryMessage(0, 0),
    "Este administrador ainda nao tem um dispositivo ativo para notificacoes.",
  );
  assert.equal(
    selfTestDeliveryMessage(1, 0),
    "Nao foi possivel entregar o teste ao dispositivo atual.",
  );
});
