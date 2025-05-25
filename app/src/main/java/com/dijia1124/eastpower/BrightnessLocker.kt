package com.dijia1124.eastpower

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrightnessLocker(prefsRepo: PrefsRepository, innerPadding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val brightness by prefsRepo.brightnessFlow.collectAsState(initial = 2047)
    val context = LocalContext.current
    var isServiceOn by rememberSaveable { mutableStateOf(false) }
    var pendingWantOn by remember { mutableStateOf<Boolean?>(null) }
    val maxValue by prefsRepo.maxBrightnessFlow.collectAsState(initial = 4094)
    var maxValueText by rememberSaveable { mutableStateOf("") }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName
    var hasRoot by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hasRoot) {
        scope.launch {
            hasRoot = hasRootAccess()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingWantOn == true) {
            if (granted) {
                context.startForegroundService(
                    Intent(context, BrightnessLockerService::class.java)
                        .apply { action = BrightnessLockerService.ACTION_START }
                )
                isServiceOn = true
            } else {
                Toast.makeText(context,
                    context.getString(R.string.notification_permission_is_required_to_run_the_service),
                    Toast.LENGTH_SHORT).show()
                isServiceOn = false
            }
        }
        pendingWantOn = null
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.source_code_repo),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.author_dijia1124),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.current_version, versionName.toString()),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            stringResource(R.string.need_to_disable_auto_brightness),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(stringResource(R.string.system_settings_reminder), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = maxValueText,
            onValueChange = { input ->
                maxValueText = input.filter { it.isDigit() }
            },
            label = { Text(stringResource(R.string.maximum_value_of_brightness)) },
            singleLine = true,
            readOnly = !isEditing,
            enabled = isEditing,
            trailingIcon = {
                IconButton(onClick = {
                    if (isEditing) {
                        val newMax = maxValueText.toIntOrNull() ?: maxValue
                        scope.launch { prefsRepo.setMaxBrightness(newMax) }
                        focusManager.clearFocus()
                    }
                    isEditing = !isEditing
                }) {
                    val icon = if (isEditing) Icons.Default.Check else Icons.Default.Edit
                    Icon(icon, contentDescription = null)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.current_brightness_value_on_slider, brightness))

        Spacer(Modifier.height(16.dp))

        Slider(
            value = brightness.toFloat().coerceAtMost(maxValue.toFloat()),
            onValueChange = { v ->
                scope.launch {
                    prefsRepo.setBrightness(v.toInt().coerceAtMost(maxValue))
                }
            },
            valueRange = 0f..maxValue.toFloat(),
            steps = maxValue
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val intent = Intent(context, BrightnessLockerService::class.java)
                .apply { action = BrightnessLockerService.ACTION_APPLY }
            context.startService(intent)
        },
            enabled = isServiceOn,
            modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.apply))
        }

        Spacer(Modifier.height(16.dp))

        BrightnessServiceToggle(
            isOn = isServiceOn,
            onToggle = { wantOn ->
                pendingWantOn = wantOn
                if (hasRoot != true) {
                    Toast
                        .makeText(context,
                            context.getString(R.string.root_access_denied), Toast.LENGTH_LONG)
                        .show()
                    isServiceOn = false
                    pendingWantOn = null
                    return@BrightnessServiceToggle
                }
                if (wantOn) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startForegroundService(
                            Intent(context, BrightnessLockerService::class.java)
                                .apply { action = BrightnessLockerService.ACTION_START }
                        )
                        isServiceOn = true
                        pendingWantOn = null
                    }
                } else {
                    context.stopService(
                        Intent(context, BrightnessLockerService::class.java)
                            .apply { action = BrightnessLockerService.ACTION_STOP }
                    )
                    isServiceOn = false
                    pendingWantOn = null
                }
            }
        )
    }
}

@Composable
fun BrightnessServiceToggle(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.brightness_locker_service), modifier = Modifier.weight(1f))
        Switch(
            checked = isOn,
            onCheckedChange = onToggle
        )
    }
}

suspend fun hasRootAccess(): Boolean = withContext(Dispatchers.IO) {
    try {
        Shell.cmd("su -c whoami").exec().isSuccess
    } catch (e: Exception) {
        false
    }
}
