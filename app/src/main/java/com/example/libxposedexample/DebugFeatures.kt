package com.example.libxposedexample

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * 1.1.8: the debugging features are gated on the APK being
 * debuggable, not on a build-time constant, so one source tree
 * produces both variants:
 *
 * - debug APK (aapt2 --debug-mode): debuggable, so the MiUIX
 *   top bar shows the switch button, the original interface shows
 *   library checks and the last crash, and CrashLog records crashes.
 * - release APK (no --debug-mode): not debuggable, so none of
 *   that is shown. The MiUIX interface is clean, with no switch
 *   button and no diagnostic UI.
 *
 * This is the standard Android distinction: Gradle's debug
 * build type is debuggable, its release build type is not.
 */
object DebugFeatures {
    fun isEnabled(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
