package com.selendang3.app

import android.content.SharedPreferences
import com.selendang3.app.data.estateDisplayName
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberEstateName(importedName: String? = null): String {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("user_settings", 0) }
    var name by remember(preferences) { mutableStateOf(preferences.getString("estate_name", "") ?: "") }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "estate_name") name = prefs.getString("estate_name", "") ?: ""
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return estateDisplayName(name, importedName)
}

@Composable
fun EstateProfileControls() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("user_settings", 0) }
    var name by rememberSaveable { mutableStateOf(preferences.getString("estate_name", "") ?: "") }
    var saved by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(name, { if (it.length <= 100) { name = it; saved = false } },
        label = { Text("Estate name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        preferences.edit().putString("estate_name", name.trim()).apply()
        saved = true
    }, enabled = name.isNotBlank()) { Text("Save Estate Name") }
    if (saved) Text("Estate name saved on this device.")
}
