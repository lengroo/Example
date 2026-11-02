package com.example.libxposedexample

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * The real MiUIX interface, moved here in 1.1.2 so the launcher
 * ([MainActivity]) can stay on framework classes only.
 *
 * If setContent itself throws, fall back to a framework TextView
 * with the exception, and [CrashLog] (installed by the launcher in
 * the same process) records uncaught composition crashes for the
 * launcher to show on the next launch.
 */
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(this)
        try {
            setContent {
                val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
                MiuixTheme(colors = colors) {
                    Scaffold(
                        topBar = {
                            SmallTopAppBar(title = "LibXposed Example")
                        },
                        content = { paddingValues ->
                            ExampleScreen(paddingValues = paddingValues)
                        },
                    )
                }
            }
        } catch (e: Throwable) {
            val view = TextView(this)
            view.text = "MiUIX failed to start:\n${e.javaClass.name}: ${e.message}\n\n${e.stackTraceToString().take(3000)}"
            val d = resources.displayMetrics.density
            view.setPadding((24 * d).toInt(), (72 * d).toInt(), (24 * d).toInt(), (24 * d).toInt())
            setContentView(view)
        }
    }
}

/**
 * 1.1.4 title fix: the first content below SmallTopAppBar was
 * obscured by the title on the user's phone, so the content is now
 * pushed well down on purpose. Scaffold's paddingValues are kept,
 * plus an extra 32dp on top, plus a 16dp spacer as the first item,
 * so the first SmallTitle can never sit underneath the top bar,
 * even if paddingValues ever reports a smaller top inset.
 */
@Composable
private fun ExampleScreen(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            SmallTitle(text = "About")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(text = "This is a minimal LSPosed / Vector module.")
            }
        }
        item {
            SmallTitle(text = "Install and try")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(
                    text = "1. Install this APK on a device with " +
                        "LSPosed (API 101+) or Vector (API 102).\n" +
                        "2. Enable \"LibXposed Example\" in the LSPosed " +
                        "Manager and pick a scope app.\n" +
                        "3. Reboot / force-stop the scoped app, then launch it.\n" +
                        "4. Watch logcat for the LibXposedExample tag:\n" +
                        "   adb logcat -s LibXposedExample",
                )
            }
        }
        item {
            SmallTitle(text = "What it does")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(
                    text = "It only logs when a scoped app loads and " +
                        "when an Activity in that app resumes. It does not " +
                        "change any app behaviour, return values, or arguments.",
                )
            }
        }
        item {
            SmallTitle(text = "From the 1.0 interface")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(
                    text = "LibXposed Example\n\n" +
                        "This is a minimal LSPosed / Vector module.\n\n" +
                        "1. Install this APK on a device with LSPosed " +
                        "(API 101+) or Vector (API 102).\n" +
                        "2. Enable \"LibXposed Example\" in the LSPosed " +
                        "Manager and pick a scope app.\n" +
                        "3. Reboot / force-stop the scoped app, then launch it.\n" +
                        "4. Watch logcat for the LibXposedExample tag:\n" +
                        "   adb logcat -s LibXposedExample\n\n" +
                        "What it does: it only logs when a scoped app loads and " +
                        "when an Activity in that app resumes. It does not change " +
                        "any app behaviour, return values, or arguments.",
                )
            }
        }
    }
}
