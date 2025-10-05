package me.nanova.summaryexpressive.data.converters

import androidx.room.TypeConverter
import me.nanova.summaryexpressive.model.SummaryType

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