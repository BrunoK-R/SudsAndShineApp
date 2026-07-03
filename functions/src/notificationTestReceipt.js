"use strict";

function deliveryStateForSend(tokenCount, sentCount, failedCount) {
  const tokens = Number(tokenCount) || 0;
  const sent = Number(sentCount) || 0;
  const failed = Number(failedCount) || 0;
  if (sent > 0) return "sent";
  if (failed > 0) return "failed";
  return tokens > 0 ? "queued" : "no_recipients";
}

function selfTestDeliveryMessage(tokenCount, sentCount) {
  const tokens = Number(tokenCount) || 0;
  const sent = Number(sentCount) || 0;
  if (sent > 0) return "Teste enviado apenas para o administrador atual.";
  if (tokens === 0) return "Este administrador ainda nao tem um dispositivo ativo para notificacoes.";
  return "Nao foi possivel entregar o teste ao dispositivo atual.";
}

module.exports = {
  deliveryStateForSend,
  selfTestDeliveryMessage,
};
