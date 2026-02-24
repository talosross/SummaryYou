package com.talosross.summaryyou.data.converters

import androidx.room.TypeConverter
import com.talosross.summaryyou.model.SummaryType

class SummaryTypeConverter {
    @TypeConverter
    fun fromSummaryType(value: SummaryType): String {
        return value.name
    }

    @TypeConverter
    fun toSummaryType(value: String): SummaryType {
        return SummaryType.valueOf(value)
    }
}