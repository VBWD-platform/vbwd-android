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

OWNER="vbwd-platform"
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

# 1b) Seed the Android SDK location into each submodule. Composite (includeBuild)
# submodules are standalone Gradle builds; each reads its OWN local.properties (or
# ANDROID_HOME) — without it the build fails "SDK location not found". Copy the
# umbrella's sdk.dir down. local.properties is gitignored (machine-specific), so
# this is a local convenience, not committed; ANDROID_HOME is the durable option.
if [ -f local.properties ] && grep -q '^sdk.dir=' local.properties; then
  sdkdir="$(grep '^sdk.dir=' local.properties | head -1)"
  echo "$SUBS" | while IFS=: read -r dir repo; do
    [ -z "$dir" ] && continue
    printf '%s\n' "$sdkdir" > "$dir/local.properties"
  done
  echo ">> seeded submodule local.properties from the umbrella sdk.dir"
fi

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

// Composite builds — each submodule is a standalone Gradle build whose project
// is named after its directory (:core, :example, …), NOT its published
// `com.vbwd:vbwd-android-*` artifact. Automatic substitution can't match those,
// so each includeBuild declares an explicit substitution from the Maven
// coordinate to the submodule's project (this also covers meinchat-plus's
// dependency on meinchat), letting the tree build + test from source.
includeBuild("core") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-core")).using(project(":core"))
    }
}
includeBuild("plugins/example") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-example")).using(project(":example"))
    }
}
includeBuild("plugins/subscription") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-subscription")).using(project(":subscription"))
    }
}
includeBuild("plugins/token-payment") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-token-payment")).using(project(":token-payment"))
    }
}
includeBuild("plugins/stripe") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-stripe")).using(project(":stripe"))
    }
}
includeBuild("plugins/invoice") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-invoice")).using(project(":invoice"))
    }
}
includeBuild("plugins/cms") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-cms")).using(project(":cms"))
    }
}
includeBuild("plugins/tarot") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-tarot")).using(project(":tarot"))
    }
}
includeBuild("plugins/meinchat") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-meinchat")).using(project(":meinchat"))
    }
}
includeBuild("plugins/meinchat-plus") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-meinchat-plus")).using(project(":meinchat-plus"))
    }
}
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
