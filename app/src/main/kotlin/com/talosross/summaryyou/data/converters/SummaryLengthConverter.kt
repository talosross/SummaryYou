package com.talosross.summaryyou.data.converters

import androidx.room.TypeConverter
import com.talosross.summaryyou.llm.SummaryLength

class SummaryLengthConverter {
    @TypeConverter
    fun fromSummaryLength(value: SummaryLength): String {
        return value.name
    }

    @TypeConverter
    fun toSummaryLength(value: String): SummaryLength {
        return SummaryLength.valueOf(value)
    }
}