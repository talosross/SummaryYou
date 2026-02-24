package com.talosross.summaryyou.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class SummaryType(val icon: ImageVector) {
    VIDEO(Icons.Outlined.Videocam),
    ARTICLE(Icons.AutoMirrored.Outlined.Article),
    DOCUMENT(Icons.AutoMirrored.Outlined.InsertDriveFile),
    TEXT(Icons.AutoMirrored.Outlined.Notes)
}