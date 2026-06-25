#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_DIR="${ANDROID_HOME:-/home/lqtiger/Android/Sdk}"
BUILD_TOOLS="$SDK_DIR/build-tools/35.0.0"
PLATFORM_JAR="$SDK_DIR/platforms/android-35/android.jar"
QEMU="${QEMU_X86_64:-/tmp/qemu-user-static/usr/bin/qemu-x86_64-static}"
AMD64_ROOT="${AMD64_ROOT:-/tmp/amd64-root}"
AAPT2="$BUILD_TOOLS/aapt2"
OUT_DIR="$ROOT_DIR/app/build/manual"
RES_DIR="$ROOT_DIR/app/src/main/res"
SRC_DIR="$ROOT_DIR/app/src/main/java"
MANIFEST="$ROOT_DIR/app/src/main/AndroidManifest.xml"
KEYSTORE="$OUT_DIR/debug.keystore"
UNSIGNED_APK="$OUT_DIR/lqtigee-unsigned.apk"
ALIGNED_APK="$OUT_DIR/lqtigee-aligned.apk"
SIGNED_APK="$ROOT_DIR/app/build/outputs/apk/debug/lqtigee-debug.apk"
RELEASE_APK="$ROOT_DIR/release/Lqtigee-debug.apk"

run_x86_64() {
  "$QEMU" -L "$AMD64_ROOT" "$@"
}

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/compiled-res" "$OUT_DIR/classes" "$OUT_DIR/dex" "$OUT_DIR/manifest" "$(dirname "$SIGNED_APK")" "$(dirname "$RELEASE_APK")"

run_x86_64 "$AAPT2" compile --dir "$RES_DIR" -o "$OUT_DIR/compiled-res" >/dev/null

run_x86_64 "$AAPT2" link \
  -I "$PLATFORM_JAR" \
  --manifest "$MANIFEST" \
  --java "$OUT_DIR/manifest" \
  --auto-add-overlay \
  --min-sdk-version 26 \
  --target-sdk-version 35 \
  --version-code 2 \
  --version-name 0.1.1 \
  --compile-sdk-version-code 35 \
  --compile-sdk-version-name 15 \
  -o "$UNSIGNED_APK" \
  "$OUT_DIR"/compiled-res/*.flat

javac \
  -encoding UTF-8 \
  -source 8 \
  -target 8 \
  -bootclasspath "$PLATFORM_JAR" \
  -classpath "$OUT_DIR/manifest" \
  -d "$OUT_DIR/classes" \
  $(find "$SRC_DIR" "$OUT_DIR/manifest" -name '*.java' -print)

java -cp "$BUILD_TOOLS/lib/d8.jar" com.android.tools.r8.D8 \
  --min-api 26 \
  --lib "$PLATFORM_JAR" \
  --output "$OUT_DIR/dex" \
  $(find "$OUT_DIR/classes" -name '*.class' -print)

(
  cd "$OUT_DIR/dex"
  zip -q -u "$UNSIGNED_APK" classes.dex
)

run_x86_64 "$BUILD_TOOLS/zipalign" -f 4 "$UNSIGNED_APK" "$ALIGNED_APK"

if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null
fi

java -jar "$BUILD_TOOLS/lib/apksigner.jar" sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$SIGNED_APK" \
  "$ALIGNED_APK"

java -jar "$BUILD_TOOLS/lib/apksigner.jar" verify "$SIGNED_APK"
cp "$SIGNED_APK" "$RELEASE_APK"
echo "$RELEASE_APK"
