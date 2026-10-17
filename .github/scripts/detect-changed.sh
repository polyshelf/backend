#!/usr/bin/env bash
set -euo pipefail

BASE="${1:-}"
HEAD_REF="${2:-HEAD}"
OP="${3:-..}"
OUT="${GITHUB_OUTPUT:-/dev/stdout}"

all_modules() {
  git ls-tree -d --name-only "$HEAD_REF" | while read -r d; do
    git cat-file -e "$HEAD_REF:$d/build.gradle.kts" 2>/dev/null && echo "$d"
  done || true
}

if [[ -z "$BASE" || "$BASE" =~ ^0+$ ]] || ! git cat-file -e "$BASE^{commit}" 2>/dev/null; then
  dirs=$(all_modules)
else
  files=$(git diff --name-only "${BASE}${OP}${HEAD_REF}")
  if grep -qE '^(gradle/|settings\.gradle\.kts$|build\.gradle\.kts$|gradlew(\.bat)?$|flyway/|\.github/)' <<<"$files"; then
    dirs=$(all_modules)
  else
    dirs=$(grep '/' <<<"$files" | cut -d/ -f1 | sort -u | while read -r d; do
      git cat-file -e "$HEAD_REF:$d/build.gradle.kts" 2>/dev/null && echo "$d"
    done || true)
  fi
fi

matrix=$(printf '%s\n' "$dirs" | jq -R -s -c 'split("\n") | map(select(length>0))')
echo "matrix=$matrix" >> "$OUT"
if [ "$matrix" = "[]" ]; then echo "has=false" >> "$OUT"; else echo "has=true" >> "$OUT"; fi
echo "Changed services: $matrix" >&2
