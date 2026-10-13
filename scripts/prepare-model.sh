#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FILE="$ROOT/app/src/main/assets/qwen2.5-1.5b-instruct-q4_k_m.gguf"
URL="https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
if [ -f "$FILE" ] && [ "$(stat -c%s "$FILE")" -gt 1000000000 ]; then
  echo "GGUF already present: $FILE"; exit 0
fi
mkdir -p "$(dirname "$FILE")"
echo "Downloading official Qwen2.5 1.5B Instruct Q4_K_M (~1.12 GB)..."
curl --fail --location --retry 3 --continue-at - --output "$FILE.part" "$URL"
BYTES=$(stat -c%s "$FILE.part")
if [ "$BYTES" -lt 1000000000 ]; then
  echo "Download appears incomplete ($BYTES bytes)." >&2; exit 1
fi
mv "$FILE.part" "$FILE"
echo "Ready: $FILE"
