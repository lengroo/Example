package com.example.libxposedexample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Framework launcher (1.1.2).
 *
 * 1.1.0 and 1.1.1 closed immediately after the opening animation on
 * the user's phone, with no UI at all, and no logcat was available to
 * find out why. This activity deliberately uses ONLY framework
 * classes (android.app.Activity, TextView, Button) - no Compose, no
 * MiUIX, no androidx imports - so it cannot die from a Compose class
 * loading failure. It is the same kind of UI that 1.0.1 used, which
 * did display.
 *
 * It shows:
 * - whether the key Compose / MiUIX / androidx classes load at all
 *   (Class.forName, so a failure is reported instead of crashing),
 * - the last crash saved by [CrashLog], if the MiUIX activity died,
 * - a button to open the real MiUIX interface ([ComposeActivity]).
 *
 * If the MiUIX activity still dies, reopening this app will show the
 * exact exception here, and that screenshot is the diagnosis.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        val title = TextView(this).apply {
            text = "LibXposed Example 1.1.2"
            textSize = 22f
        }
        layout.addView(title)

        val intro = TextView(this).apply {
            text = "This is a minimal LSPosed / Vector module. " +
                "It only logs when a scoped app loads and when an " +
                "Activity in that app resumes. It does not change " +
                "any app behaviour."
            textSize = 16f
        }
        layout.addView(intro)

        val checks = TextView(this).apply {
            text = runChecks()
            textSize = 14f
        }
        layout.addView(checks)

        val lastCrash = CrashLog.read(this)
        if (lastCrash != null) {
            val crashTitle = TextView(this).apply {
                text = "\nLast crash (from the MiUIX screen):"
                textSize = 16f
            }
            layout.addView(crashTitle)
            val crashText = TextView(this).apply {
                text = lastCrash.take(4000)
                textSize = 13f
            }
            layout.addView(crashText)
            val clearButton = Button(this).apply {
                text = "Clear crash log"
                setOnClickListener {
                    CrashLog.clear(this@MainActivity)
                    recreate()
                }
            }
            layout.addView(clearButton)
        }

        val openButton = Button(this).apply {
            text = "Open MiUIX interface"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ComposeActivity::class.java))
            }
        }
        layout.addView(openButton)

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun runChecks(): String {
        val names = listOf(
            "androidx.collection.MutableScatterMap",
            "androidx.collection.IntList",
            "androidx.compose.ui.geometry.Rect",
            "androidx.compose.ui.geometry.Offset",
            "androidx.compose.ui.platform.ComposeView",
            "androidx.activity.ComponentActivity",
            "androidx.activity.compose.ComponentActivityKt",
            "androidx.core.view.ViewCompat",
            "androidx.window.layout.WindowMetricsCalculator",
            "top.yukonga.miuix.kmp.theme.MiuixThemeKt",
            "top.yukonga.miuix.kmp.basic.ScaffoldKt",
            "top.yukonga.miuix.kmp.basic.CardKt",
        )
        val sb = StringBuilder("\nLibrary checks:\n")
        for (name in names) {
            try {
                Class.forName(name)
                sb.append("OK  ").append(name).append("\n")
            } catch (e: Throwable) {
                sb.append("FAIL  ").append(name)
                    .append(" : ").append(e.javaClass.simpleName)
                    .append(": ").append(e.message).append("\n")
            }
        }
        return sb.toString()
    }
}
