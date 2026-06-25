#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_HOME="${ANDROID_HOME:-/home/lqtiger/Android/Sdk}"
GRADLE_BIN="${GRADLE_BIN:-/tmp/gradle-8.11.1/bin/gradle}"
RELEASE_APK="$ROOT_DIR/release/Lqtigee-debug.apk"
DEBUG_APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -x "$GRADLE_BIN" ]; then
  echo "Gradle not found at $GRADLE_BIN" >&2
  echo "Download Gradle 8.11.1 to /tmp/gradle-8.11.1 or set GRADLE_BIN." >&2
  exit 1
fi

mkdir -p "$(dirname "$RELEASE_APK")"

(
  cd "$ROOT_DIR"
  ANDROID_HOME="$ANDROID_HOME" "$GRADLE_BIN" --no-daemon :app:assembleDebug
)

cp "$DEBUG_APK" "$RELEASE_APK"
echo "$RELEASE_APK"
