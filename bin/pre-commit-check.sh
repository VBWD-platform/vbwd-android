#!/usr/bin/env bash
#
# pre-commit-check.sh — the vbwd-android quality gate.
#
# Android port of the backend/iOS `bin/pre-commit-check.sh`. Runs from the
# umbrella root so the composite build resolves core + every plugin submodule
# from local source (the `com.vbwd:vbwd-android-*` coordinates are substituted
# by includeBuild). Maps the gate phases onto Gradle tasks across all modules:
#
#   stylecheck   ktlintCheck + detekt + Android lint + dependencyBoundaryCheck
#   unit         JVM unit tests           (testDebugUnitTest)
#   integration  instrumented tests       (connectedDebugAndroidTest — needs a
#                = ux / e2e                 running emulator or device)
#
# Usage:  bin/pre-commit-check.sh [flags]
#
#   (no flag)          quick gate: stylecheck + unit          (the iterate loop)
#   --full             stylecheck + unit + integration        (the merge gate)
#   --quick            stylecheck + unit                      (explicit default)
#   --unit             unit tests only
#   --lint, --style    stylecheck only (ktlint + detekt + lint + boundary)
#   --integration      instrumented (integration/ux/e2e) only
#   --no-stylecheck    drop the stylecheck phase from whatever is selected
#   --plugin <name>    scope to a single module (core | app | <plugin-dir>)
#   -h, --help         this help
#
# Exit code is non-zero if any selected phase fails (all phases still run so the
# report is complete — `--continue` semantics).
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
[ -f settings.gradle.kts ] || { echo "run from the vbwd-android umbrella root"; exit 1; }

# name | gradle composite task prefix | module dir (for androidTest detection)
MODULES=(
    "app|:app|app"
    "core|:core:core|core/core"
    "example|:example:example|plugins/example/example"
    "subscription|:subscription:subscription|plugins/subscription/subscription"
    "token-payment|:token-payment:token-payment|plugins/token-payment/token-payment"
    "stripe|:stripe:stripe|plugins/stripe/stripe"
    "invoice|:invoice:invoice|plugins/invoice/invoice"
    "cms|:cms:cms|plugins/cms/cms"
    "tarot|:tarot:tarot|plugins/tarot/tarot"
    "meinchat|:meinchat:meinchat|plugins/meinchat/meinchat"
    "meinchat-plus|:meinchat-plus:meinchat-plus|plugins/meinchat-plus/meinchat-plus"
)

# ---- args -----------------------------------------------------------------
RUN_STYLE=1; RUN_UNIT=1; RUN_INTEGRATION=0; ONLY_PLUGIN=""
while [ $# -gt 0 ]; do
    case "$1" in
        --full)                 RUN_STYLE=1; RUN_UNIT=1; RUN_INTEGRATION=1 ;;
        --quick)                RUN_STYLE=1; RUN_UNIT=1; RUN_INTEGRATION=0 ;;
        --unit)                 RUN_STYLE=0; RUN_UNIT=1; RUN_INTEGRATION=0 ;;
        --lint|--style|--stylecheck) RUN_STYLE=1; RUN_UNIT=0; RUN_INTEGRATION=0 ;;
        --integration|--instrumented|--e2e|--ux) RUN_STYLE=0; RUN_UNIT=0; RUN_INTEGRATION=1 ;;
        --no-stylecheck)        RUN_STYLE=0 ;;
        --plugin)               shift; ONLY_PLUGIN="${1:-}"; [ -z "$ONLY_PLUGIN" ] && { echo "--plugin needs a name"; exit 1; } ;;
        -h|--help)              sed -n '2,40p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)                      echo "unknown flag: $1 (try --help)"; exit 1 ;;
    esac
    shift
done

# ---- JDK 17 (AGP/Gradle do not run on 23+) --------------------------------
# Verify a candidate actually IS 17 — `/usr/libexec/java_home -v 17` returns the
# newest JDK >= 17 (e.g. 26), so every candidate is version-checked.
is_jdk17() { [ -x "$1/bin/java" ] && "$1/bin/java" -version 2>&1 | grep -q 'version "17'; }
detect_jdk17() {
    is_jdk17 "${JAVA_HOME:-}" && return
    local h
    for h in \
        /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
        "$(/usr/libexec/java_home -v 17 2>/dev/null)" \
        /usr/lib/jvm/temurin-17-jdk /usr/lib/jvm/java-17-openjdk; do
        if is_jdk17 "$h"; then export JAVA_HOME="$h"; return; fi
    done
    echo "WARNING: no JDK 17 found — Gradle may fail on a newer JDK. Set JAVA_HOME to a 17." >&2
}
detect_jdk17

# ---- build the module set + task list -------------------------------------
selected_modules() {
    local entry name
    for entry in "${MODULES[@]}"; do
        name="${entry%%|*}"
        if [ -z "$ONLY_PLUGIN" ] || [ "$ONLY_PLUGIN" = "$name" ]; then echo "$entry"; fi
    done
}

[ -n "$ONLY_PLUGIN" ] && [ -z "$(selected_modules)" ] && { echo "unknown module: $ONLY_PLUGIN"; exit 1; }

TASKS=()
add_module_task() {  # $1 = task name → append "<prefix>:<task>" for each selected module
    local entry prefix
    while IFS='|' read -r _name prefix _dir; do
        TASKS+=("${prefix}:${1}")
    done < <(selected_modules)
}

if [ "$RUN_STYLE" = 1 ]; then
    add_module_task ktlintCheck
    add_module_task detekt
    add_module_task lintDebug
    TASKS+=("dependencyBoundaryCheck")   # root task — core-agnosticism boundary
fi
[ "$RUN_UNIT" = 1 ] && add_module_task testDebugUnitTest
if [ "$RUN_INTEGRATION" = 1 ]; then
    # Only modules that actually have an androidTest source set get a connected task.
    while IFS='|' read -r _name prefix dir; do
        [ -d "$dir/src/androidTest" ] && TASKS+=("${prefix}:connectedDebugAndroidTest")
    done < <(selected_modules)
fi

if [ ${#TASKS[@]} -eq 0 ]; then echo "nothing selected"; exit 0; fi

# ---- run ------------------------------------------------------------------
phases=""
[ "$RUN_STYLE" = 1 ] && phases="$phases stylecheck"
[ "$RUN_UNIT" = 1 ] && phases="$phases unit"
[ "$RUN_INTEGRATION" = 1 ] && phases="$phases integration"
echo "== vbwd-android gate ==${ONLY_PLUGIN:+  [module: $ONLY_PLUGIN]}"
echo "   phases:$phases"
echo "   java:   ${JAVA_HOME:-<system>}"
echo "   tasks:  ${#TASKS[@]}"
echo

# --continue: run every task so the report is complete; --no-configuration-cache:
# the Kotlin classpath-snapshot bean does not serialise cleanly in this composite.
./gradlew "${TASKS[@]}" --continue --no-configuration-cache
status=$?

echo
if [ $status -eq 0 ]; then
    echo "✅ gate PASSED ($phases )"
else
    echo "❌ gate FAILED — see the task output / reports above (exit $status)"
fi
exit $status
