#!/usr/bin/env bash
set -euo pipefail

echo "Functions deploy blocked: SudsAndShineFirebase/functions is the canonical production backend." >&2
echo "Deploy functions from ../SudsAndShineFirebase instead. App/functions is retained for local compatibility only." >&2
exit 1
