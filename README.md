**Language:** [English](README.md) | [中文](README.zh-CN.md)

# LibXposed Example

A minimal, modern libxposed module (API 101/102) for LSPosed / Vector.

It does one thing and does it visibly: it logs, to logcat, when a
scoped app loads and when an Activity in that app resumes. It changes
no app behaviour, no arguments, and no return values.

## The app has two interfaces

Opening the app goes straight to the **MiUIX interface**
(`ComposeActivity`, a `ComponentActivity` built with Compose +
MiUIX `0.9.4`, see https://github.com/compose-miuix-ui/miuix).
It shows, in MiUIX `Card`s:

- **About** — what the module is.
- **Install and try** — install, enable, and logcat steps.
- **What it does** — the logging-only promise.
- **From the 1.0 interface** — the full text of the original
  framework interface, kept verbatim.

Since 1.1.12, each of these sections starts out collapsed.
Since 1.1.13, each section is a settings-style card in the style
of a MIUI settings list: a leading icon, the title with a
one-line summary under it, and a chevron on the right. Tap the
card to expand its text underneath (the chevron rotates down);
tap it again to collapse it. Each section expands and collapses
independently.

A button at the top right of the MiUIX top bar, **"Original"**,
switches to the **original interface** (`MainActivity`). That
button, and the diagnostics below, are debugging features: they
appear only in the debug build (see "Debug and release builds").
That screen is deliberately built with framework classes only —
no Compose, no MiUIX, no androidx imports — so it still works
even if the MiUIX screen fails to start. It shows:

- The same introduction and 1.0-era text.
- **Library checks** — whether each key Compose / MiUIX / androidx
  class actually loads (`Class.forName`), reported as `OK` / `FAIL`
  instead of crashing.
- **Last crash** — if the MiUIX screen ever died, the exact stack
  trace is saved by `CrashLog` and shown here on the next launch,
  with a button to clear it.
- **"Open MiUIX interface"** — back to the MiUIX screen.

The system title bar is hidden on both screens (`NoActionBar`), so
each screen shows only its own title.

## What the hook does

- When the module loads, it logs under the tag `LibXposedExample`.
- When a scoped app becomes ready, it logs the package name.
- It hooks `android.app.Activity.onResume()` in the scoped app,
  logs each resumed Activity's class name and package, then calls
  the original method unchanged.

Scope is intentionally empty (`META-INF/xposed/scope.list`
contains only a comment): pick the target app(s) yourself in the
LSPosed Manager. That keeps the example generic and avoids
targeting any specific third-party app.

## How it works (the modern API, verified against api-102.0.0)

- Entry class: `com.example.libxposedexample.ExampleModule`
  extends `io.github.libxposed.api.XposedModule` with a public
  no-arg constructor. The framework calls `attachFramework()`
  itself.
- Lifecycle: `onModuleLoaded(ModuleLoadedParam)`,
  `onPackageReady(PackageReadyParam)` (guarded by `isFirstPackage`).
- Hook: `hook(method).intercept { chain -> ...; chain.proceed() }`.
  `Chain.thisObject` is the receiver, `Chain.proceed()` runs the
  original method, `Chain.proceed(newArgs)` would run it with new
  arguments.
- Discovery (no legacy meta-data in the manifest):
  - `src/main/resources/META-INF/xposed/java_init.list` — entry class
  - `src/main/resources/META-INF/xposed/module.prop` —
    `minApiVersion=101`, `targetApiVersion=102`,
    `staticScope=false`, `exceptionMode=protective`
  - `src/main/resources/META-INF/xposed/scope.list` — default scope
- Dependencies: `io.github.libxposed:api:102.0.0` is `compileOnly`
  (the framework provides it at runtime inside the hooked process).
  `io.github.libxposed:service:102.0.0` is `implementation` in the
  Gradle build (it supplies the framework-communication provider);
  the manual build below notes a d8 issue with that AAR.

## Install and try

1. Install on a device with LSPosed (API 101+) or Vector (API 102).
   The APK is signed with a debug key, so it is for testing only.
   Releases are on the [Releases](https://github.com/lengroo/Example/releases) page.
2. In LSPosed Manager, enable "LibXposed Example" and choose a scope app.
3. Force-stop and relaunch the scoped app (or reboot).
4. Observe logcat:

```sh
adb logcat -s LibXposedExample
```

You should see "Package ready: …" and "Activity resumed: …" lines.
If you see nothing, check that the module is enabled, the scope
includes the app you launched, and that the app was actually
restarted after enabling.

## Build (normal route)

```sh
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17, Android SDK 35, and a working Gradle daemon.

## Build (manual route, used for the released APKs)

In the VM the releases were built in, the Gradle daemon could not
communicate with its client (even a trivial Gradle project failed
the same way), so the APKs were built step by step instead:

```sh
./build-manual-compose.sh
# app-debug.apk
```

That script runs: `aapt2 compile/link` (app + library AAR
resources, generating R classes) → `kotlinc` with the Compose
compiler plugin → `javac` for the generated R classes → `d8`
(standalone R8/D8 8.11.32, because d8 from build-tools 34.0.0
crashes on several current androidx classes) → zip assembly
(adding `classes*.dex` and `META-INF/xposed/*`) → `zipalign` →
`apksigner` (debug key).

It needs the library jars in `build-manual-compose/jars/`
(classes.jar extracted from the MiUIX / Compose / activity AARs,
plus lifecycle-common and kotlinx-coroutines); that directory is
a local build cache and is not part of the source tree.

Two lessons from that route are recorded in the build script and
worth knowing if you rebuild by hand:

- **Generated R classes must be dexed.** They live apart from the
  app classes, and library code such as `ViewCompat` references
  `androidx.core.R$id` as a non-final field, so a build that drops
  them fails at runtime with `NoClassDefFoundError`.
- **Split artifacts must be included, not just their parent.**
  `androidx.core.viewtree.ViewTree` is not in `core-1.16.0` — it is
  in `core-viewtree 1.0.0` — and MiUIX's
  `RuntimeShader_androidKt` is not in `miuix-ui` — it is in
  `miuix-shader-android 0.9.4`, a dependency of `miuix-squircle`.
  Missing either one crashes the MiUIX screen on first use.

Note on the service AAR: d8 from build-tools 34.0.0 fails on
`io.github.libxposed.service.HookedTarget$State` with a
NullPointerException, so the manual build bundles only the module's
own classes, the Compose/MiUIX/activity libraries, and
kotlin-stdlib, not the service AAR. The hook itself does not use
the service API, so the resulting APK is still a valid module; if
you build with Gradle, the service classes are included as usual.

## Files

- `app/src/main/java/com/example/libxposedexample/ExampleModule.kt`
  — the hook entry point
- `app/src/main/java/com/example/libxposedexample/ComposeActivity.kt`
  — the MiUIX interface (default entry)
- `app/src/main/java/com/example/libxposedexample/MainActivity.kt`
  — the original framework interface: library checks, crash log,
  and the button back to MiUIX
- `app/src/main/java/com/example/libxposedexample/CrashLog.kt`
  — saves uncaught MiUIX crashes for the original interface to show
- `app/src/main/AndroidManifest.xml`
- `app/src/main/resources/META-INF/xposed/{module.prop,java_init.list,scope.list}`
- `app/build.gradle.kts`, `gradle/libs.versions.toml`, `build-manual-compose.sh`

## Version notes

- **1.0** — original framework interface.
- **1.1.0–1.1.1** — first MiUIX refactor; crashed on launch on some
  devices because real runtime classes were missing from the dex.
- **1.1.2–1.1.6** — diagnostic split (framework launcher + MiUIX
  activity), then one missing class at a time, each diagnosed from
  the crash log shown in the launcher: `FastSafeIterableMap`,
  `ViewTree`, then MiUIX's `RuntimeShader_androidKt`.
- **1.1.7** — MiUIX is the default entry, the system title bar is
  hidden, and a bottom button switches back to the original
  interface.
- **1.1.8** — the switch button moves to the top bar, and both
  a debug and a release APK are published (see below).
- **1.1.9** — the excess blank space below the title bar is
  removed: the 1.1.4 extra 32dp top padding and 16dp spacer are
  dropped, leaving only Scaffold's own top-bar padding plus
  12dp content padding above "About". From 1.1.9 on, only the
  debug APK is packaged and released.
- **1.1.10** — switching interfaces reuses the existing activity
  (`FLAG_ACTIVITY_REORDER_TO_FRONT`) instead of creating a new
  one on every tap, so the back stack never grows past the two
  interfaces and backing out no longer needs one press per switch.
- **1.1.11** — launcher (desktop) icon changed to the
  user-provided image, shipped as a 192x192
  `mipmap-xxxhdpi/ic_launcher.png` and referenced as the app's
  icon and round icon in the manifest.
- **1.1.12** — the MiUIX screen's four sections (About, Install
  and try, What it does, From the 1.0 interface) now start
  collapsed. Tap a section title to expand its card; tap the
  title or the open card again to collapse it.
- **1.1.13** — the MiUIX screen's sections are now
  settings-style cards (leading icon, title, one-line summary,
  chevron), in the style of a MIUI settings list. Tapping a card
  expands its text inside the same card. Adds the MiUIX
  `miuix-icons` dependency for the section icons.

## Telegram forwarding

When a release is published, a GitHub Actions workflow
(`.github/workflows/notify-telegram.yml`) automatically sends
the release APK to a Telegram channel via the Telegram Bot API.
It needs two repository secrets (Settings > Secrets and
variables > Actions): `TELEGRAM_BOT_TOKEN` and
`TELEGRAM_CHAT_ID`. Neither value is stored in this repo — the
workflow only references the secret names. To test it without
a new release, run it manually from the Actions tab with an
existing release tag.

## Debug and release builds

One source tree produces both, gated on whether the APK is
debuggable (`DebugFeatures`, checked against
`ApplicationInfo.FLAG_DEBUGGABLE`):

- **Debug** (`app-debug.apk`, `aapt2 --debug-mode`, and Gradle's
  debug build type): the MiUIX top bar shows the "Original"
  switch button, the original interface shows library checks and
  the last crash, and `CrashLog` records crashes. This is the
  only variant packaged and released from 1.1.9 on.
- **Release** (`app-release.apk`, no `--debug-mode`, and Gradle's
  release build type): none of that debugging UI appears. The
  MiUIX interface is clean, with no switch button. Not packaged
  from 1.1.9 on, per the user's decision.

Both are signed here with the local debug keystore (testing only);
a production release would need its own release key. Note that
the diagnostic classes are still present in the release dex —
their UI is simply unreachable, with no switch button to open
them.
