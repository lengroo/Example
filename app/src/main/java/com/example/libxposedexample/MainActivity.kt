package com.example.libxposedexample

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

/**
 * Plain configuration UI for the module app.
 *
 * The actual hook lives in [ExampleModule] and only runs inside a
 * process that LSPosed injects it into. This activity is just a
 * landing screen explaining that, so the APK is a normal, launchable app.
 * It uses only framework APIs (no AndroidX) to keep the example minimal.
 *
 * Layout fix: the previous version called setPadding(48, 48, 48, 48),
 * which is 48 *pixels*, not dp — on a high-density screen that is only
 * about 16dp, so the first line of text started right underneath the
 * title/action bar and was clipped by it (see the screenshot).
 * This version converts padding to dp properly, gives the text a
 * generous top margin below the title bar, and wraps it in a
 * ScrollView so the whole text stays reachable on small screens.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density + 0.5f).toInt()

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
            // 24dp sides/bottom, 32dp on top so the first line sits
            // clearly below the title bar instead of underneath it.
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }

        val scrollView = ScrollView(this).apply {
            fitsSystemWindows = true
            addView(text)
        }
        setContentView(scrollView)
    }
}
