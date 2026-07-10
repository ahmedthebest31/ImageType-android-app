package com.ahmedsamy.imagetype

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ahmedsamy.imagetype.util.LocalAppLanguage
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import com.ahmedsamy.imagetype.util.stringRes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmedsamy.imagetype.ui.screens.TabEditorWorkspace
import com.ahmedsamy.imagetype.ui.screens.TabLivePreviewWorkspace
import com.ahmedsamy.imagetype.ui.screens.TabSettingsWorkspace
import com.ahmedsamy.imagetype.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EditorViewModel = viewModel()

            LaunchedEffect(Unit) {
                val intent = this@MainActivity.intent
                if (intent != null && intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.setInputText(sharedText)
                    }
                }
            }

            val themeState by viewModel.appTheme.collectAsState()
            val forceDark = when (themeState) {
                "Dark Mode" -> true
                "Light Mode" -> false
                else -> isSystemInDarkTheme()
            }

            val appLanguage by viewModel.appLanguage.collectAsState()

            CompositionLocalProvider(
                LocalAppLanguage provides appLanguage
            ) {
                MyApplicationTheme(darkTheme = forceDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: EditorViewModel) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val isRtl = currentLang == "ar"

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides if (isRtl) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    ) {
        var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ImageType",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier.shadow(4.dp)
                )
            },
            bottomBar = {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val haptics = LocalHapticFeedback.current
                    val isHapticsEnabled by viewModel.hapticsEnabled.collectAsState()

                    NavigationBarItem(
                        selected = selectedTabIndex == 0,
                        onClick = {
                            selectedTabIndex = 0
                            if (isHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        label = { Text(stringRes(R.string.tab_editor)) },
                        modifier = Modifier.testTag("nav_tab_editor")
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            if (isHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        icon = { Icon(Icons.Default.Image, contentDescription = null) },
                        label = { Text(stringRes(R.string.tab_preview)) },
                        modifier = Modifier.testTag("nav_tab_preview")
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 2,
                        onClick = {
                            selectedTabIndex = 2
                            if (isHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringRes(R.string.tab_settings)) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTabIndex) {
                    0 -> TabEditorWorkspace(viewModel)
                    1 -> TabLivePreviewWorkspace(viewModel)
                    2 -> TabSettingsWorkspace(viewModel)
                }
            }
        }
    }
}
