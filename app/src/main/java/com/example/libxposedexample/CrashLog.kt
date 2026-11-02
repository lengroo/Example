package com.example.libxposedexample

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Saves the last uncaught crash to the app's private files dir, so the
 * framework launcher can show it on the next launch even if the
 * Compose activity dies before any UI appears. No Compose, no MiUIX,
 * no androidx imports here on purpose: this file must load even when
 * Compose itself is broken.
 */
object CrashLog {
    private const val NAME = "last_crash.txt"

    fun install(context: Context) {
        // 1.1.8: crash recording is a debugging feature. The release
        // APK is not debuggable, so it records nothing.
        if (!DebugFeatures.isEnabled(context)) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        // Only install once per process.
        if (previous is SavingHandler) return
        Thread.setDefaultUncaughtExceptionHandler(SavingHandler(context, previous))
    }

    fun read(context: Context): String? {
        return try {
            val f = File(context.filesDir, NAME)
            if (f.exists()) f.readText() else null
        } catch (_: Throwable) {
            null
        }
    }

    fun clear(context: Context) {
        try {
            File(context.filesDir, NAME).delete()
        } catch (_: Throwable) {
        }
    }

    private class SavingHandler(
        private val context: Context,
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                var cause = throwable.cause
                while (cause != null) {
                    sw.append("Caused by: ")
                    cause.printStackTrace(PrintWriter(sw))
                    cause = cause.cause
                }
                File(context.filesDir, NAME).writeText(sw.toString())
            } catch (_: Throwable) {
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }
    }
}
