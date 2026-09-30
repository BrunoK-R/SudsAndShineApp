"use strict";

const TITLE_MAX_LENGTH = 120;
const BODY_MAX_LENGTH = 4000;
const SCREENSHOT_MAX_BYTES = 5 * 1024 * 1024;
const SCREENSHOT_MIME_TYPES = { "image/jpeg": "jpg", "image/png": "png", "image/webp": "webp" };

function normalizeShakeFeedback(data = {}) {
  const title = typeof data.title === "string" ? data.title.trim() : "";
  const body = typeof data.body === "string" ? data.body.trim() : "";
  const platform = typeof data.platform === "string" ? data.platform.trim().toLowerCase() : "";
  if (!title || title.length > TITLE_MAX_LENGTH || body.length > BODY_MAX_LENGTH || !["android", "ios"].includes(platform)) {
    return null;
  }
  const screenshot = normalizeScreenshot(data.screenshot);
  if (data.screenshot != null && !screenshot) return null;
  return { title, body, platform, screenshot };
}

function normalizeScreenshot(input) {
  if (input == null) return null;
  if (typeof input !== "object" || Array.isArray(input)) return null;
  const mimeType = input.mimeType;
  if (!Object.hasOwn(SCREENSHOT_MIME_TYPES, mimeType) || typeof input.base64 !== "string") return null;
  if (input.base64.length > Math.ceil(SCREENSHOT_MAX_BYTES / 3) * 4 ||
      input.base64.length % 4 !== 0 || !/^[A-Za-z0-9+/]*={0,2}$/.test(input.base64)) return null;
  const bytes = Buffer.from(input.base64, "base64");
  if (!bytes.length || bytes.length > SCREENSHOT_MAX_BYTES || !Number.isInteger(input.widthPx) ||
      !Number.isInteger(input.heightPx) || input.widthPx <= 0 || input.heightPx <= 0) return null;
  if (!isSupportedImage(bytes, mimeType)) return null;
  return { bytes, mimeType, widthPx: input.widthPx, heightPx: input.heightPx,
    extension: SCREENSHOT_MIME_TYPES[mimeType] };
}

function isSupportedImage(bytes, mimeType) {
  if (mimeType === "image/jpeg") return bytes.length >= 4 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff;
  if (mimeType === "image/png") return bytes.length >= 8 && bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]));
  return bytes.length >= 12 && bytes.toString("ascii", 0, 4) === "RIFF" && bytes.toString("ascii", 8, 12) === "WEBP";
}

module.exports = { normalizeShakeFeedback, TITLE_MAX_LENGTH, BODY_MAX_LENGTH, SCREENSHOT_MAX_BYTES };
