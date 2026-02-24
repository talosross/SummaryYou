package com.talosross.summaryyou.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import com.talosross.summaryyou.data.converters.SummaryLengthConverter
import com.talosross.summaryyou.data.converters.SummaryTypeConverter
import com.talosross.summaryyou.data.converters.VideoSubtypeConverter
import com.talosross.summaryyou.llm.SummaryLength
import java.util.UUID

@Serializable
@Entity(tableName = "history")
@TypeConverters(
    SummaryLengthConverter::class,
    SummaryTypeConverter::class,
    VideoSubtypeConverter::class
)
data class HistorySummary(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val author: String = "",
    val createdOn: Long = System.currentTimeMillis(),
    val length: SummaryLength,
    val type: SummaryType,
    val subtype: VideoSubtype? = null,
    val sourceLink: String? = null,
    val sourceText: String? = null,
) {
    val isYoutubeLink: Boolean
        get() = type == SummaryType.VIDEO && subtype == VideoSubtype.YOUTUBE

    val isBiliBiliLink: Boolean
        get() = type == SummaryType.VIDEO && subtype == VideoSubtype.BILIBILI
}