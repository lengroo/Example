#!/bin/bash
# Manual APK build for the libxposed example.
#
# The normal route is Gradle:  ./gradlew :app:assembleDebug
# This script is the equivalent, step by step, without Gradle:
#   aapt2 compile/link  ->  kotlinc  ->  d8 (dex)  ->  zipalign  ->  apksigner
#
# It is used here because the Gradle daemon cannot talk to its client
# in this particular VM (a trivial Gradle project fails the same way),
# so Gradle is not a reliable builder here even though the Gradle
# project files in this directory are complete and correct.
#
# Required environment (defaults shown):
#   JAVA_HOME=$HOME/tools/jdk17
#   ANDROID_HOME=$HOME/Android/Sdk   (platform android-35, build-tools 34.0.0)
#   KOTLIN_HOME=$HOME/tools/kotlinc
#   LIBXPOSED_DIR=$HOME/tools/libxposed   (api-classes.jar, service-classes.jar
#     extracted from io.github.libxposed:api/service:102.0.0 AARs)
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="${JAVA_HOME:-$HOME/tools/jdk17}"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
KOTLIN_HOME="${KOTLIN_HOME:-$HOME/tools/kotlinc}"
LIBXPOSED_DIR="${LIBXPOSED_DIR:-$HOME/tools/libxposed}"
BUILD_TOOLS="$ANDROID_HOME/build-tools/34.0.0"
ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android.jar"
OUT="$PROJECT_DIR/build-manual"

export PATH="$JAVA_HOME/bin:$PATH"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex"

echo "== 1. Compile + link resources =="
"$BUILD_TOOLS/aapt2" compile --dir "$PROJECT_DIR/app/src/main/res" -o "$OUT/compiled-res.zip"
# aapt2 needs a package attribute; Gradle supplies it from `namespace`,
# so add it to a temporary copy for the manual build only.
sed 's|<manifest |<manifest package="com.example.libxposedexample" |' \
    "$PROJECT_DIR/app/src/main/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"
"$BUILD_TOOLS/aapt2" link -o "$OUT/base.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$OUT/AndroidManifest.xml" \
    --min-sdk-version 26 --target-sdk-version 35 \
    --version-code 2 --version-name 1.0.1 \
    "$OUT/compiled-res.zip"

echo "== 2. Compile Kotlin (libxposed api = compileOnly, service = implementation) =="
"$KOTLIN_HOME/bin/kotlinc" -jvm-target 17 \
    -cp "$ANDROID_JAR:$LIBXPOSED_DIR/api-classes.jar:$LIBXPOSED_DIR/service-classes.jar" \
    -d "$OUT/classes" \
    "$PROJECT_DIR/app/src/main/java/com/example/libxposedexample/"*.kt

echo "== 3. Dex (kotlin-stdlib is bundled; api is NOT bundled) =="
# d8 rejects the META-INF/*.kotlin_module file kotlinc emits, so jar
# only the class files first.
(cd "$OUT/classes" && jar cf "$OUT/app-classes.jar" com)
# NOTE: d8 from build-tools 34.0.0 crashes on
# io.github.libxposed.service.HookedTarget$State (NullPointerException),
# so the manual build omits the service AAR. The hook does not use the
# service API, so the module still works; the Gradle build includes it.
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 26 \
    --output "$OUT/dex" \
    "$OUT/app-classes.jar" \
    "$KOTLIN_HOME/lib/kotlin-stdlib.jar"

echo "== 4. Assemble APK: dex + META-INF/xposed metadata =="
python3 - "$OUT/base.apk" "$OUT/unsigned.apk" "$OUT/dex/classes.dex" "$PROJECT_DIR/app/src/main/resources/META-INF/xposed" << 'PY'
import sys, zipfile, pathlib
base, out, dex, meta = sys.argv[1:5]
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        zout.writestr(item, zin.read(item.filename))
    zout.write(dex, 'classes.dex')
    for p in sorted(pathlib.Path(meta).iterdir()):
        zout.write(p, f'META-INF/xposed/{p.name}')
PY

echo "== 5. Zipalign + sign with a debug key =="
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [ ! -f "$OUT/debug.keystore" ]; then
    keytool -genkeypair -keystore "$OUT/debug.keystore" -storepass android \
        -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 \
        -validity 10000 -dname "CN=Debug, OU=Debug, O=Debug, L=Debug, ST=Debug, C=US" \
        > /dev/null 2>&1
fi
"$BUILD_TOOLS/apksigner" sign --ks "$OUT/debug.keystore" \
    --ks-pass pass:android --key-pass pass:android \
    --out "$OUT/app-debug.apk" "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$OUT/app-debug.apk"
cp "$OUT/app-debug.apk" "$PROJECT_DIR/app-debug.apk"
echo "Built: $PROJECT_DIR/app-debug.apk"
