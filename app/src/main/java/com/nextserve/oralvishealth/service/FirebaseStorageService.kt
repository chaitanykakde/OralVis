package com.nextserve.oralvishealth.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.nextserve.oralvishealth.data.entity.Session
import com.nextserve.oralvishealth.data.model.CloudSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class FirebaseStorageService {
    
    companion object {
        private const val SESSIONS_PATH = "sessions"
        private const val METADATA_FILE = "metadata.json"
    }
    
    private lateinit var storage: FirebaseStorage
    private val gson = Gson()
    
    fun initialize() {
        storage = FirebaseStorage.getInstance()
        android.util.Log.d("FirebaseStorageService", "Firebase Storage initialized")
    }
    
    suspend fun uploadSession(
        session: Session,
        imageFiles: List<File>,
        progressCallback: (Int, String) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: throw IllegalStateException("User not authenticated")
            
            val userId = currentUser.uid
            val sessionPath = "$SESSIONS_PATH/$userId/${session.sessionId}"
            
            android.util.Log.d("FirebaseStorageService", "Starting upload for session: ${session.sessionId}")
            android.util.Log.d("FirebaseStorageService", "User ID: $userId")
            android.util.Log.d("FirebaseStorageService", "Session path: $sessionPath")
            android.util.Log.d("FirebaseStorageService", "Total images to upload: ${imageFiles.size}")
            
            progressCallback(5, "Preparing upload...")
            
            // Upload session metadata
            val metadataRef = storage.reference.child("$sessionPath/$METADATA_FILE")
            val metadataJson = gson.toJson(session)
            android.util.Log.d("FirebaseStorageService", "Uploading metadata: $metadataJson")
            
            metadataRef.putBytes(metadataJson.toByteArray()).await()
            
            android.util.Log.d("FirebaseStorageService", "Metadata uploaded successfully")
            
            // Upload images with progress tracking
            val totalImages = imageFiles.size
            var uploadedImages = 0
            
            progressCallback(10, "Uploading metadata...")
            
            for ((index, imageFile) in imageFiles.withIndex()) {
                // Use consistent naming: sessionId_img1, sessionId_img2, etc.
                val imageNumber = index + 1
                val newImageName = "${session.sessionId}_img$imageNumber.jpg"
                val imageRef = storage.reference.child("$sessionPath/images/$newImageName")
                
                android.util.Log.d("FirebaseStorageService", "Uploading image to path: $sessionPath/images/$newImageName")
                android.util.Log.d("FirebaseStorageService", "Original file: ${imageFile.name}, New name: $newImageName")
                android.util.Log.d("FirebaseStorageService", "Image file exists: ${imageFile.exists()}, size: ${imageFile.length()} bytes")
                
                progressCallback(
                    10 + (index * 80) / totalImages,
                    "Uploading image ${index + 1}/${totalImages}: $newImageName"
                )
                
                val uploadTask = imageRef.putFile(android.net.Uri.fromFile(imageFile))
                uploadTask.await()
                
                uploadedImages++
                val progress = 10 + (uploadedImages * 80) / totalImages
                progressCallback(progress, "Uploaded ${uploadedImages}/${totalImages} images")
                
                android.util.Log.d("FirebaseStorageService", "Successfully uploaded image: $newImageName to Firebase Storage")
            }
            
            android.util.Log.d("FirebaseStorageService", "Upload completed successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageService", "Upload failed", e)
            false
        }
    }
    
    suspend fun listCloudSessionsMetadata(): List<CloudSession> = withContext(Dispatchers.IO) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: throw IllegalStateException("User not authenticated")
            
            val userId = currentUser.uid
            val userSessionsRef = storage.reference.child("$SESSIONS_PATH/$userId")
            
            val cloudSessions = mutableListOf<CloudSession>()
            
            // Only load metadata quickly for list display
            android.util.Log.d("FirebaseStorageService", "Loading metadata only from: $SESSIONS_PATH/$userId")
            val listResult = userSessionsRef.listAll().await()
            
            android.util.Log.d("FirebaseStorageService", "Found ${listResult.prefixes.size} session folders")
            
            for (sessionRef in listResult.prefixes) {
                try {
                    val metadataRef = sessionRef.child(METADATA_FILE)
                    val metadataBytes = metadataRef.getBytes(Long.MAX_VALUE).await()
                    val metadataJson = String(metadataBytes)
                    val session = gson.fromJson(metadataJson, Session::class.java)
                    
                    cloudSessions.add(
                        CloudSession(
                            sessionId = session.sessionId,
                            name = session.name,
                            age = session.age,
                            timestamp = session.timestamp,
                            folderId = sessionRef.path
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseStorageService", "Failed to load session metadata", e)
                }
            }
            
            cloudSessions
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageService", "Failed to list cloud sessions", e)
            emptyList()
        }
    }
    
    suspend fun downloadSession(
        cloudSession: CloudSession,
        localDirectory: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: throw IllegalStateException("User not authenticated")
            
            // Create local session directory
            val sessionDir = File(localDirectory, cloudSession.sessionId)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            
            val userId = currentUser.uid
            val sessionPath = "$SESSIONS_PATH/$userId/${cloudSession.sessionId}"
            val imagesRef = storage.reference.child("$sessionPath/images")
            
            // Download all images from the session
            val imagesList = imagesRef.listAll().await()
            
            for (imageRef in imagesList.items) {
                val localFile = File(sessionDir, imageRef.name)
                imageRef.getFile(localFile).await()
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageService", "Failed to download session", e)
            false
        }
    }
    
    suspend fun downloadSessionImages(
        sessionId: String,
        localDirectory: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check Firebase Auth status
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                android.util.Log.e("FirebaseStorageService", "User not authenticated - cannot access Firebase Storage")
                throw IllegalStateException("User not authenticated")
            }
            
            android.util.Log.d("FirebaseStorageService", "User authenticated: ${currentUser.email}, UID: ${currentUser.uid}")
            
            // Check if user token is valid
            try {
                val tokenResult = currentUser.getIdToken(false).await()
                android.util.Log.d("FirebaseStorageService", "Auth token valid, expires: ${tokenResult.expirationTimestamp}")
            } catch (e: Exception) {
                android.util.Log.e("FirebaseStorageService", "Auth token invalid", e)
                throw e
            }
            
            val userId = currentUser.uid
            val sessionPath = "$SESSIONS_PATH/$userId/$sessionId"
            
            // First check if Firebase Storage service is initialized
            if (!::storage.isInitialized) {
                android.util.Log.d("FirebaseStorageService", "Initializing Firebase Storage")
                initialize()
            }
            
            android.util.Log.d("FirebaseStorageService", "Firebase Storage bucket: ${storage.reference.bucket}")
            android.util.Log.d("FirebaseStorageService", "Downloading images from path: $sessionPath/images")
            
            val imagesRef = storage.reference.child("$sessionPath/images")
            
            // Create local session directory structure to match upload
            val sessionsDir = File(localDirectory, "Sessions")
            if (!sessionsDir.exists()) {
                sessionsDir.mkdirs()
            }
            
            val sessionDir = File(sessionsDir, sessionId)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            
            android.util.Log.d("FirebaseStorageService", "Created local directory: ${sessionDir.absolutePath}")
            
            // Test Firebase Storage connection first
            try {
                android.util.Log.d("FirebaseStorageService", "Testing Firebase Storage connection...")
                val rootRef = storage.reference
                val testList = rootRef.listAll().await()
                android.util.Log.d("FirebaseStorageService", "Root level folders: ${testList.prefixes.map { it.name }}")
            } catch (e: Exception) {
                android.util.Log.e("FirebaseStorageService", "Failed to connect to Firebase Storage", e)
                throw e
            }
            
            // Download all images from the session
            android.util.Log.d("FirebaseStorageService", "Attempting to list images at: ${imagesRef.path}")
            val imagesList = try {
                imagesRef.listAll().await()
            } catch (e: Exception) {
                android.util.Log.e("FirebaseStorageService", "Failed to list images at path: ${imagesRef.path}", e)
                
                // Try different approaches to debug the issue
                android.util.Log.d("FirebaseStorageService", "Trying to list parent session folder...")
                val sessionRef = storage.reference.child(sessionPath)
                try {
                    val sessionList = sessionRef.listAll().await()
                    android.util.Log.d("FirebaseStorageService", "Session folder contents: ${sessionList.items.map { it.name }} and subfolders: ${sessionList.prefixes.map { it.name }}")
                    
                    // Check if images folder exists as a prefix
                    val imagesFolder = sessionList.prefixes.find { it.name == "images" }
                    if (imagesFolder != null) {
                        android.util.Log.d("FirebaseStorageService", "Images folder found, trying to list its contents...")
                        val imagesFolderList = imagesFolder.listAll().await()
                        android.util.Log.d("FirebaseStorageService", "Images folder contents: ${imagesFolderList.items.map { it.name }}")
                        imagesFolderList
                    } else {
                        android.util.Log.w("FirebaseStorageService", "Images folder not found in session")
                        throw e
                    }
                } catch (sessionE: Exception) {
                    android.util.Log.e("FirebaseStorageService", "Failed to list session folder", sessionE)
                    throw e
                }
            }
            
            android.util.Log.d("FirebaseStorageService", "Found ${imagesList.items.size} images to download")
            android.util.Log.d("FirebaseStorageService", "Image names: ${imagesList.items.map { it.name }}")
            android.util.Log.d("FirebaseStorageService", "Full image paths: ${imagesList.items.map { it.path }}")
            
            if (imagesList.items.isEmpty()) {
                android.util.Log.w("FirebaseStorageService", "No images found in Firebase Storage for session: $sessionId")
                android.util.Log.w("FirebaseStorageService", "Checked path: $sessionPath/images")
                return@withContext false
            }
            
            for (imageRef in imagesList.items) {
                val localFile = File(sessionDir, imageRef.name)
                imageRef.getFile(localFile).await()
                android.util.Log.d("FirebaseStorageService", "Downloaded image: ${imageRef.name} to ${localFile.absolutePath}")
            }
            
            android.util.Log.d("FirebaseStorageService", "Successfully downloaded ${imagesList.items.size} images")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorageService", "Failed to download session images for $sessionId", e)
            false
        }
    }
    
}
