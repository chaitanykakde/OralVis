package com.nextserve.oralvishealth.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseStorageTestService {
    
    private lateinit var storage: FirebaseStorage
    
    fun initialize() {
        storage = FirebaseStorage.getInstance()
        android.util.Log.d("FirebaseStorageTestService", "Firebase Storage initialized")
    }
    
    suspend fun testFirebaseStorageAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check authentication
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                android.util.Log.e("FirebaseStorageTestService", "User not authenticated")
                return@withContext false
            }
            
            android.util.Log.d("FirebaseStorageTestService", "User: ${currentUser.email}, UID: ${currentUser.uid}")
            
            // Initialize storage if needed
            if (!::storage.isInitialized) {
                initialize()
            }
            
            // Test basic Firebase Storage access
            android.util.Log.d("FirebaseStorageTestService", "Testing Firebase Storage connection...")
            val rootRef = storage.reference
            android.util.Log.d("FirebaseStorageTestService", "Storage bucket: ${rootRef.bucket}")
            
            // List root level contents
            val rootList = rootRef.listAll().await()
            android.util.Log.d("FirebaseStorageTestService", "Root folders: ${rootList.prefixes.map { it.name }}")
            android.util.Log.d("FirebaseStorageTestService", "Root files: ${rootList.items.map { it.name }}")
            
            // Check sessions folder
            val sessionsRef = rootRef.child("sessions")
            try {
                val sessionsList = sessionsRef.listAll().await()
                android.util.Log.d("FirebaseStorageTestService", "Sessions folder users: ${sessionsList.prefixes.map { it.name }}")
                
                // Check current user's folder
                val userRef = sessionsRef.child(currentUser.uid)
                try {
                    val userList = userRef.listAll().await()
                    android.util.Log.d("FirebaseStorageTestService", "User sessions: ${userList.prefixes.map { it.name }}")
                    
                    // Check each session
                    for (sessionPrefix in userList.prefixes) {
                        android.util.Log.d("FirebaseStorageTestService", "Checking session: ${sessionPrefix.name}")
                        val sessionList = sessionPrefix.listAll().await()
                        android.util.Log.d("FirebaseStorageTestService", "Session ${sessionPrefix.name} contents:")
                        android.util.Log.d("FirebaseStorageTestService", "  Files: ${sessionList.items.map { it.name }}")
                        android.util.Log.d("FirebaseStorageTestService", "  Folders: ${sessionList.prefixes.map { it.name }}")
                        
                        // Check images folder if exists
                        val imagesFolder = sessionList.prefixes.find { it.name == "images" }
                        if (imagesFolder != null) {
                            val imagesList = imagesFolder.listAll().await()
                            android.util.Log.d("FirebaseStorageTestService", "  Images: ${imagesList.items.map { it.name }}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseStorageTestService", "Failed to list user sessions", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseStorageTestService", "Failed to list sessions folder", e)
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageTestService", "Firebase Storage test failed", e)
            false
        }
    }
}
