#!/usr/bin/env bash
#
# Converts THIS vbwd-android repo into the dev "umbrella": replaces the inline
# core/ + plugins/* directories with **git submodules** of the split repos and
# rewrites settings.gradle.kts to **composite-build** them (includeBuild). Gradle
# then substitutes the published `com.vbwd:vbwd-android-*` coordinates with the
# local submodule builds, so `git clone --recurse-submodules && ./gradlew check`
# builds + tests every repo from source.
#
# RUN AFTER tools/create-android-repos.sh has created + pushed the split repos.
# This is destructive to the inline core/ + plugins/* dirs (they become
# submodules). Commit/branch first.
set -euo pipefail

OWNER="dantweb"
GROUP="com.vbwd"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
[ -f settings.gradle.kts ] || { echo "run from the vbwd-android repo"; exit 1; }

# path : repo-name
SUBS=$(cat <<'EOF'
core:vbwd-android-core
plugins/example:vbwd-android-example
plugins/subscription:vbwd-android-subscription
plugins/token-payment:vbwd-android-token-payment
plugins/stripe:vbwd-android-stripe
plugins/invoice:vbwd-android-invoice
plugins/cms:vbwd-android-cms
plugins/tarot:vbwd-android-tarot
plugins/meinchat:vbwd-android-meinchat
plugins/meinchat-plus:vbwd-android-meinchat-plus
EOF
)

# 1) inline dirs -> submodules (same paths, so build wiring is unchanged)
echo "$SUBS" | while IFS=: read -r dir repo; do
  [ -z "$dir" ] && continue
  echo ">> submodule $dir -> $OWNER/$repo"
  git rm -r --quiet --cached "$dir" 2>/dev/null || true
  rm -rf "$dir"
  git submodule add -f "https://github.com/$OWNER/$repo.git" "$dir"
done

# 2) settings: composite-build every submodule; keep the umbrella app inline.
cat > settings.gradle.kts <<'EOF'
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "vbwd-android"

// The umbrella's host app (consumes core + plugins by Maven coordinate;
// includeBuild below substitutes those with the local submodule builds).
include(":app")

// Composite builds — each submodule is a standalone Gradle build. Their
// published `com.vbwd:vbwd-android-*` coordinates are auto-substituted with the
// local source, so the whole tree builds + tests from source.
includeBuild("core")
includeBuild("plugins/example")
includeBuild("plugins/subscription")
includeBuild("plugins/token-payment")
includeBuild("plugins/stripe")
includeBuild("plugins/invoice")
includeBuild("plugins/cms")
includeBuild("plugins/tarot")
includeBuild("plugins/meinchat")
includeBuild("plugins/meinchat-plus")
EOF

# 3) app build: project(":...") -> Maven coordinates (includeBuild substitutes them)
sed -i.bak \
  -e "s#implementation(project(\":core\"))#implementation(\"$GROUP:vbwd-android-core:0.1.0\")#g" \
  -e "s#implementation(project(\":plugins:example\"))#implementation(\"$GROUP:vbwd-android-example:1.0.0\")#g" \
  -e "s#implementation(project(\":plugins:subscription\"))#implementation(\"$GROUP:vbwd-android-subscription:1.0.0\")#g" \
  -e "s#implementation(project(\":plugins:token-payment\"))#implementation(\"$GROUP:vbwd-android-token-payment:1.0.0\")#g" \
  -e "s#implementation(project(\":plugins:stripe\"))#implementation(\"$GROUP:vbwd-android-stripe:1.0.0\")#g" \
  -e "s#implementation(project(\":plugins:invoice\"))#implementation(\"$GROUP:vbwd-android-invoice:1.0.0\")#g" \
  -e "s#implementation(project(\":plugins:cms\"))#implementation(\"$GROUP:vbwd-android-cms:0.1.0\")#g" \
  -e "s#implementation(project(\":plugins:tarot\"))#implementation(\"$GROUP:vbwd-android-tarot:0.1.0\")#g" \
  -e "s#implementation(project(\":plugins:meinchat\"))#implementation(\"$GROUP:vbwd-android-meinchat:1.1.0\")#g" \
  -e "s#implementation(project(\":plugins:meinchat-plus\"))#implementation(\"$GROUP:vbwd-android-meinchat-plus:0.2.0\")#g" \
  app/build.gradle.kts
rm -f app/build.gradle.kts.bak

echo "Umbrella ready. Review app/build.gradle.kts + settings.gradle.kts, then:"
echo "  git add -A && git commit -m 'umbrella: core + plugins as submodules (composite build)'"
echo "  ./gradlew check    # builds every submodule from source via includeBuild"
