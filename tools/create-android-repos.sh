#!/usr/bin/env bash
#
# Splits the vbwd-android monorepo into standalone public GitHub repos:
#   vbwd-android-core  +  one repo per plugin  +  vbwd-android-template (the app).
# Plugins are consumed as GitHub Packages Maven artifacts (com.vbwd:vbwd-android-*).
#
# RUN THIS ONLY AFTER `./gradlew check` IS GREEN (see report 14). It creates and
# PUSHES public repos under $OWNER — review the generated build files first.
#
# Usage:   tools/create-android-repos.sh [OUT_DIR]
#   OUT_DIR defaults to ../vbwd-android-repos (sibling of this repo).
#
# Requires: gh (authed), git. Set OWNER below.
set -euo pipefail

OWNER="vbwd-platform"
GROUP="com.vbwd"
MONO="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$MONO/../vbwd-android-repos}"
VISIBILITY="--public"

[ -f "$MONO/settings.gradle.kts" ] || { echo "run from the vbwd-android repo"; exit 1; }
mkdir -p "$OUT"

# module-dir | repo-name | maven-artifact | version | space-sep upstream artifacts
MODULES=$(cat <<'EOF'
core|vbwd-android-core|vbwd-android-core|0.1.0|
plugins/example|vbwd-android-example|vbwd-android-example|1.0.0|vbwd-android-core
plugins/subscription|vbwd-android-subscription|vbwd-android-subscription|1.0.0|vbwd-android-core
plugins/token-payment|vbwd-android-token-payment|vbwd-android-token-payment|1.0.0|vbwd-android-core
plugins/stripe|vbwd-android-stripe|vbwd-android-stripe|1.0.0|vbwd-android-core
plugins/invoice|vbwd-android-invoice|vbwd-android-invoice|1.0.0|vbwd-android-core
plugins/cms|vbwd-android-cms|vbwd-android-cms|0.1.0|vbwd-android-core
plugins/tarot|vbwd-android-tarot|vbwd-android-tarot|0.1.0|vbwd-android-core
plugins/meinchat|vbwd-android-meinchat|vbwd-android-meinchat|1.1.0|vbwd-android-core
plugins/meinchat-plus|vbwd-android-meinchat-plus|vbwd-android-meinchat-plus|0.2.0|vbwd-android-core vbwd-android-meinchat
app|vbwd-android-app-example|||vbwd-android-core vbwd-android-example vbwd-android-subscription vbwd-android-token-payment vbwd-android-stripe vbwd-android-invoice vbwd-android-cms vbwd-android-tarot vbwd-android-meinchat vbwd-android-meinchat-plus
EOF
)

# Maps a repo name to its published artifact + version (for dep rewriting).
artifact_coord() {
  case "$1" in
    vbwd-android-core) echo "$GROUP:vbwd-android-core:0.1.0" ;;
    vbwd-android-example) echo "$GROUP:vbwd-android-example:1.0.0" ;;
    vbwd-android-subscription) echo "$GROUP:vbwd-android-subscription:1.0.0" ;;
    vbwd-android-token-payment) echo "$GROUP:vbwd-android-token-payment:1.0.0" ;;
    vbwd-android-stripe) echo "$GROUP:vbwd-android-stripe:1.0.0" ;;
    vbwd-android-invoice) echo "$GROUP:vbwd-android-invoice:1.0.0" ;;
    vbwd-android-cms) echo "$GROUP:vbwd-android-cms:0.1.0" ;;
    vbwd-android-tarot) echo "$GROUP:vbwd-android-tarot:0.1.0" ;;
    vbwd-android-meinchat) echo "$GROUP:vbwd-android-meinchat:1.1.0" ;;
    vbwd-android-meinchat-plus) echo "$GROUP:vbwd-android-meinchat-plus:0.2.0" ;;
  esac
}

gh_maven_repos_block() {  # $* = upstream repo names → emit maven{} blocks
  for up in "$@"; do
    cat <<EOF
        maven {
            name = "gpr-$up"
            url = uri("https://maven.pkg.github.com/$OWNER/$up")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: (providers.gradleProperty("gpr.user").orNull ?: "")
                password = System.getenv("GITHUB_TOKEN") ?: (providers.gradleProperty("gpr.key").orNull ?: "")
            }
        }
EOF
  done
}

scaffold() {
  local moduledir="$1" repo="$2" upstreams="$3"
  local name; name="$(basename "$moduledir")"
  local dest="$OUT/$repo"
  echo ">> $repo  (from $moduledir)"
  rm -rf "$dest"; mkdir -p "$dest"

  # shared gradle bits (wrapper + version catalog + detekt config + editorconfig)
  cp -R "$MONO/gradle" "$dest/gradle"
  cp "$MONO/gradlew" "$MONO/gradlew.bat" "$MONO/gradle.properties" "$dest/"
  cp -R "$MONO/config" "$dest/config"
  cp "$MONO/.editorconfig" "$dest/" 2>/dev/null || true
  cp -R "$MONO/$moduledir" "$dest/$name"

  # root build (plugin aliases, applied per-module)
  cat > "$dest/build.gradle.kts" <<'EOF'
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.android.junit5) apply false
    alias(libs.plugins.roborazzi) apply false
}
EOF

  # settings (single module + GitHub Packages consumer repos for upstreams)
  {
    cat <<EOF
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
EOF
    gh_maven_repos_block $upstreams
    cat <<EOF
    }
}
rootProject.name = "$repo"
include(":$name")
EOF
  } > "$dest/settings.gradle.kts"

  # rewrite project() deps → Maven coordinates in the module build
  local mb="$dest/$name/build.gradle.kts"
  sed -i.bak \
    -e "s#implementation(project(\":core\"))#implementation(\"$(artifact_coord vbwd-android-core)\")#g" \
    -e "s#implementation(project(\":plugins:meinchat\"))#implementation(\"$(artifact_coord vbwd-android-meinchat)\")#g" \
    -e "s#implementation(project(\":plugins:example\"))#implementation(\"$(artifact_coord vbwd-android-example)\")#g" \
    -e "s#implementation(project(\":plugins:subscription\"))#implementation(\"$(artifact_coord vbwd-android-subscription)\")#g" \
    -e "s#implementation(project(\":plugins:token-payment\"))#implementation(\"$(artifact_coord vbwd-android-token-payment)\")#g" \
    -e "s#implementation(project(\":plugins:stripe\"))#implementation(\"$(artifact_coord vbwd-android-stripe)\")#g" \
    -e "s#implementation(project(\":plugins:invoice\"))#implementation(\"$(artifact_coord vbwd-android-invoice)\")#g" \
    -e "s#implementation(project(\":plugins:cms\"))#implementation(\"$(artifact_coord vbwd-android-cms)\")#g" \
    -e "s#implementation(project(\":plugins:tarot\"))#implementation(\"$(artifact_coord vbwd-android-tarot)\")#g" \
    -e "s#implementation(project(\":plugins:meinchat-plus\"))#implementation(\"$(artifact_coord vbwd-android-meinchat-plus)\")#g" \
    "$mb"
  rm -f "$mb.bak"

  # README
  cat > "$dest/README.md" <<EOF
# $repo

Part of the **vbwd-android** SDK — the Kotlin/Compose/Hilt port of the vbwd-ios
plugin-host platform. Module \`$name\`.

## Dependency
Consumes the core SDK as a GitHub Packages artifact:
\`$(artifact_coord vbwd-android-core)\`.
Set \`gpr.user\`/\`gpr.key\` (a PAT with \`read:packages\`) in
\`~/.gradle/gradle.properties\`, or \`GITHUB_ACTOR\`/\`GITHUB_TOKEN\` in the env.

## Build & test
\`\`\`bash
./gradlew check        # ktlint + detekt + unit tests
\`\`\`

## Docs
See \`docs/\`. License: BSL 1.1.
EOF

  # docs skeleton (preserve any module docs already copied under $name/docs)
  mkdir -p "$dest/docs"
  [ -f "$dest/docs/README.md" ] || cat > "$dest/docs/README.md" <<EOF
# $repo — docs

Architecture and usage notes for the \`$name\` module. See the monorepo
\`docs/dev_log/\` for the original sprint reports.
EOF

  # CI
  mkdir -p "$dest/.github/workflows"
  cat > "$dest/.github/workflows/ci.yml" <<'EOF'
name: ci
on:
  push:
    branches: [main]
  pull_request:
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
      - uses: android-actions/setup-android@v3
      - uses: gradle/actions/setup-gradle@v4
      - name: ./gradlew check
        run: ./gradlew check --stacktrace
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
EOF

  # core also gets a publish-on-tag workflow
  if [ "$repo" = "vbwd-android-core" ]; then
    cat > "$dest/.github/workflows/publish.yml" <<'EOF'
name: publish
on:
  push:
    tags: ["v*"]
jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
      - uses: android-actions/setup-android@v3
      - uses: gradle/actions/setup-gradle@v4
      - name: Publish core to GitHub Packages
        run: ./gradlew :core:publish
        env:
          GITHUB_ACTOR: ${{ github.actor }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
EOF
  fi

  cp "$MONO/.gitignore" "$dest/.gitignore" 2>/dev/null || true

  # init + create + push (the steps the assistant cannot run)
  ( cd "$dest"
    git init -q -b main
    git add -A
    git commit -q -m "$repo — split from the vbwd-android monorepo"
    gh repo create "$OWNER/$repo" "$VISIBILITY" --source=. --remote=origin --push )
  echo "   ✓ $OWNER/$repo pushed"
}

echo "$MODULES" | while IFS='|' read -r moduledir repo _artifact _version upstreams; do
  [ -z "$moduledir" ] && continue
  scaffold "$moduledir" "$repo" "$upstreams"
done

echo "Done. Tag vbwd-android-core (git tag v0.1.0 && git push --tags) to publish core,"
echo "then meinchat, so dependent CI can resolve them from GitHub Packages."
