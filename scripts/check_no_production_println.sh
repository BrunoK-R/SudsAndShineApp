#!/usr/bin/env bash
set -euo pipefail

if ! command -v rg >/dev/null 2>&1; then
  echo "ripgrep (rg) is required for check_no_production_println.sh" >&2
  exit 1
fi

matches="$(rg --line-number --glob '!**/build/**' 'println\\(' . || true)"
if [[ -n "$matches" ]]; then
  echo "Found println() calls:" >&2
  echo "$matches" >&2
  exit 1
fi

echo "No println() calls found."
