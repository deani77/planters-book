package com.selendang3.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

@Composable
fun PdfAiControls(prompt: String, enabled: Boolean, onReview: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showPaste by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val file = withContext(Dispatchers.IO) {
                    val directory = File(context.cacheDir, "pdf-ai").apply { mkdirs() }
                    directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
                    val target = File(directory, "estate-${UUID.randomUUID()}.pdf")
                    try {
                        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open PDF.")
                        input.use { source -> target.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var total = 0
                            while (true) {
                                val count = source.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= 25 * 1024 * 1024) { "Choose a PDF smaller than 25 MB." }
                                output.write(buffer, 0, count)
                            }
                        } }
                        require(target.inputStream().use { String(ByteArray(5).also { header -> java.io.DataInputStream(it).readFully(header) }, Charsets.US_ASCII) } == "%PDF-") { "The selected file is not a PDF." }
                        target
                    } catch (failure: Exception) { target.delete(); throw failure }
                }
                val shared = FileProvider.getUriForFile(context, "${context.packageName}.pdfshare", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, shared)
                    putExtra(Intent.EXTRA_TEXT, prompt)
                    clipData = ClipData.newRawUri("Estate PDF", shared)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Choose an AI app"))
                message = "Choose an app that accepts PDFs. If instructions are missing, use Copy AI Instructions. Return here to paste its JSON result or import its JSON file."
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) { message = failure.message ?: "Could not share this PDF." }
            finally { busy = false }
        }
    }
    HorizontalDivider()
    Text("Read PDF with AI", style = MaterialTheme.typography.titleMedium)
    Text("Choose a PDF, send it to a compatible AI app, then return to review the extracted data. The AI app may upload the PDF and require internet. Nothing is saved automatically.")
    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }, enabled = enabled && !busy,
        modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Preparing PDF…" else "Read PDF with AI") }
    TextButton(onClick = {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("AI extraction instructions", prompt))
        message = "Instructions copied. Paste them into your AI app with the PDF."
    }, enabled = enabled && !busy) { Text("Copy AI Instructions") }
    OutlinedButton(onClick = { showPaste = true }, enabled = enabled && !busy,
        modifier = Modifier.fillMaxWidth()) { Text("Paste AI Result") }
    message?.let { Text(it) }
    if (showPaste) AlertDialog(onDismissRequest = { showPaste = false },
        title = { Text("Paste extracted JSON") },
        text = { OutlinedTextField(result, { if (it.length <= 2 * 1024 * 1024) result = it },
            label = { Text("JSON from your AI app") }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 300.dp)) },
        confirmButton = { TextButton(enabled = result.isNotBlank(), onClick = { onReview(result); showPaste = false; result = "" }) { Text("Review Data") } },
        dismissButton = { TextButton(onClick = { showPaste = false; result = "" }) { Text("Cancel") } })
}
