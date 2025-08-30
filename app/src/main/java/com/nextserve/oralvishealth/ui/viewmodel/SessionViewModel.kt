package com.nextserve.oralvishealth.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.nextserve.oralvishealth.data.database.AppDatabase
import com.nextserve.oralvishealth.data.entity.Session
import com.nextserve.oralvishealth.data.repository.SessionRepository
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SessionRepository
    private val _searchQuery = MutableLiveData<String>()
    
    val allSessions: LiveData<List<Session>> = _searchQuery.switchMap { query ->
        if (query.isNullOrBlank()) {
            repository.getAllSessions()
        } else {
            repository.searchSessions(query)
        }
    }

    init {
        val sessionDao = AppDatabase.getDatabase(application).sessionDao()
        repository = SessionRepository(sessionDao)
        _searchQuery.value = "" // Initialize with empty query to show all sessions
    }

    fun searchSessions(query: String) {
        _searchQuery.value = query
    }

    fun refreshSessions() {
        // Trigger a refresh by setting the search query to current value
        _searchQuery.value = _searchQuery.value
    }

    fun insertSession(session: Session) = viewModelScope.launch {
        repository.insertSession(session)
    }

    suspend fun getSessionById(sessionId: String): Session? {
        return repository.getSessionById(sessionId)
    }

    fun deleteSession(sessionId: String) = viewModelScope.launch {
        repository.deleteSession(sessionId)
    }
}
