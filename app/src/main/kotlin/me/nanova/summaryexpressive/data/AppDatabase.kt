package me.nanova.summaryexpressive.data

import androidx.room.Database
import androidx.room.RoomDatabase
import me.nanova.summaryexpressive.model.HistorySummary

@Database(entities = [HistorySummary::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}