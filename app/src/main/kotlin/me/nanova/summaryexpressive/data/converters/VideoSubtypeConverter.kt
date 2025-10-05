package me.nanova.summaryexpressive.data.converters

import androidx.room.TypeConverter
import me.nanova.summaryexpressive.model.VideoSubtype

class VideoSubtypeConverter {
    @TypeConverter
    fun fromVideoSubtype(value: VideoSubtype?): String? {
        return value?.name
    }

    @TypeConverter
    fun toVideoSubtype(value: String?): VideoSubtype? {
        return value?.let { VideoSubtype.valueOf(it) }
    }
}