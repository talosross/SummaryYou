package me.nanova.summaryexpressive.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryType

@Dao
interface HistoryDao {
    @Query(
        """
         SELECT * FROM history 
         WHERE (:type IS NULL OR type = :type) 
         AND (:query = '' OR title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%')
         ORDER BY createdOn DESC
    """
    )
    fun getSummaries(query: String, type: SummaryType?): PagingSource<Int, HistorySummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: HistorySummary)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)
}