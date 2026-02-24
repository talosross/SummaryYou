package com.talosross.summaryyou.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

@Composable
fun ClickablePasteIcon(
    text: String,
    onPaste: (String) -> Unit,
    onClear: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    if (text.isNotBlank()) {
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Clear, contentDescription = "Clear")
        }
    } else {
        IconButton(onClick = {
            scope.launch {
                clipboard.getClipEntry()?.let {
                    val pastedText = it.clipData.getItemAt(0).text.toString()
                    onPaste(pastedText)
                }
            }
        }) {
            Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
        }
    }
}
