package dev.uniflow.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.uniflow.app.ui.*
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as UniFlowApplication
        setContent {
            val vm: StudyViewModel = viewModel(factory = StudyViewModel.factory(app.repository))
            val state by vm.state.collectAsStateWithLifecycle()
            val original = LocalContext.current
            val systemConfiguration = LocalConfiguration.current
            val config = remember(systemConfiguration, state.settings.language) {
                Configuration(systemConfiguration).apply { if (state.settings.language != "system") setLocale(Locale.forLanguageTag(state.settings.language)) }
            }
            val context = remember(original, config) { original.createConfigurationContext(config) }
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config,
                LocalActivityResultRegistryOwner provides this@MainActivity, LocalActivity provides this@MainActivity) {
                UniTheme(state.settings.theme) { UniApp(state, vm, intent.getStringExtra("route") ?: "home") }
            }
        }
    }
}
