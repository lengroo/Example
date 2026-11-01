package com.example.libxposedexample

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * Plain configuration UI for the module app.
 *
 * The actual hook lives in [ExampleModule] and only runs inside a
 * process that LSPosed injects it into. This activity is just a
 * landing screen explaining that, so the APK is a normal, launchable app.
 * It uses only framework APIs (no AndroidX) to keep the example minimal.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this).apply {
            text = """
                LibXposed Example

                This is a minimal LSPosed / Vector module.

                1. Install this APK on a device with LSPosed (API 101+) or Vector (API 102).
                2. Enable "LibXposed Example" in the LSPosed Manager and pick a scope app.
                3. Reboot / force-stop the scoped app, then launch it.
                4. Watch logcat for the LibXposedExample tag:
                   adb logcat -s LibXposedExample

                What it does: it only logs when a scoped app loads and
                when an Activity in that app resumes. It does not change
                any app behaviour, return values, or arguments.
            """.trimIndent()
            textSize = 16f
            setPadding(48, 48, 48, 48)
        }
        setContentView(text)
    }
}
