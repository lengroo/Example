package com.example.libxposedexample

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * The real MiUIX interface, moved here in 1.1.2 so the launcher
 * ([MainActivity]) can stay on framework classes only.
 *
 * 1.1.7: this is now the DEFAULT entry (MAIN/LAUNCHER in the
 * manifest). The system title bar is hidden by the NoActionBar
 * theme, so only the MiUIX SmallTopAppBar shows at the top
 * (the screenshot in 1.1.6 showed both stacked).
 *
 * 1.1.8: the UI switch button moved from Scaffold's bottomBar to
 * the top bar (SmallTopAppBar actions, top-right), and it is a
 * debugging feature: it only appears in the debuggable build.
 * The release build has no switch button and no diagnostic UI.
 *
 * If setContent itself throws, fall back to a framework TextView
 * with the exception, and [CrashLog] (installed by the launcher in
 * the same process) records uncaught composition crashes for the
 * launcher to show on the next launch.
 *
 * 1.1.12: every section below starts collapsed; tapping its title
 * row (or its open card) expands / collapses it. See
 * [CollapsibleSection].
 */
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(this)
        // Belt and braces for the 1.1.6 duplicate-title screenshot:
        // hide the system action bar even if the theme is ignored.
        actionBar?.hide()
        try {
            setContent {
                val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
                MiuixTheme(colors = colors) {
                    Scaffold(
                        topBar = {
                            SmallTopAppBar(
                                title = "LibXposed Example",
                                actions = {
                                    // 1.1.8: switch button at the top,
                                    // debug builds only.
                                    val context = LocalContext.current
                                    if (DebugFeatures.isEnabled(context)) {
                                        TextButton(
                                            text = "Original",
                                            onClick = {
                                                // 1.1.10: reuse, don't stack.
                                                // Without this flag, every
                                                // switch created a new
                                                // MainActivity, so backing
                                                // out needed one press per
                                                // switch. REORDER_TO_FRONT
                                                // brings the existing
                                                // instance forward if it
                                                // is already in the task,
                                                // so the stack never grows
                                                // past the two interfaces.
                                                val intent = Intent(
                                                    context,
                                                    MainActivity::class.java,
                                                ).apply {
                                                    flags =
                                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                                }
                                                context.startActivity(intent)
                                            },
                                        )
                                    }
                                },
                            )
                        },
                        content = { paddingValues ->
                            ExampleScreen(paddingValues = paddingValues)
                        },
                    )
                }
            }
        } catch (e: Throwable) {
            val view = TextView(this)
            // 1.1.8: the stack trace is debugging detail, shown
            // only in the debuggable build.
            view.text = if (DebugFeatures.isEnabled(this)) {
                "MiUIX failed to start:\n${e.javaClass.name}: ${e.message}\n\n${e.stackTraceToString().take(3000)}"
            } else {
                "MiUIX failed to start."
            }
            val d = resources.displayMetrics.density
            view.setPadding((24 * d).toInt(), (72 * d).toInt(), (24 * d).toInt(), (24 * d).toInt())
            setContentView(view)
        }
    }
}

/**
 * 1.1.9 spacing fix: 1.1.4 added an extra 32dp on top plus a 16dp
 * spacer as the first item, on top of Scaffold's paddingValues.
 * That was too much — the 1.1.8 screenshots showed a large blank
 * gap below the title bar and above "About". Scaffold's
 * paddingValues already place the content below SmallTopAppBar,
 * so the extras are removed: content now starts 12dp below the
 * top bar (the LazyColumn contentPadding), with no extra top
 * padding and no spacer item.
 */
/**
 * 1.1.12: a section whose card starts out collapsed.
 *
 * Only the title row is visible at first (the section title plus
 * MiUIX's up/down arrow). Tapping the title row expands the card
 * with an [AnimatedVisibility] animation; tapping the title row
 * again, or tapping the open card itself, collapses it. Each
 * section remembers its own state, so sections expand and
 * collapse independently. The card content is exactly what the
 * section showed uncollapsed before 1.1.12.
 */
@Composable
private fun CollapsibleSection(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SmallTitle(text = title)
            Icon(
                imageVector = MiuixIcons.Basic.ArrowUpDown,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                modifier = Modifier.padding(end = 28.dp),
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                insideMargin = PaddingValues(16.dp),
                onClick = { expanded = false },
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ExampleScreen(paddingValues: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CollapsibleSection(title = "About") {
                Text(text = "This is a minimal LSPosed / Vector module.")
            }
        }
        item {
            CollapsibleSection(title = "Install and try") {
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
            CollapsibleSection(title = "What it does") {
                Text(
                    text = "It only logs when a scoped app loads and " +
                        "when an Activity in that app resumes. It does not " +
                        "change any app behaviour, return values, or arguments.",
                )
            }
        }
        item {
            CollapsibleSection(title = "From the 1.0 interface") {
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
