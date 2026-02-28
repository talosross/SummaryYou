package com.talosross.summaryyou.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.talosross.summaryyou.model.HistorySummary

@Database(entities = [HistorySummary::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}