package com.nextserve.oralvishealth.data.repository

import androidx.lifecycle.LiveData
import com.nextserve.oralvishealth.data.dao.SessionDao
import com.nextserve.oralvishealth.data.entity.Session

class SessionRepository(private val sessionDao: SessionDao) {
    
    fun getAllSessions(): LiveData<List<Session>> = sessionDao.getAllSessions()
    
    fun searchSessions(query: String): LiveData<List<Session>> = sessionDao.searchSessions(query)
    
    suspend fun getSessionById(sessionId: String): Session? {
        return sessionDao.getSessionById(sessionId)
    }
    
    suspend fun insertSession(session: Session) {
        sessionDao.insertSession(session)
    }

    suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session)
    }

    suspend fun deleteSession(sessionId: String) = sessionDao.deleteSession(sessionId)
}
