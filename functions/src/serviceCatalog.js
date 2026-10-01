"use strict";

function mergeCatalogDocuments(defaultItems, persistedDocuments) {
  const merged = new Map(defaultItems.map((item) => [item.id, item]));

  for (const document of persistedDocuments) {
    merged.set(document.id, {
      ...merged.get(document.id),
      ...document.data,
      id: document.id,
    });
  }

  return [...merged.values()];
}

module.exports = { mergeCatalogDocuments };
