package com.selendang3.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun UserSettingsScreen(themeChoice: String, onThemeChange: (String) -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("user_settings", 0) }
    var page by rememberSaveable { mutableStateOf("Settings") }
    var name by rememberSaveable { mutableStateOf(preferences.getString("profile_name", "") ?: "") }
    var position by rememberSaveable { mutableStateOf(preferences.getString("profile_position", "") ?: "") }
    var phone by rememberSaveable { mutableStateOf(preferences.getString("profile_phone", "") ?: "") }
    var saved by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (page != "Settings") TextButton(onClick = { page = "Settings" }) { Text("‹ Settings") }
            Text(page, style = MaterialTheme.typography.headlineSmall)
            when (page) {
                "Settings" -> {
                    listOf("Inbox", "User Profile", "Appearance", "About").forEach { item ->
                        OutlinedButton(onClick = { page = item }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(item)
                        }
                    }
                    HorizontalDivider()
                    EstateDataControls()

                }
                "About" -> {
                    Text("S3 Estate Info", style = MaterialTheme.typography.titleMedium)
                    Text("v${BuildConfig.VERSION_NAME}")
                    Text("Android estate information application. Initial Alpha interface.")
                }
                "User Profile" -> {
                    Text("Your profile on this device. This is not a signed-in account.")
                    OutlinedTextField(name, { name = it; saved = false }, label = { Text("Display name") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(position, { position = it; saved = false }, label = { Text("Position (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it; saved = false }, label = { Text("Phone (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        preferences.edit().putString("profile_name", name.trim())
                            .putString("profile_position", position.trim())
                            .putString("profile_phone", phone.trim()).apply()
                        saved = true
                    }, enabled = name.isNotBlank()) { Text("Save Profile") }
                    if (saved) Text("Profile saved on this device.")
                }
                "Appearance" -> {
                    Text("Choose how the app looks. Your choice applies immediately.")
                    listOf("light" to "Light", "dark" to "Dark", "system" to "Follow System").forEach { (value, label) ->
                        OutlinedButton(onClick = { onThemeChange(value) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(if (themeChoice == value) "✓ $label" else label)
                        }
                    }
                }
                "Inbox" -> {
                    Text("No messages yet", style = MaterialTheme.typography.titleMedium)
                    Text("Announcements and replies will appear here once messaging is connected.")
                }
            }
        }
    }
}
