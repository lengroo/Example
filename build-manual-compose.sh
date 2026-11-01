#!/bin/bash
# Manual Compose + MiUIX build (used because the Gradle daemon does not
# work in this VM: it fails with "Could not receive a message from the
# daemon" even for a trivial project, with --no-daemon and a clean env).
#
# Steps: aapt2 compile/link (app + library AAR resources, generating
# R classes) -> kotlinc 2.4 with the Compose compiler plugin ->
# javac the generated R.java files -> d8 (app + R + library jars +
# kotlin-stdlib, excluding libxposed api which is compileOnly) ->
# zip assembly (classes.dex + META-INF/xposed/*) -> zipalign -> apksigner.
#
# Library jars are expected in build-manual-compose/jars/ (classes.jar
# extracted from each AAR, plus lifecycle-common and coroutines jars).
# See README for the Gradle route, which is the normal way to build.
#
# NOTE (1.1.1): 1.1.0 crashed on launch because two real runtime jars
# were missing from the dex: androidx.collection:collection-jvm (Compose
# runtime/foundation use MutableScatterMap/IntList on first composition)
# and androidx.compose.ui:ui-geometry-android (Rect/Offset/Size). The
# metadata-only collection-1.4.5.jar / annotation-1.9.1.jar (0 classes)
# were mistaken for the real jars. 1.1.1 adds collection-jvm 1.5.0,
# ui-geometry-android 1.12.0, window 1.5.0, and the other Compose
# runtime-compose / savedstate-compose / runtime-retain / material3
# window-size-class jars listed in the jars/ directory.
#
# NOTE (1.1.2): 1.1.1 still closed on launch, so the launcher was
# split: MainActivity is now framework-only (no Compose imports) and
# shows library load checks + the last crash, with a button to open
# the MiUIX UI in ComposeActivity. Core was also upgraded to the
# version Compose 1.12 actually requires: core/core-ktx 1.13.1 ->
# 1.16.0, savedstate-android 1.3.1 -> 1.3.2, to rule out a
# NoSuchMethodError from the version mismatch.
set -euo pipefail
cd "$(dirname "$0")"
PROJECT_DIR="$PWD"

SDK="$HOME/Android/Sdk"
BT="$SDK/build-tools/34.0.0"
AAPT2="$BT/aapt2"
D8="$HOME/tools/jdk17/bin/java -cp $HOME/tools/downloads/r8-8.11.32.jar com.android.tools.r8.D8"
# NOTE: d8 from build-tools 34.0.0 crashes with an NPE on several
# current androidx classes (core ResultReceiver, customview
# AbsSavedState, ...), so this script uses standalone R8/D8 8.11.32
# from Google Maven instead.
ZIPALIGN="$BT/zipalign"
APKSIGNER="$BT/apksigner"
ANDROID_JAR="$SDK/platforms/android-35/android.jar"
KOTLINC="$HOME/tools/kotlinc/bin/kotlinc"
JAVA="$HOME/tools/jdk17/bin/java"
JAVAC="$HOME/tools/jdk17/bin/javac"
JAR="$HOME/tools/jdk17/bin/jar"

OUT="$PROJECT_DIR/build-manual-compose"
rm -rf "$OUT/work"
mkdir -p "$OUT/work/compiled-res" "$OUT/work/R" "$OUT/work/classes" "$OUT/work/dex"

echo "== 1. Compile + link resources (app + AARs) =="
# Extract AAR resources into per-library dirs and compile them.
i=0
for aar in "$HOME"/tools/compose-deps/*.aar; do
  case "$aar" in
    *lifecycle-common-android*|*lifecycle-livedata-android*|*lifecycle-runtime-2.8.7*|*lifecycle-runtime-2.9.2.aar|*lifecycle-runtime-ktx*|*lifecycle-viewmodel-2.9.2.aar|*savedstate-1.3.1.aar|*lifecycle-runtime-old*) continue ;;
  esac
  dir="$OUT/work/aar-res-$i"; mkdir -p "$dir"
  if unzip -q "$aar" 'res/*' -d "$dir" 2>/dev/null && [ -d "$dir/res" ]; then
    "$AAPT2" compile --dir "$dir/res" -o "$OUT/work/compiled-res/lib-$i.zip" 2>/dev/null || true
  fi
  i=$((i+1))
done
"$AAPT2" compile --dir app/src/main/res -o "$OUT/work/compiled-res/app.zip"

# Temporary manifest copy with a package attribute (Gradle supplies it
# from namespace normally).
cp app/src/main/AndroidManifest.xml "$OUT/work/AndroidManifest.xml"
python3 - "$OUT/work/AndroidManifest.xml" << 'PY'
import sys
p = sys.argv[1]
t = open(p).read()
t = t.replace('<manifest ', '<manifest package="com.example.libxposedexample" ', 1)
open(p, 'w').write(t)
PY

EXTRA=""
for pkg in androidx.activity androidx.activity.compose androidx.collection androidx.compose.animation androidx.compose.animation.core androidx.compose.animation.graphics androidx.compose.foundation androidx.compose.foundation.layout androidx.compose.material3.windowsizeclass androidx.compose.runtime androidx.compose.runtime.retain androidx.compose.runtime.saveable androidx.compose.ui androidx.compose.ui.geometry androidx.compose.ui.graphics androidx.compose.ui.text androidx.compose.ui.unit androidx.compose.ui.util androidx.core androidx.core.ktx androidx.customview androidx.customview.poolingcontainer androidx.emoji2 androidx.emoji2.widget androidx.lifecycle.runtime androidx.lifecycle.runtime.compose androidx.lifecycle.viewmodel androidx.lifecycle.viewmodel.savedstate androidx.navigationevent androidx.navigationevent.compose androidx.profileinstaller androidx.savedstate androidx.savedstate.compose androidx.startup androidx.window top.yukonga.miuix.kmp.core top.yukonga.miuix.kmp.squircle top.yukonga.miuix.kmp.ui com.materialkolor.colorutilities org.jetbrains.androidx.compose.material3.windowsizeclass; do
  EXTRA="$EXTRA --extra-packages $pkg"
done

"$AAPT2" link -o "$OUT/work/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$OUT/work/AndroidManifest.xml" \
  --java "$OUT/work/R" \
  $EXTRA \
  --min-sdk-version 26 --target-sdk-version 35 \
  --version-code 5 --version-name 1.1.2 \
  --auto-add-overlay \
  "$OUT/work/compiled-res/"*.zip

echo "== 2. Compile Kotlin with Compose plugin =="
CP=$(ls "$OUT/jars/"*.jar | tr '\n' ':')
CP="$CP$ANDROID_JAR:$HOME/tools/libxposed/api-classes.jar:$HOME/tools/kotlinc/lib/kotlin-stdlib.jar"
"$KOTLINC" -jvm-target 17 \
  -Xplugin="$HOME/tools/kotlinc/lib/compose-compiler-plugin.jar" \
  -P plugin:androidx.compose.compiler.plugins.kotlin:generateFunctionKeyMetaAnnotations=true \
  -cp "$CP" -d "$OUT/classes" \
  app/src/main/java/com/example/libxposedexample/*.kt

echo "== 3. Compile generated R.java =="
find "$OUT/work/R" -name '*.java' > "$OUT/work/r-sources.txt"
"$JAVAC" -source 17 -target 17 -cp "$ANDROID_JAR" -d "$OUT/work/classes" @"$OUT/work/r-sources.txt"

echo "== 4. Dex (excluding libxposed api; excluding the old lifecycle-runtime 2.6.2 jar, whose Registry classes duplicate the 2.9.2 runtime-android jar; lifecycle-common 2.6.2 is kept for Lifecycle/LifecycleOwner) =="
DEX_CP=""
for j in "$OUT"/jars/*.jar; do
  case "$j" in
    *lifecycle-runtime-2.6.2*) continue ;;
  esac
  DEX_CP="$DEX_CP $j"
done
(cd "$OUT/classes" && "$JAR" cf "$OUT/work/app-classes.jar" .)
$D8 --release --min-api 26 --output "$OUT/work/dex" \
  "$OUT/work/app-classes.jar" $DEX_CP "$HOME/tools/kotlinc/lib/kotlin-stdlib.jar"

echo "== 5. Assemble, zipalign, sign =="
python3 - "$OUT/work/dex" "$OUT/work/base.apk" "$OUT/work/unsigned.apk" << 'PY'
import sys, zipfile, pathlib
dexdir, base, out = sys.argv[1:4]
zin = zipfile.ZipFile(base)
zout = zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED)
for item in zin.infolist():
    if item.filename.startswith('classes') and item.filename.endswith('.dex'):
        continue
    zout.writestr(item, zin.read(item.filename))
zin.close()
# D8 may split into classes.dex, classes2.dex, ... (multidex; minSdk 26
# supports it natively). Include every dex file, or half the app is missing.
for dex in sorted(pathlib.Path(dexdir).glob('*.dex')):
    zout.write(dex, dex.name)
for f in ['java_init.list', 'module.prop', 'scope.list']:
    zout.write(f'app/src/main/resources/META-INF/xposed/{f}', f'META-INF/xposed/{f}')
zout.close()
PY
"$ZIPALIGN" -f 4 "$OUT/work/unsigned.apk" "$OUT/work/aligned.apk"
if [ ! -f $PROJECT_DIR/build-manual/debug.keystore ]; then
  "$HOME/tools/jdk17/bin/keytool" -genkeypair -v -keystore $PROJECT_DIR/build-manual/debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi
"$APKSIGNER" sign --ks $PROJECT_DIR/build-manual/debug.keystore --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey "$OUT/work/aligned.apk"
cp "$OUT/work/aligned.apk" "$PROJECT_DIR/app-debug.apk"
"$APKSIGNER" verify "$PROJECT_DIR/app-debug.apk"
echo "Built: $PWD/app-debug.apk"
