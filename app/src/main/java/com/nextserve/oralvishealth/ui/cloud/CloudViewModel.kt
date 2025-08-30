package com.nextserve.oralvishealth.ui.cloud

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nextserve.oralvishealth.data.database.AppDatabase
import com.nextserve.oralvishealth.data.entity.Session
import com.nextserve.oralvishealth.data.model.CloudSession
import com.nextserve.oralvishealth.data.repository.SessionRepository
import com.nextserve.oralvishealth.service.FirebaseStorageService
import kotlinx.coroutines.launch

class CloudViewModel(application: Application) : AndroidViewModel(application) {
    
    private val firebaseStorageService = FirebaseStorageService()
    private val sessionRepository: SessionRepository
    
    private val _cloudSessions = MutableLiveData<List<CloudSession>>()
    val cloudSessions: LiveData<List<CloudSession>> = _cloudSessions
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        val sessionDao = AppDatabase.getDatabase(application).sessionDao()
        sessionRepository = SessionRepository(sessionDao)
    }
    
    fun loadCloudSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Initialize Firebase Storage service first
                firebaseStorageService.initialize()
                android.util.Log.d("CloudViewModel", "Firebase Storage initialized")
                
                val sessions = firebaseStorageService.listCloudSessionsMetadata()
                android.util.Log.d("CloudViewModel", "Found ${sessions.size} cloud sessions")
                _cloudSessions.value = sessions
            } catch (e: Exception) {
                android.util.Log.e("CloudViewModel", "Failed to load cloud sessions", e)
                _error.value = e.message
                _cloudSessions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    suspend fun downloadSessionImages(sessionId: String): Boolean {
        return try {
            android.util.Log.d("CloudViewModel", "Downloading images for session: $sessionId")
            val result = firebaseStorageService.downloadSessionImages(
                sessionId,
                getApplication<Application>().getExternalFilesDir(null)!!
            )
            android.util.Log.d("CloudViewModel", "Download result for $sessionId: $result")
            result
        } catch (e: Exception) {
            android.util.Log.e("CloudViewModel", "Failed to download session images for $sessionId", e)
            false
        }
    }
    
    // Removed downloadAndSaveSession - cloud sessions should remain independent from local database
}
