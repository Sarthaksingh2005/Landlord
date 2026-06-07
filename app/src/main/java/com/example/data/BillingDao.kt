package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BillingDao {
    @Query("SELECT * FROM billing_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BillingRecord>>

    @Query("SELECT * FROM billing_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): BillingRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BillingRecord): Long

    @Delete
    suspend fun deleteRecord(record: BillingRecord)

    @Query("DELETE FROM billing_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}
