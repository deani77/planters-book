package com.selendang3.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Feedback address supplied by the app owner.
internal const val SUGGESTIONS_EMAIL = "deani77@gmail.com"

@Composable
fun SuggestionsScreen() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("suggestions", 0) }
    var suggestion by remember { mutableStateOf(preferences.getString("draft", "") ?: "") }
    var status by remember { mutableStateOf<String?>(null) }
    Text("Share an idea or report a problem. Your suggestion stays as a draft on this device until you send it through your email app.")
    OutlinedTextField(suggestion, {
        if (it.length <= 2000) {
            suggestion = it
            preferences.edit().putString("draft", it).apply()
            status = null
        }
    }, label = { Text("Your suggestion") }, supportingText = { Text("${suggestion.length}/2000") },
        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), minLines = 6)
    if (SUGGESTIONS_EMAIL.isBlank()) Text("The receiving email address has not been configured yet. Your draft is saved.")
    else Text("To: $SUGGESTIONS_EMAIL")
    Button(onClick = {
        val body = "Suggestion:\n${suggestion.trim()}\n\nPlanters Book · Android · ${BuildConfig.VERSION_NAME}"
        val uri = Uri.parse("mailto:$SUGGESTIONS_EMAIL?subject=${Uri.encode("Planters Book — Suggestion")}&body=${Uri.encode(body)}")
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
            status = "Email app opened. Tap Send there to submit. Your draft remains saved here."
        } catch (_: ActivityNotFoundException) {
            status = "No email app is available. Set up an email app, then try again. Your draft is saved."
        } catch (_: Exception) { status = "Could not open email. Your draft is saved." }
    }, enabled = suggestion.isNotBlank() && SUGGESTIONS_EMAIL.isNotBlank()) { Text("Email Suggestion") }
    TextButton(onClick = { suggestion = ""; preferences.edit().remove("draft").apply(); status = "Draft cleared." },
        enabled = suggestion.isNotEmpty()) { Text("Clear Draft") }
    status?.let { Text(it) }
}
