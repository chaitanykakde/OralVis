package com.nextserve.oralvishealth.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nextserve.oralvishealth.data.entity.Session

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    fun getAllSessions(): LiveData<List<Session>>

    @Query("SELECT * FROM sessions WHERE sessionId LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchSessions(query: String): LiveData<List<Session>>

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    @Update
    suspend fun updateSession(session: Session)

    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
