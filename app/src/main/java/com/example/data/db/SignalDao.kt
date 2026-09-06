package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {
    @Query("SELECT * FROM trade_signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE setupType = :setupType ORDER BY timestamp DESC")
    fun getSignalsBySetup(setupType: String): Flow<List<SignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE status = 'ACTIVE' ORDER BY timestamp DESC")
    fun getActiveSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE id = :id")
    suspend fun getSignalById(id: Long): SignalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<SignalEntity>)

    @Update
    suspend fun updateSignal(signal: SignalEntity)

    @Query("UPDATE trade_signals SET currentPrice = :currentPrice, highPrice = MAX(highPrice, :currentPrice), lowPrice = MIN(lowPrice, :currentPrice), status = :status WHERE id = :id")
    suspend fun updatePriceAndStatus(id: Long, currentPrice: Double, status: String)

    @Query("UPDATE trade_signals SET trailedStopLoss = :trailedSL, isTrailedToCost = :isTrailedToCost WHERE id = :id")
    suspend fun updateTrailingStopLoss(id: Long, trailedSL: Double, isTrailedToCost: Boolean)

    @Query("UPDATE trade_signals SET isTelegramSent = 1 WHERE id = :id")
    suspend fun markTelegramSent(id: Long)

    @Query("DELETE FROM trade_signals WHERE id = :id")
    suspend fun deleteSignal(id: Long)

    @Query("DELETE FROM trade_signals")
    suspend fun clearAllSignals()

    @Query("SELECT COUNT(*) FROM trade_signals")
    suspend fun getCount(): Int
}
