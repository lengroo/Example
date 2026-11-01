package com.example.libxposedexample

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * Minimal modern libxposed (API 101/102) module.
 *
 * It deliberately changes nothing: when LSPosed injects it into a
 * scoped app, it only logs (a) that the app is ready and (b) each
 * Activity resume in that app. That makes the hook observable in
 * logcat without altering any argument or return value.
 *
 * Key modern-API facts (verified against api-102.0.0):
 * - Entry class extends [XposedModule] with a public no-arg constructor.
 * - The framework calls attachFramework() itself; do not call it.
 * - Hooks are installed with hook(Method).intercept { chain -> ... },
 *   where chain.proceed() calls the original method unchanged.
 * - The class is listed by name in META-INF/xposed/java_init.list,
 *   and module.prop declares minApiVersion=101 / targetApiVersion=102.
 *
 * Note: the framework instantiates this class by name from
 * META-INF/xposed/java_init.list, so it must stay public with a
 * public no-arg constructor (see proguard-rules.pro).
 */
class ExampleModule : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        log(Log.INFO, TAG, "LibXposed Example module loaded in ${param.processName}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        super.onPackageReady(param)
        if (!param.isFirstPackage) return
        log(Log.INFO, TAG, "Package ready: ${param.packageName}")

        try {
            val activityClass = Class.forName(
                "android.app.Activity", false, param.classLoader
            )
            val onResume = activityClass.getDeclaredMethod("onResume")
            hook(onResume).intercept { chain ->
                val activity = chain.thisObject
                log(
                    Log.INFO, TAG,
                    "Activity resumed: ${activity?.javaClass?.name} " +
                        "in ${param.packageName}"
                )
                // Pass through unchanged: no args modified, no result replaced.
                chain.proceed()
            }
        } catch (t: Throwable) {
            log(Log.WARN, TAG, "Could not install Activity hook: $t")
        }
    }

    companion object {
        private const val TAG = "LibXposedExample"
    }
}
