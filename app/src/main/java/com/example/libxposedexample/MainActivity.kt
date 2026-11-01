package com.example.libxposedexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Plain configuration UI for the module app, now built with
 * Compose + MiUIX (top.yukonga.miuix.kmp:miuix-ui-android:0.9.4).
 *
 * The actual hook lives in [ExampleModule] and only runs inside a
 * process that LSPosed injects it into. This activity is just a
 * landing screen explaining that, so the APK is a normal, launchable app.
 *
 * The previous version used a framework TextView inside a ScrollView.
 * This version replaces it with a MiUIX Scaffold + SmallTopAppBar +
 * LazyColumn of Cards, so the title bar is a real top bar (not an
 * overlay the text can slide under) and the content scrolls with
 * proper top padding from Scaffold's PaddingValues.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }
}
