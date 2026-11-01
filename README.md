# LibXposed Example

A minimal, modern libxposed module (API 101/102) for LSPosed / Vector.

## What it does

Nothing destructive, and nothing that changes an app's behaviour:

- When the module loads, it logs to logcat under the tag `LibXposedExample`.
- When a scoped app becomes ready, it logs the package name.
- It hooks `android.app.Activity.onResume()` in the scoped app and logs
  each resumed Activity's class name, then calls the original method
  unchanged. No arguments are modified and no return value is replaced.

Scope is intentionally empty (`META-INF/xposed/scope.list` contains only
a comment): pick the target app(s) yourself in the LSPosed Manager.
That keeps the example generic and avoids targeting any specific
third-party app.

## How it works (the modern API, verified against api-102.0.0)

- Entry class: `com.example.libxposedexample.ExampleModule`
  extends `io.github.libxposed.api.XposedModule` with a public no-arg
  constructor. The framework calls `attachFramework()` itself.
- Lifecycle: `onModuleLoaded(ModuleLoadedParam)`,
  `onPackageReady(PackageReadyParam)` (guarded by `isFirstPackage`).
- Hook: `hook(method).intercept { chain -> ...; chain.proceed() }`.
  `Chain.thisObject` is the receiver, `Chain.proceed()` runs the
  original method, `Chain.proceed(newArgs)` would run it with new args.
- Discovery (no legacy meta-data in the manifest):
  - `src/main/resources/META-INF/xposed/java_init.list` — entry class name
  - `src/main/resources/META-INF/xposed/module.prop` —
    `minApiVersion=101`, `targetApiVersion=102`, `staticScope=false`,
    `exceptionMode=protective`
  - `src/main/resources/META-INF/xposed/scope.list` — default scope
- Dependencies: `io.github.libxposed:api:102.0.0` is `compileOnly`
  (the framework provides it at runtime inside the hooked process).
  `io.github.libxposed:service:102.0.0` is `implementation` in the
  Gradle build (it supplies the framework-communication provider);
  the manual build below notes a d8 issue with that AAR, see below.

## Build (normal route)

```sh
./gradlew :app:assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17, Android SDK 35, and a working Gradle daemon.

## Build (manual route, used for the APK attached here)

In the VM this was built in, the Gradle daemon could not communicate
with its client (even a trivial Gradle project failed the same way),
so the APK was built step by step instead:

```sh
./build-manual.sh
# app-debug.apk
```

That script runs: `aapt2 compile/link` → `kotlinc` →
`d8` → zip assembly (adding `classes.dex` and `META-INF/xposed/*`) →
`zipalign` → `apksigner` (debug key).

Note on the service AAR: d8 from build-tools 34.0.0 fails on
`io.github.libxposed.service.HookedTarget$State` with a
NullPointerException, so the manual build bundles only the module's
own classes and kotlin-stdlib, not the service AAR. The hook itself
does not use the service API, so the resulting APK is still a valid
module; if you build with Gradle, the service classes are included
as usual.

## Install and try

1. Install on a device with LSPosed (API 101+) or Vector (API 102).
   The APK is signed with a debug key, so it is for testing only.
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

## Files

- `app/src/main/java/com/example/libxposedexample/ExampleModule.kt`
  — the hook entry point
- `app/src/main/java/com/example/libxposedexample/MainActivity.kt`
  — a plain landing screen explaining the module
- `app/src/main/AndroidManifest.xml`
- `app/src/main/resources/META-INF/xposed/{module.prop,java_init.list,scope.list}`
- `app/build.gradle.kts`, `gradle/libs.versions.toml`, `build-manual.sh`
