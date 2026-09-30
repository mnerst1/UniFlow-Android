package dev.uniflow.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.uniflow.app.R
import dev.uniflow.app.data.Settings
import kotlinx.coroutines.launch

@Composable fun SettingsScreen(settings: Settings, save: (Settings) -> Unit, reset: () -> Unit, snackbar: SnackbarHostState) {
    var name by rememberSaveable(settings.name) { mutableStateOf(settings.name) }
    var resetConfirm by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val denied = stringResource(R.string.permission_denied)
    val nameSaved = stringResource(R.string.name_saved)
    fun permission() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var allowed by remember { mutableStateOf(permission()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) allowed = permission() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        save(settings.copy(reminders = granted))
        if (!granted) scope.launch { snackbar.showSnackbar(denied) }
    }
    Page {
        item { GlassCard {
            SectionTitle(stringResource(R.string.profile))
            FormText(R.string.name, name, { name = it })
            Button(enabled = name.isNotBlank(), onClick = { save(settings.copy(name = name.trim())); scope.launch { snackbar.showSnackbar(nameSaved) } }) { Text(stringResource(R.string.save)) }
        } }
        item { GlassCard {
            SelectField(stringResource(R.string.appearance), listOf("system" to stringResource(R.string.system), "light" to stringResource(R.string.light), "dark" to stringResource(R.string.dark)), settings.theme) { save(settings.copy(theme = it)) }
            SelectField(stringResource(R.string.language), listOf("system" to stringResource(R.string.system), "en" to "English", "kk" to "Қазақша", "ru" to "Русский"), settings.language) { save(settings.copy(language = it)) }
        } }
        item { GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reminders), Modifier.weight(1f))
                Switch(settings.reminders && allowed, onCheckedChange = { enabled ->
                    if (!enabled) save(settings.copy(reminders = false))
                    else if (permission()) save(settings.copy(reminders = true))
                    else if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
            Text(stringResource(R.string.reminders_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!allowed) {
                Text(stringResource(R.string.permission_denied), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text(stringResource(R.string.open_system_settings)) }
            }
        } }
        item { GlassCard {
            Text("UniFlow 1.0", style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.about))
            Text(stringResource(R.string.offline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { resetConfirm = true }) { Text(stringResource(R.string.demo)) }
        } }
    }
    if (resetConfirm) AlertDialog(onDismissRequest = { resetConfirm = false }, title = { Text(stringResource(R.string.demo)) },
        text = { Text(stringResource(R.string.demo_warning)) },
        confirmButton = { TextButton(onClick = { reset(); resetConfirm = false }) { Text(stringResource(R.string.demo)) } },
        dismissButton = { TextButton(onClick = { resetConfirm = false }) { Text(stringResource(R.string.cancel)) } })
}
