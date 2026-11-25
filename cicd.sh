#!/usr/bin/env bash
#
# Gahshomar local CI/CD pipeline (replaces .github/workflows/build-and-release.yml)
#
# Usage:
#   ./cicd.sh              # full pipeline: test -> lint -> debug build -> package
#   ./cicd.sh test         # :core:jalali:test + testDebugUnitTest
#   ./cicd.sh lint         # lintDebug
#   ./cicd.sh build        # assembleDebug
#   ./cicd.sh release      # signed assembleRelease (needs keystore env vars)
#   ./cicd.sh package      # package already-built APKs into dist/
#   ./cicd.sh clean        # ./gradlew clean
#
# Release signing (Rule 2.1 of AGENTS.md) — either:
#   export KEYSTORE_FILE=/path/release.jks KEYSTORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=...
# or keep an untracked signing/keystore.properties.

set -euo pipefail

cd "$(dirname "$0")"

GRADLE_CMD="./gradlew"
PIPELINE_STEPS=(":core:jalali:test" "testDebugUnitTest" "lintDebug" "assembleDebug")

log()  { printf '\033[1;34m[ci]\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m[ok]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[fail]\033[0m %s\n' "$*" >&2; exit 1; }

app_version() {
    local major minor patch
    major=$(grep '^VERSION_MAJOR' version.properties | cut -d'=' -f2 | tr -d '[:space:]')
    minor=$(grep '^VERSION_MINOR' version.properties | cut -d'=' -f2 | tr -d '[:space:]')
    patch=$(grep '^VERSION_PATCH' version.properties | cut -d'=' -f2 | tr -d '[:space:]')
    echo "${major}.${minor}.${patch}"
}

stage_test() {
    log "Running calendar + unit tests..."
    $GRADLE_CMD :core:jalali:test testDebugUnitTest --stacktrace
    ok "Unit tests passed."
}

stage_lint() {
    log "Running lint..."
    $GRADLE_CMD lintDebug --stacktrace
    ok "Lint passed."
}

stage_build() {
    log "Building debug APK..."
    $GRADLE_CMD assembleDebug --stacktrace
    ok "Debug build finished."
}

stage_release() {
    if [ -z "${KEYSTORE_FILE:-}" ] && [ ! -f "signing/keystore.properties" ]; then
        fail "Release signing requires KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD env vars or signing/keystore.properties."
    fi
    log "Building signed release APKs (multi-arch)..."
    $GRADLE_CMD assembleRelease --stacktrace
    ok "Release build finished."
}

stage_package() {
    local version dist
    version=$(app_version)
    dist="dist/Gahshomar-v${version}"
    mkdir -p "$dist"

    for apk in app/build/outputs/apk/release/*.apk; do
        [ -f "$apk" ] || continue
        filename=$(basename "$apk")
        if [[ "$filename" == *"arm64-v8a"* ]]; then
            cp "$apk" "$dist/Gahshomar-v${version}-arm64-v8a.apk"
        elif [[ "$filename" == *"armeabi-v7a"* ]]; then
            cp "$apk" "$dist/Gahshomar-v${version}-armeabi-v7a.apk"
        elif [[ "$filename" == *"x86_64"* ]]; then
            cp "$apk" "$dist/Gahshomar-v${version}-x86_64.apk"
        elif [[ "$filename" == *"universal"* ]]; then
            cp "$apk" "$dist/Gahshomar-v${version}-universal.apk"
        else
            fail "Unrecognized APK artifact: $filename"
        fi
    done

    if compgen -G "app/build/outputs/apk/debug/*.apk" >/dev/null; then
        cp app/build/outputs/apk/debug/*.apk "$dist/" 2>/dev/null || true
    fi

    ok "Packaged artifacts in $dist:"
    ls -lh "$dist"
}

case "${1:-all}" in
    test)    stage_test ;;
    lint)    stage_lint ;;
    build)   stage_build ;;
    release) stage_release ;;
    package) stage_package ;;
    clean)   $GRADLE_CMD clean ;;
    all)
        start=$(date +%s)
        stage_test
        stage_lint
        stage_build
        stage_package
        end=$(date +%s)
        ok "Full pipeline passed in $((end - start))s."
        ;;
    *)
        fail "Unknown stage: $1 (use: test | lint | build | release | package | all | clean)"
        ;;
esac
