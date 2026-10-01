#!/usr/bin/env bash
set -euo pipefail

BASE="${1:-}"
HEAD_REF="${2:-HEAD}"
OP="${3:-..}"
OUT="${GITHUB_OUTPUT:-/dev/stdout}"

if [[ -z "$BASE" || "$BASE" =~ ^0+$ ]] || ! git cat-file -e "$BASE^{commit}" 2>/dev/null; then
  changed=$(git ls-tree -d --name-only "$HEAD_REF")
else
  changed=$(git diff --name-only "${BASE}${OP}${HEAD_REF}" | grep '/' | cut -d/ -f1 | sort -u || true)
fi

dirs=""
for d in $changed; do
  if [ -f "$d/pom.xml" ] || [ -f "$d/build.gradle" ] || [ -f "$d/build.gradle.kts" ]; then
    dirs+="$d"$'\n'
  fi
done

matrix=$(printf '%s' "$dirs" | jq -R -s -c 'split("\n") | map(select(length>0))')
echo "matrix=$matrix" >> "$OUT"
if [ "$matrix" = "[]" ]; then echo "has=false" >> "$OUT"; else echo "has=true" >> "$OUT"; fi
echo "Changed services: $matrix" >&2