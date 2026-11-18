#!/usr/bin/env bash
# SketchPilot hard-constraint guard. Run before every delivery.
# Fails on forbidden Java constructs and any Kotlin source file.
# Scans only project source directories (the workspace root is a shared filesystem root).
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 2

# Project-owned directories only. Never scan system/scratch dirs that live beside the project.
SOURCE_DIRS=(
  app core project blocks view agent provider tools mcp ui util
  src tests test scripts docs
)

dirs=()
for d in "${SOURCE_DIRS[@]}"; do
  if [ -d "$d" ]; then
    dirs+=("$d")
  fi
done

failures=0

if [ "${#dirs[@]}" -eq 0 ]; then
  echo "FAIL: no project source directories found; cannot enforce constraints."
  exit 2
fi

# 1) No Kotlin sources anywhere in project-owned directories.
kt_files="$(find "${dirs[@]}" -type f -name '*.kt' -not -path '*/build/*' | sort)"
if [ -n "$kt_files" ]; then
  echo "FAIL: Kotlin source files are not allowed:"
  echo "$kt_files"
  failures=$((failures + 1))
fi

# 2) Forbidden constructs in Java sources.
mapfile -d '' java_files < <(find "${dirs[@]}" -type f -name '*.java' \
  -not -path '*/build/*' -print0 | sort -z)

patterns=(
  '->'
  '::'
  'try ('
  'Runtime.getRuntime().exec'
  'ExecutorService'
  'Executors'
  'CompletableFuture'
  'AsyncTask'
)

if [ "${#java_files[@]}" -eq 0 ]; then
  echo "NOTE: no Java sources yet (Phase 0); Java constraint scan is a no-op."
else
  for pattern in "${patterns[@]}"; do
    hits="$(grep -Fn -- "$pattern" "${java_files[@]}" 2>/dev/null || true)"
    if [ -n "$hits" ]; then
      echo "FAIL: forbidden pattern '$pattern':"
      echo "$hits"
      failures=$((failures + 1))
    fi
  done
fi

if [ "$failures" -ne 0 ]; then
  echo "check_constraints.sh: FAILED ($failures rule group(s) violated)."
  exit 1
fi

echo "check_constraints.sh: PASS (Java-only, Thread+Handler, ProcessBuilder, no forbidden constructs)."
exit 0
